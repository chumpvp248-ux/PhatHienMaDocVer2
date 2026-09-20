"""
Huấn luyện mô hình MLP và Random Forest baseline trên 20 đặc trưng từ vựng URL.
Tự động hiệu chuẩn ngưỡng trên Validation set và đánh giá trên Test set.
"""

import json
import os
import numpy as np
from sklearn.neural_network import MLPClassifier
from sklearn.ensemble import RandomForestClassifier
from sklearn.preprocessing import StandardScaler
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix, roc_auc_score

def load_split(filename: str):
    path = os.path.join("artifacts", "data", filename)
    with open(path, "r", encoding="utf-8") as f:
        data = json.load(f)
    X = np.array([item["vector"] for item in data], dtype=np.float32)
    y = np.array([item["label"] for item in data], dtype=np.int32)
    return X, y

def evaluate_predictions(y_true, y_pred, y_prob):
    cm = confusion_matrix(y_true, y_pred)
    # TN, FP, FN, TP
    tn, fp, fn, tp = cm.ravel() if cm.size == 4 else (0, 0, 0, 0)
    acc = accuracy_score(y_true, y_pred)
    prec = precision_score(y_true, y_pred, zero_division=0)
    rec = recall_score(y_true, y_pred, zero_division=0)
    f1 = f1_score(y_true, y_pred, zero_division=0)
    fpr = fp / (fp + tn) if (fp + tn) > 0 else 0.0
    auc = roc_auc_score(y_true, y_prob) if len(np.unique(y_true)) > 1 else 0.5

    return {
        "accuracy": round(float(acc), 4),
        "precision": round(float(prec), 4),
        "recall": round(float(rec), 4),
        "f1": round(float(f1), 4),
        "fpr": round(float(fpr), 4),
        "auc": round(float(auc), 4),
        "confusion_matrix": {
            "tp": int(tp), "fp": int(fp), "tn": int(tn), "fn": int(fn)
        }
    }

def train_models():
    print("Đang nạp dữ liệu train / val / test...")
    X_train, y_train = load_split("train.json")
    X_val, y_val = load_split("val.json")
    X_test, y_test = load_split("test.json")

    print(f"X_train: {X_train.shape}, X_val: {X_val.shape}, X_test: {X_test.shape}")

    # Standard Scaler (chỉ fit trên train set)
    scaler = StandardScaler()
    X_train_scaled = scaler.fit_transform(X_train)
    X_val_scaled = scaler.transform(X_val)
    X_test_scaled = scaler.transform(X_test)

    # 1. Huấn luyện MLP (64, 32)
    print("\n--- Huấn luyện mô hình chính: MLP (Multi-Layer Perceptron 20 -> 64 -> 32 -> 1) ---")
    mlp = MLPClassifier(
        hidden_layer_sizes=(64, 32),
        activation="relu",
        solver="adam",
        alpha=0.001,
        batch_size=32,
        learning_rate_init=0.005,
        max_iter=600,
        random_state=42,
        early_stopping=True,
        n_iter_no_change=20
    )
    mlp.fit(X_train_scaled, y_train)

    val_prob_mlp = mlp.predict_proba(X_val_scaled)[:, 1]
    # Chọn threshold tối ưu trên val set (đảm bảo recall cao, FPR thấp)
    best_thresh = 0.5
    best_f1 = 0.0
    for thresh in np.arange(0.2, 0.8, 0.05):
        pred = (val_prob_mlp >= thresh).astype(int)
        f = f1_score(y_val, pred, zero_division=0)
        if f > best_f1:
            best_f1 = f
            best_thresh = round(float(thresh), 2)

    print(f"Ngưỡng tối ưu xác định trên Val set: {best_thresh} (Val F1: {best_f1:.4f})")

    val_pred_mlp = (val_prob_mlp >= best_thresh).astype(int)
    mlp_val_metrics = evaluate_predictions(y_val, val_pred_mlp, val_prob_mlp)
    print("MLP Validation Metrics:", mlp_val_metrics)

    # Đánh giá trên Test set
    test_prob_mlp = mlp.predict_proba(X_test_scaled)[:, 1]
    test_pred_mlp = (test_prob_mlp >= best_thresh).astype(int)
    mlp_test_metrics = evaluate_predictions(y_test, test_pred_mlp, test_prob_mlp)
    print("MLP Test Metrics:", mlp_test_metrics)

    # 2. Huấn luyện Random Forest làm đối chứng (Baseline)
    print("\n--- Huấn luyện mô hình đối chứng: Random Forest (100 trees) ---")
    rf = RandomForestClassifier(n_estimators=100, max_depth=10, random_state=42)
    rf.fit(X_train, y_train)

    val_prob_rf = rf.predict_proba(X_val)[:, 1]
    val_pred_rf = rf.predict(X_val)
    rf_val_metrics = evaluate_predictions(y_val, val_pred_rf, val_prob_rf)

    test_prob_rf = rf.predict_proba(X_test)[:, 1]
    test_pred_rf = rf.predict(X_test)
    rf_test_metrics = evaluate_predictions(y_test, test_pred_rf, test_prob_rf)
    print("Random Forest Test Metrics:", rf_test_metrics)

    # Lưu trọng số MLP và Scaler parameters
    # MLP coefs_ có 3 mảng: (20, 64), (64, 32), (32, 1)
    # intercepts_ có 3 mảng: (64,), (32,), (1,)
    weights_data = {
        "scaler_mean": scaler.mean_.tolist(),
        "scaler_scale": scaler.scale_.tolist(),
        "w1": mlp.coefs_[0].tolist(),
        "b1": mlp.intercepts_[0].tolist(),
        "w2": mlp.coefs_[1].tolist(),
        "b2": mlp.intercepts_[1].tolist(),
        "w3": mlp.coefs_[2].tolist(),
        "b3": mlp.intercepts_[2].tolist(),
        "threshold": best_thresh
    }

    artifacts_dir = os.path.join("artifacts")
    os.makedirs(artifacts_dir, exist_ok=True)

    with open(os.path.join(artifacts_dir, "mlp_weights.json"), "w", encoding="utf-8") as f:
        json.dump(weights_data, f, indent=2)

    metrics_report = {
        "version": "1.0.0",
        "selected_threshold": best_thresh,
        "mlp": {
            "architecture": "MLP (20 -> 64 -> 32 -> 1)",
            "validation": mlp_val_metrics,
            "test": mlp_test_metrics
        },
        "random_forest_baseline": {
            "architecture": "RandomForest (n_estimators=100)",
            "validation": rf_val_metrics,
            "test": rf_test_metrics
        }
    }

    with open(os.path.join(artifacts_dir, "model_metrics.json"), "w", encoding="utf-8") as f:
        json.dump(metrics_report, f, indent=2)

    print(f"\nĐã lưu trọng số tại {os.path.join(artifacts_dir, 'mlp_weights.json')} và metrics tại {os.path.join(artifacts_dir, 'model_metrics.json')}!")


if __name__ == "__main__":
    train_models()
