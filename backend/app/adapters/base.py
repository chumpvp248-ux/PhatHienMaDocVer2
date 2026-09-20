"""
Base class cho các Adapter Threat Intelligence bên thứ ba.
"""

from abc import ABC, abstractmethod
from typing import Dict, Any, Optional
from datetime import datetime, timezone

class BaseThreatAdapter(ABC):
    def __init__(self, name: str, timeout_seconds: float = 3.0):
        self.name = name
        self.timeout_seconds = timeout_seconds

    @abstractmethod
    async def lookup(self, url: str) -> Dict[str, Any]:
        """
        Tra cứu URL trong nguồn threat intelligence.
        Trả về dictionary với:
        - status: "malicious" | "not_found" | "unknown" | "unavailable" | "rate_limited"
        - checked_at: ISO8601 string
        - evidence_type: str
        - error_code: Optional[str]
        """
        pass

    def unavailable_result(self, error_code: str = "NOT_CONFIGURED") -> Dict[str, Any]:
        return {
            "status": "unavailable",
            "checked_at": datetime.now(timezone.utc).isoformat(),
            "evidence_type": "threat_feed",
            "error_code": error_code
        }
