"""
Adapter tra cứu nguồn dữ liệu OpenPhish Community Phishing Feed.
Hỗ trợ kiểm tra trực tiếp danh sách lừa đảo theo thời gian thực mà không cần API key.
"""

import os
import time
import httpx
from datetime import datetime, timezone
from typing import Dict, Any, Set
from backend.app.adapters.base import BaseThreatAdapter

class OpenPhishAdapter(BaseThreatAdapter):
    _cached_feed: Set[str] = set()
    _last_fetched: float = 0.0
    _cache_ttl_seconds: float = 600.0  # 10 phút làm mới feed

    def __init__(self, timeout_seconds: float = 4.0):
        super().__init__("OpenPhish", timeout_seconds)
        self.feed_url = os.getenv("OPENPHISH_FEED_URL", "https://openphish.com/feed.txt")

    async def _refresh_feed_if_needed(self):
        now = time.time()
        if self._cached_feed and (now - self._last_fetched) < self._cache_ttl_seconds:
            return

        try:
            headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppChongLuaDao-ThreatIntel/1.0"}
            async with httpx.AsyncClient(timeout=self.timeout_seconds) as client:
                resp = await client.get(self.feed_url, headers=headers)
                if resp.status_code == 200:
                    lines = set(line.strip().lower() for line in resp.text.splitlines() if line.strip())
                    OpenPhishAdapter._cached_feed = lines
                    OpenPhishAdapter._last_fetched = now
        except Exception:
            # Giữ cache cũ nếu có lỗi kết nối
            pass

    async def lookup(self, url: str) -> Dict[str, Any]:
        try:
            await self._refresh_feed_if_needed()

            if not self._cached_feed:
                return self.unavailable_result("FEED_UNAVAILABLE")

            normalized_target = url.strip().lower().rstrip("/")
            # Kiểm tra khớp chính xác hoặc bắt đầu bằng
            is_match = any(
                target == normalized_target or normalized_target.startswith(target)
                for target in self._cached_feed
            )

            status = "malicious" if is_match else "not_found"
            return {
                "status": status,
                "checked_at": datetime.now(timezone.utc).isoformat(),
                "evidence_type": "openphish_community_feed",
                "error_code": None
            }
        except Exception:
            return self.unavailable_result("LOOKUP_ERROR")
