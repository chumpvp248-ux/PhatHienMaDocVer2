"""
Xuất mô hình MLP sang chuẩn TensorFlow Lite FlatBuffer (model.tflite).
Hấp thụ StandardScaler trực tiếp vào trọng số tầng 1 để Android không cần tiền xử lý chuẩn hóa.
Kiểm tra độ chính xác trên 105 Golden Vectors với Interpreter TFLite.
"""

import json
import os
import numpy as np
import flatbuffers
from ai_edge_litert import schema_py_generated as s
from ai_edge_litert.interpreter import Interpreter

def export_mlp_to_tflite():
    weights_path = os.path.join("artifacts", "mlp_weights.json")
    with open(weights_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    mean = np.array(data["scaler_mean"], dtype=np.float32)
    scale = np.array(data["scaler_scale"], dtype=np.float32)
    w1_raw = np.array(data["w1"], dtype=np.float32) # shape (20, 64)
    b1_raw = np.array(data["b1"], dtype=np.float32) # shape (64,)
    w2 = np.array(data["w2"], dtype=np.float32)     # shape (64, 32)
    b2 = np.array(data["b2"], dtype=np.float32)     # shape (32,)
    w3 = np.array(data["w3"], dtype=np.float32)     # shape (32, 1)
    b3 = np.array(data["b3"], dtype=np.float32)     # shape (1,)

    # Hấp thụ scaler vào W1 và b1:
    # x_scaled = (x - mean) / scale
    # z1 = x_scaled @ W1 + b1 = x @ (W1 / scale) + (b1 - (mean / scale) @ W1)
    w1_eff = w1_raw / scale[:, np.newaxis] # (20, 64)
    b1_eff = b1_raw - np.dot(mean / scale, w1_raw) # (64,)

    # TFLite FULLY_CONNECTED yêu cầu filter shape là [num_outputs, num_inputs]
    w1_tflite = w1_eff.T # (64, 20)
    w2_tflite = w2.T     # (32, 64)
    w3_tflite = w3.T     # (1, 32)

    # Khởi tạo Model TFLite
    model = s.ModelT()
    model.version = 3
    model.description = "AppChongLuaDao MLP Phishing Detection Model"

    buffers = [s.BufferT()] # Buffer 0 luôn rỗng theo spec

    def add_buf(arr: np.ndarray) -> int:
        b = s.BufferT()
        b.data = list(arr.astype(np.float32).tobytes())
        buffers.append(b)
        return len(buffers) - 1

    # Operator Codes
    op_fc = s.OperatorCodeT()
    op_fc.builtinCode = s.BuiltinOperator.FULLY_CONNECTED
    op_fc.deprecatedBuiltinCode = s.BuiltinOperator.FULLY_CONNECTED

    op_log = s.OperatorCodeT()
    op_log.builtinCode = s.BuiltinOperator.LOGISTIC
    op_log.deprecatedBuiltinCode = s.BuiltinOperator.LOGISTIC

    model.operatorCodes = [op_fc, op_log]

    subgraph = s.SubGraphT()
    model.subgraphs = [subgraph]

    def make_tensor(name: str, shape: list, buf_idx: int = 0) -> s.TensorT:
        t = s.TensorT()
        t.name = name
        t.shape = shape
        t.type = s.TensorType.FLOAT32
        t.buffer = buf_idx
        return t

    # Danh sách tensors:
    # 0: input [1, 20]
    # 1: w1 [64, 20]
    # 2: b1 [64]
    # 3: out1 [1, 64]
    # 4: w2 [32, 64]
    # 5: b2 [32]
    # 6: out2 [1, 32]
    # 7: w3 [1, 32]
    # 8: b3 [1]
    # 9: logits [1, 1]
    # 10: output_prob [1, 1]

    t_in = make_tensor("input_features", [1, 20])
    t_w1 = make_tensor("w1", [64, 20], add_buf(w1_tflite))
    t_b1 = make_tensor("b1", [64], add_buf(b1_eff))
    t_out1 = make_tensor("out1_relu", [1, 64])

    t_w2 = make_tensor("w2", [32, 64], add_buf(w2_tflite))
    t_b2 = make_tensor("b2", [32], add_buf(b2))
    t_out2 = make_tensor("out2_relu", [1, 32])

    t_w3 = make_tensor("w3", [1, 32], add_buf(w3_tflite))
    t_b3 = make_tensor("b3", [1], add_buf(b3))
    t_logits = make_tensor("logits", [1, 1])

    t_prob = make_tensor("output_probability", [1, 1])

    subgraph.tensors = [
        t_in, t_w1, t_b1, t_out1,
        t_w2, t_b2, t_out2,
        t_w3, t_b3, t_logits,
        t_prob
    ]
    subgraph.inputs = [0]
    subgraph.outputs = [10]

    # Op 1: FC 1 (ReLU)
    fc1 = s.OperatorT()
    fc1.opcodeIndex = 0
    fc1.inputs = [0, 1, 2]
    fc1.outputs = [3]
    opt1 = s.FullyConnectedOptionsT()
    opt1.fusedActivationFunction = s.ActivationFunctionType.RELU
    fc1.builtinOptions = opt1
    fc1.builtinOptionsType = s.BuiltinOptions.FullyConnectedOptions

    # Op 2: FC 2 (ReLU)
    fc2 = s.OperatorT()
    fc2.opcodeIndex = 0
    fc2.inputs = [3, 4, 5]
    fc2.outputs = [6]
    opt2 = s.FullyConnectedOptionsT()
    opt2.fusedActivationFunction = s.ActivationFunctionType.RELU
    fc2.builtinOptions = opt2
    fc2.builtinOptionsType = s.BuiltinOptions.FullyConnectedOptions

    # Op 3: FC 3 (NONE)
    fc3 = s.OperatorT()
    fc3.opcodeIndex = 0
    fc3.inputs = [6, 7, 8]
    fc3.outputs = [9]
    opt3 = s.FullyConnectedOptionsT()
    opt3.fusedActivationFunction = s.ActivationFunctionType.NONE
    fc3.builtinOptions = opt3
    fc3.builtinOptionsType = s.BuiltinOptions.FullyConnectedOptions

    # Op 4: LOGISTIC (Sigmoid)
    log_op = s.OperatorT()
    log_op.opcodeIndex = 1
    log_op.inputs = [9]
    log_op.outputs = [10]
    log_op.builtinOptions = None
    log_op.builtinOptionsType = s.BuiltinOptions.NONE

    subgraph.operators = [fc1, fc2, fc3, log_op]
    model.buffers = buffers

    builder = flatbuffers.Builder(1024 * 64)
    builder.Finish(model.Pack(builder), b"TFL3")
    tflite_bytes = bytes(builder.Output())

    artifacts_path = os.path.join("artifacts", "model.tflite")
    assets_dir = os.path.join("app", "src", "main", "assets")
    os.makedirs(assets_dir, exist_ok=True)
    assets_path = os.path.join(assets_dir, "model.tflite")

    with open(artifacts_path, "wb") as f:
        f.write(tflite_bytes)

    with open(assets_path, "wb") as f:
        f.write(tflite_bytes)

    model_size_kb = len(tflite_bytes) / 1024
    print(f"Đã xuất model.tflite thành công! Kích thước: {model_size_kb:.2f} KB (Mục tiêu: < 5000 KB).")

    # Kiểm tra xác thực (Validation) với Interpreter trên Golden Vectors
    print("\nĐang kiểm thử mô hình TFLite trên 105 Golden Vectors...")
    interp = Interpreter(model_path=artifacts_path)
    interp.allocate_tensors()
    in_idx = interp.get_input_details()[0]["index"]
    out_idx = interp.get_output_details()[0]["index"]

    with open(os.path.join("contracts", "golden_vectors.json"), "r", encoding="utf-8") as f:
        golden_data = json.load(f)

    max_diff = 0.0
    verification_records = []

    for item in golden_data["items"]:
        raw_vec = np.array([item["vector"]], dtype=np.float32)

        # 1. Dự đoán theo công thức Python gốc
        scaled_vec = (raw_vec - mean) / scale
        h1 = np.maximum(0, np.dot(scaled_vec, w1_raw) + b1_raw)
        h2 = np.maximum(0, np.dot(h1, w2) + b2)
        logits_py = np.dot(h2, w3) + b3
        prob_py = float(1.0 / (1.0 + np.exp(-logits_py))[0, 0])

        # 2. Dự đoán qua TFLite Interpreter
        interp.set_tensor(in_idx, raw_vec)
        interp.invoke()
        prob_tflite = float(interp.get_tensor(out_idx)[0, 0])

        diff = abs(prob_py - prob_tflite)
        max_diff = max(max_diff, diff)

        # Tính risk score 1-10 theo công thức: clamp(1 + floor(9p + 0.5), 1, 10)
        score_py = max(1, min(10, int(1 + np.floor(9 * prob_py + 0.5))))
        score_tflite = max(1, min(10, int(1 + np.floor(9 * prob_tflite + 0.5))))

        verification_records.append({
            "id": item["id"],
            "url": item["raw_url"],
            "prob_python": round(prob_py, 6),
            "prob_tflite": round(prob_tflite, 6),
            "diff": round(diff, 8),
            "score_python": score_py,
            "score_tflite": score_tflite,
            "match": score_py == score_tflite
        })

    print(f"Kiểm thử hoàn tất! Sai lệch lớn nhất (Max Diff): {max_diff:.8f}")
    all_match = all(r["match"] for r in verification_records)
    print(f"Tất cả {len(verification_records)} Golden Vectors khớp tuyệt đối điểm số 1-10: {all_match}")

    with open(os.path.join("artifacts", "tflite_verification.json"), "w", encoding="utf-8") as f:
        json.dump({
            "model_size_kb": model_size_kb,
            "max_absolute_diff": max_diff,
            "all_score_match": all_match,
            "sample_count": len(verification_records),
            "records": verification_records[:10] # lưu 10 mẫu đầu làm minh chứng
        }, f, indent=2)

    print("Đã lưu kết quả kiểm thử tại artifacts/tflite_verification.json!")


if __name__ == "__main__":
    export_mlp_to_tflite()
