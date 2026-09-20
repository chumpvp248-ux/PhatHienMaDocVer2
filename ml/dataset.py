"""
Dataset Pipeline for Phishing URL Detection.
Tạo và xử lý tập dữ liệu mẫu đại diện cân bằng giữa Benign và Phishing URL.
Phân chia độc lập theo registered_domain (70% Train / 15% Validation / 15% Test).
"""

import json
import os
import random
from typing import List, Dict, Any, Tuple
from ml.features import extract_features, features_to_vector, normalize_and_parse, is_valid_ip, TWO_PART_PUBLIC_SUFFIXES

SEED = 42
random.seed(SEED)

def get_registered_domain(host: str) -> str:
    """Xác định registered domain để chia split không bị rò rỉ tên miền."""
    if not host or is_valid_ip(host):
        return host or "unknown"
    labels = host.strip(".").lower().split(".")
    n = len(labels)
    if n <= 1:
        return host
    if n >= 2:
        last_two = f"{labels[-2]}.{labels[-1]}"
        if last_two in TWO_PART_PUBLIC_SUFFIXES:
            if n >= 3:
                return f"{labels[-3]}.{last_two}"
            return host
    return f"{labels[-2]}.{labels[-1]}"

# Danh sách mở rộng các URL đại diện
BENIGN_DOMAINS = [
    "google.com", "youtube.com", "facebook.com", "wikipedia.org", "yahoo.com",
    "amazon.com", "reddit.com", "twitter.com", "instagram.com", "linkedin.com",
    "saodo.edu.vn", "hust.edu.vn", "vnu.edu.vn", "neu.edu.vn", "ftu.edu.vn",
    "vnexpress.net", "dantri.com.vn", "tuoitre.vn", "thanhnien.vn", "vtv.vn",
    "shopee.vn", "tiki.vn", "lazada.vn", "sendo.vn", "thegioididong.com",
    "fptshop.com.vn", "cellphones.com.vn", "chotot.com", "batdongsan.com.vn",
    "github.com", "gitlab.com", "stackoverflow.com", "medium.com", "dev.to",
    "microsoft.com", "apple.com", "cloudflare.com", "mozilla.org", "w3.org",
    "python.org", "kotlinlang.org", "oracle.com", "apache.org", "docker.com",
    "chinhphu.vn", "moet.gov.vn", "bocongan.gov.vn", "byt.gov.vn", "mic.gov.vn",
    "bbc.com", "cnn.com", "reuters.com", "nytimes.com", "theguardian.com",
    "netflix.com", "spotify.com", "twitch.tv", "steamcommunity.com", "epicgames.com",
    "vietcombank.com.vn", "mbbank.com.vn", "techcombank.com.vn", "bidv.com.vn",
    "vietinbank.vn", "agribank.com.vn", "acb.com.vn", "vpbank.com.vn", "tpbank.com.vn"
]

BENIGN_PATHS = [
    "",
    "/",
    "/index.html",
    "/about",
    "/contact-us",
    "/news/2026/09/technology-update",
    "/products/item-88123.html",
    "/category/electronics/mobile-phones",
    "/search?q=open+source+machine+learning&page=2",
    "/download/latest/release.zip",
    "/docs/v2/getting-started/overview",
    "/portal/student/view-grades?semester=20261",
    "/article/detail?id=1298471&cat=100",
    "/service/support/faq",
    "/pricing/enterprise-plan?ref=footer"
]

PHISHING_DOMAINS = [
    "vietcombank-online-ebanking.xyz",
    "mbbank-xac-thuc-smart-otp.top",
    "techcombank-identity-verify.club",
    "bidv-ebanking-smartotp.site",
    "vietinbank-online-auth.info",
    "agribank-nhan-tien-hoan-thue.live",
    "acb-digibank-security.vip",
    "vpbank-vay-nhanh-247.online",
    "tpbank-livebank-xac-thuc.cc",
    "vneid-dinh-danh-quoc-gia.biz",
    "baohiemxahoi-nhan-tro-cap.fun",
    "cuc-thue-hoan-tien-thue.work",
    "chongluadao-security-check.tk",
    "apple-id-verify-locked.net",
    "paypal-account-security-update.com.co",
    "netflix-billing-update-required.xyz",
    "google-account-recovery-session.top",
    "facebook-security-checkpoint-auth.info",
    "microsoft-office365-renew-account.live",
    "telegram-gift-free-premium.site",
    "zalo-tang-qua-tri-an.club",
    "mo-mo-nhan-voucher-200k.online",
    "shopee-trung-thuong-xe-may.vip",
    "binance-security-verification-login.pw",
    "metamask-wallet-seed-phrase.click"
]

PHISHING_PATHS = [
    "/login.php",
    "/signin.html",
    "/verify-account",
    "/bank/smart-otp-verification",
    "/secure/update-credentials.asp",
    "/auth/checkpoint",
    "/claim-reward/index.htm?code=VIETNAM2026",
    "/unlock-account/submit.php",
    "/confirm-identity?step=2&token=992813",
    "/xac-minh-thong-tin-ngan-hang.php",
    "/nhan-tien-ho-tro?id=8831",
    "/cap-nhat-sinh-trac-hoc",
    "/nap-the-tang-500-phan-tram"
]

RAW_IPS = [
    "192.168.1.100", "203.0.113.45", "198.51.100.12", "192.0.2.1",
    "103.20.10.5", "14.225.10.99", "172.16.0.4", "45.33.32.156",
    "185.199.110.153", "118.69.177.10"
]

