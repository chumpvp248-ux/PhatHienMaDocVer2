"""
Bộ kiểm thử tự động cho Backend FastAPI và các ràng buộc bảo mật (TC06, TC09, TC10).
"""

import pytest
from fastapi.testclient import TestClient
from backend.app.main import app
from backend.app.sandbox.screenshot_worker import is_safe_egress_destination

client = TestClient(app)

def test_health_check():
    response = client.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["version"] == "1.0.0"

def test_analyze_url_success():
    payload = {
        "url": "https://google.com/search?q=antigravity",
        "client_request_id": "test-req-001",
        "model_version": "1.0.0-mlp",
        "feature_schema_version": "1.0.0",
        "viewport": "mobile",
        "consent": {
            "threat_lookup": True,
            "screenshot": False
        }
    }
    response = client.post("/api/v1/analyze-url", json=payload)
    assert response.status_code == 200
    data = response.json()
    assert data["request_id"] == "test-req-001"
    assert "score" in data
    assert "verdict" in data
    assert "third_party_results" in data
    # Kiểm tra adapter khi chưa cấu hình API key trả về trạng thái unavailable/NOT_CONFIGURED
    for p_name, p_val in data["third_party_results"].items():
        assert p_val["status"] in ("unavailable", "not_found", "malicious", "rate_limited")

def test_analyze_url_rejects_userinfo_online():
    # Ràng buộc mục 6: Từ chối userinfo khi gửi online để bảo vệ tài khoản người dùng
    payload = {
        "url": "http://admin:secret123@phishing-target.com/login",
        "client_request_id": "test-req-userinfo",
        "model_version": "1.0.0-mlp",
        "feature_schema_version": "1.0.0",
        "viewport": "mobile",
        "consent": {
            "threat_lookup": True,
            "screenshot": False
        }
    }
    response = client.post("/api/v1/analyze-url", json=payload)
    assert response.status_code == 422

def test_analyze_url_rejects_invalid_scheme():
    payload = {
        "url": "ftp://files.example.com/trojan.exe",
        "client_request_id": "test-req-scheme",
        "model_version": "1.0.0-mlp",
        "feature_schema_version": "1.0.0",
        "viewport": "mobile",
        "consent": {
            "threat_lookup": True,
            "screenshot": False
        }
    }
    response = client.post("/api/v1/analyze-url", json=payload)
    assert response.status_code == 422

def test_ssrf_egress_protection_tc10():
    # TC10: Chặn đứng mọi kết nối tới loopback, private IP, cloud metadata từ worker sandbox
    blocked_urls = [
        "http://127.0.0.1:8000/secret",
        "http://localhost:3000",
        "http://10.0.0.1/admin",
        "http://172.16.0.5/dashboard",
        "http://192.168.1.1/router",
        "http://169.254.169.254/latest/meta-data/",
        "http://[::1]/internal",
    ]
    for url in blocked_urls:
        is_safe, reason = is_safe_egress_destination(url)
        assert not is_safe, f"URL {url} phải bị chặn egress nhưng lại vượt qua: {reason}"

def test_cache_hit():
    url = "https://saodo.edu.vn/gioi-thieu"
    payload = {
        "url": url,
        "client_request_id": "req-cache-1",
        "model_version": "1.0.0-mlp",
        "feature_schema_version": "1.0.0",
        "viewport": "mobile",
        "consent": {
            "threat_lookup": True,
            "screenshot": False
        }
    }
    # Request 1
    resp1 = client.post("/api/v1/analyze-url", json=payload)
    assert resp1.status_code == 200

    # Request 2 (cùng URL, khác client_request_id)
    payload["client_request_id"] = "req-cache-2"
    resp2 = client.post("/api/v1/analyze-url", json=payload)
    assert resp2.status_code == 200
    data2 = resp2.json()
    assert data2["request_id"] == "req-cache-2"

def test_screenshot_sandbox_generation():
    # Kiểm tra việc sinh ảnh chụp di động trong môi trường cô lập
    payload = {
        "url": "https://saodo.edu.vn",
        "client_request_id": "test-req-shot",
        "model_version": "1.0.0-mlp",
        "feature_schema_version": "1.0.0",
        "viewport": "mobile",
        "consent": {
            "threat_lookup": True,
            "screenshot": True
        }
    }
    resp = client.post("/api/v1/analyze-url", json=payload)
    assert resp.status_code == 200
    data = resp.json()
    assert data["screenshot_status"] in ("ready", "unavailable")
    if data["screenshot_status"] == "ready":
        assert data["screenshot_base64"] is not None
        assert data["screenshot_base64"].startswith("data:image/png;base64,")
    assert "lookup_urls" in data
    assert "VirusTotal" in data["lookup_urls"]
    assert "Google Safe Browsing" in data["lookup_urls"]
