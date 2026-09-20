"""
Bộ kiểm thử backend sử dụng thư viện chuẩn unittest.
"""

import unittest
from fastapi.testclient import TestClient
from backend.app.main import app
from backend.app.sandbox.screenshot_worker import is_safe_egress_destination

class BackendApiTests(unittest.TestCase):
    def setUp(self):
        self.client = TestClient(app)

    def test_01_health_check(self):
        response = self.client.get("/health")
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["status"], "healthy")
        self.assertEqual(data["version"], "1.0.0")

    def test_02_analyze_url_success(self):
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
        response = self.client.post("/api/v1/analyze-url", json=payload)
        self.assertEqual(response.status_code, 200)
        data = response.json()
        self.assertEqual(data["request_id"], "test-req-001")
        self.assertIn("score", data)
        self.assertIn("verdict", data)
        self.assertIn("third_party_results", data)
        for p_name, p_val in data["third_party_results"].items():
            self.assertIn(p_val["status"], ("unavailable", "not_found", "malicious", "rate_limited"))

    def test_03_analyze_url_rejects_userinfo(self):
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
        response = self.client.post("/api/v1/analyze-url", json=payload)
        self.assertEqual(response.status_code, 422)

    def test_04_analyze_url_rejects_invalid_scheme(self):
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
        response = self.client.post("/api/v1/analyze-url", json=payload)
        self.assertEqual(response.status_code, 422)

    def test_05_ssrf_egress_protection(self):
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
            self.assertFalse(is_safe, f"URL {url} phải bị chặn egress nhưng lại vượt qua: {reason}")

    def test_06_cache_hit(self):
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
        resp1 = self.client.post("/api/v1/analyze-url", json=payload)
        self.assertEqual(resp1.status_code, 200)

        payload["client_request_id"] = "req-cache-2"
        resp2 = self.client.post("/api/v1/analyze-url", json=payload)
        self.assertEqual(resp2.status_code, 200)
        data2 = resp2.json()
        self.assertEqual(data2["request_id"], "req-cache-2")

if __name__ == "__main__":
    unittest.main()
