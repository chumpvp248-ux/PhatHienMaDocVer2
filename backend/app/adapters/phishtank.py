"""
Adapter tra cứu nguồn dữ liệu PhishTank.
"""

import os
import httpx
from datetime import datetime, timezone
from typing import Dict, Any
from backend.app.adapters.base import BaseThreatAdapter

class PhishTankAdapter(BaseThreatAdapter):
    def __init__(self, timeout_seconds: float = 3.0):
        super().__init__("PhishTank", timeout_seconds)
        self.api_key = os.getenv("PHISHTANK_API_KEY")

    async def lookup(self, url: str) -> Dict[str, Any]:
        if not self.api_key:
            return self.unavailable_result("NOT_CONFIGURED")

        try:
            async with httpx.AsyncClient(timeout=self.timeout_seconds) as client:
                resp = await client.post(
                    "https://checkurl.phishtank.com/checkurl/",
                    data={
                        "url": url,
                        "format": "json",
                        "app_key": self.api_key
                    }
                )
                if resp.status_code == 200:
                    data = resp.json()
                    in_database = data.get("results", {}).get("in_database", False)
                    valid = data.get("results", {}).get("valid", False)
                    status = "malicious" if (in_database and valid) else "not_found"
                    return {
                        "status": status,
                        "checked_at": datetime.now(timezone.utc).isoformat(),
                        "evidence_type": "phishtank_database",
                        "error_code": None
                    }
                elif resp.status_code == 429:
                    return {
                        "status": "rate_limited",
                        "checked_at": datetime.now(timezone.utc).isoformat(),
                        "evidence_type": "phishtank_database",
                        "error_code": "RATE_LIMITED"
                    }
                else:
                    return self.unavailable_result(f"HTTP_{resp.status_code}")
        except Exception as e:
            return self.unavailable_result("CONNECTION_TIMEOUT")