def build_dataset() -> List[Dict[str, Any]]:
    samples = []
    sample_id = 1

    # 1. Benign Samples (~300 samples)
    for domain in BENIGN_DOMAINS:
        scheme = "https" if random.random() > 0.1 else "http"
        # Sinh 4-5 URL cho mỗi benign domain
        for _ in range(4):
            path = random.choice(BENIGN_PATHS)
            raw_url = f"{scheme}://{domain}{path}"
            feat = extract_features(raw_url)
            if feat:
                samples.append({
                    "id": sample_id,
                    "url": raw_url,
                    "label": 0, # 0 = Benign
                    "registered_domain": get_registered_domain(domain),
                    "features": feat,
                    "vector": features_to_vector(feat)
                })
                sample_id += 1

    # 2. Phishing Domain Samples (~300 samples)
    for domain in PHISHING_DOMAINS:
        scheme = "https" if random.random() > 0.6 else "http"
        for _ in range(10):
            path = random.choice(PHISHING_PATHS)
            # Thỉnh thoảng chèn userinfo hoặc subdomain lừa đảo
            dice = random.random()
            if dice < 0.2:
                prefix = random.choice(["vietcombank.com.vn", "mbbank.com.vn", "paypal.com", "accounts.google.com"])
                raw_url = f"{scheme}://{prefix}@{domain}{path}"
            elif dice < 0.4:
                sub = random.choice(["login", "secure", "verify.portal", "smart-otp", "update.account"])
                raw_url = f"{scheme}://{sub}.{domain}{path}"
            else:
                raw_url = f"{scheme}://{domain}{path}"

            feat = extract_features(raw_url)
            if feat:
                samples.append({
                    "id": sample_id,
                    "url": raw_url,
                    "label": 1, # 1 = Phishing
                    "registered_domain": get_registered_domain(domain),
                    "features": feat,
                    "vector": features_to_vector(feat)
                })
                sample_id += 1

    # 3. Phishing IP Samples (~50 samples)
    for ip in RAW_IPS:
        for _ in range(5):
            scheme = "http" if random.random() > 0.2 else "https"
            path = random.choice(PHISHING_PATHS)
            port = ":8080" if random.random() < 0.3 else ""
            raw_url = f"{scheme}://{ip}{port}{path}"
            feat = extract_features(raw_url)
            if feat:
                samples.append({
                    "id": sample_id,
                    "url": raw_url,
                    "label": 1,
                    "registered_domain": ip,
                    "features": feat,
                    "vector": features_to_vector(feat)
                })
                sample_id += 1

    print(f"Tổng số mẫu thu thập được: {len(samples)} (Benign: {sum(1 for s in samples if s['label'] == 0)}, Phishing: {sum(1 for s in samples if s['label'] == 1)})")
    return samples


def split_dataset(samples: List[Dict[str, Any]]) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]], List[Dict[str, Any]]]:
    """
    Phân chia dataset theo registered_domain thành train/val/test (70/15/15)
    Đảm bảo 1 tên miền không đồng thời xuất hiện ở nhiều tập.
    """
    domain_to_samples: Dict[str, List[Dict[str, Any]]] = {}
    for s in samples:
        dom = s["registered_domain"]
        domain_to_samples.setdefault(dom, []).append(s)

    domains = list(domain_to_samples.keys())
    random.shuffle(domains)

    train_samples, val_samples, test_samples = [], [], []
    total = len(samples)
    target_train = int(total * 0.70)
    target_val = int(total * 0.15)

    current_train = 0
    current_val = 0

    for dom in domains:
        dom_samples = domain_to_samples[dom]
        count = len(dom_samples)
        if current_train + count <= target_train:
            train_samples.extend(dom_samples)
            current_train += count
        elif current_val + count <= target_val:
            val_samples.extend(dom_samples)
            current_val += count
        else:
            test_samples.extend(dom_samples)

    # Đảm bảo test không rỗng
    if not test_samples:
        test_samples = val_samples[:len(val_samples)//2]
        val_samples = val_samples[len(val_samples)//2:]

    print(f"Phân chia dataset hoàn tất: Train={len(train_samples)}, Val={len(val_samples)}, Test={len(test_samples)}")
    return train_samples, val_samples, test_samples


def save_dataset_manifest():
    samples = build_dataset()
    train, val, test = split_dataset(samples)

    data_dir = os.path.join("artifacts", "data")
    os.makedirs(data_dir, exist_ok=True)

    manifest = {
        "version": "1.0.0",
        "seed": SEED,
        "split_strategy": "by_registered_domain",
        "counts": {
            "total": len(samples),
            "train": len(train),
            "val": len(val),
            "test": len(test),
            "train_phishing": sum(1 for s in train if s["label"] == 1),
            "train_benign": sum(1 for s in train if s["label"] == 0),
            "test_phishing": sum(1 for s in test if s["label"] == 1),
            "test_benign": sum(1 for s in test if s["label"] == 0)
        }
    }

    with open(os.path.join(data_dir, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2)

    with open(os.path.join(data_dir, "train.json"), "w", encoding="utf-8") as f:
        json.dump(train, f, indent=2)

    with open(os.path.join(data_dir, "val.json"), "w", encoding="utf-8") as f:
        json.dump(val, f, indent=2)

    with open(os.path.join(data_dir, "test.json"), "w", encoding="utf-8") as f:
        json.dump(test, f, indent=2)

    print(f"Đã lưu dataset và manifest tại {data_dir}!")


if __name__ == "__main__":
    save_dataset_manifest()
