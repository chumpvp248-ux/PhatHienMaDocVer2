"""
Adapter tra cứu VirusTotal v3 API.
"""

import os
import base64
import httpx
from datetime import datetime, timezone
from typing import Dict, Any
from backend.app.adapters.base import BaseThreatAdapter

class VirusTotalAdapter(BaseThreatAdapter):
    def __init__(self, timeout_seconds: float = 3.0):
        super().__init__("VirusTotal", timeout_seconds)
        self.api_key = os.getenv("VIRUSTOTAL_API_KEY")

    async def lookup(self, url: str) -> Dict[str, Any]:
        if not self.api_key:
            return self.unavailable_result("NOT_CONFIGURED")

        try:
            # VirusTotal v3 URL identifier là base64 URL-safe không đệm '='
            url_id = base64.urlsafe_b64encode(url.encode("utf-8")).decode("ascii").rstrip("=")
            headers = {"x-apikey": self.api_key}

            async with httpx.AsyncClient(timeout=self.timeout_seconds) as client:
                resp = await client.get(
                    f"https://www.virustotal.com/api/v3/urls/{url_id}",
                    headers=headers
                )
                if resp.status_code == 200:
                    stats = resp.json().get("data", {}).get("attributes", {}).get("last_analysis_stats", {})
                    malicious_count = stats.get("malicious", 0)
                    suspicious_count = stats.get("suspicious", 0)
                    if malicious_count > 0:
                        status = "malicious"
                    elif suspicious_count > 0:
                        status = "unknown"
                    else:
                        status = "not_found"

                    return {
                        "status": status,
                        "checked_at": datetime.now(timezone.utc).isoformat(),
                        "evidence_type": "virustotal_engines",
                        "error_code": None
                    }
                elif resp.status_code == 404:
                    return {
                        "status": "not_found",
                        "checked_at": datetime.now(timezone.utc).isoformat(),
                        "evidence_type": "virustotal_engines",
                        "error_code": None
                    }
                elif resp.status_code == 429:
                    return {
                        "status": "rate_limited",
                        "checked_at": datetime.now(timezone.utc).isoformat(),
                        "evidence_type": "virustotal_engines",
                        "error_code": "RATE_LIMITED"
                    }
                else:
                    return self.unavailable_result(f"HTTP_{resp.status_code}")
        except Exception:
            return self.unavailable_result("CONNECTION_TIMEOUT")
