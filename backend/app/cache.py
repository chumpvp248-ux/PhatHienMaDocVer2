"""
Module bộ nhớ đệm (Cache) an toàn cho kết quả tra cứu Threat Intelligence.
Sử dụng hàm băm SHA256 để không lưu trữ URL nhạy cảm dạng thô trong bộ nhớ.
"""

import hashlib
import time
from typing import Optional, Dict, Any

class ThreatIntelCache:
    def __init__(self, ttl_seconds: int = 600):
        self.ttl_seconds = ttl_seconds
        self._store: Dict[str, Dict[str, Any]] = {}

    def _hash_key(self, url: str) -> str:
        return hashlib.sha256(url.encode("utf-8")).hexdigest()

    def get(self, url: str) -> Optional[Dict[str, Any]]:
        key = self._hash_key(url)
        entry = self._store.get(key)
        if not entry:
            return None
        now = time.time()
        age = now - entry["stored_at"]
        if age > self.ttl_seconds:
            del self._store[key]
            return None
        data = dict(entry["data"])
        data["cache_age_seconds"] = int(age)
        return data

    def set(self, url: str, data: Dict[str, Any]):
        key = self._hash_key(url)
        self._store[key] = {
            "stored_at": time.time(),
            "data": data
        }

    def clear(self):
        self._store.clear()

global_cache = ThreatIntelCache(ttl_seconds=600)
