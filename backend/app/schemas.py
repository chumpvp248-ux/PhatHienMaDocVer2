"""
Pydantic schemas cho Backend API theo đúng hợp đồng mục 6.
"""

from pydantic import BaseModel, Field, HttpUrl, field_validator
from typing import Dict, List, Optional, Any
import urllib.parse

class ConsentConfig(BaseModel):
    threat_lookup: bool = True
    screenshot: bool = False

class AnalyzeUrlRequest(BaseModel):
    url: str = Field(..., min_length=1, max_length=4096, description="URL cần phân tích (chỉ hỗ trợ HTTP/HTTPS)")
    client_request_id: str = Field(..., description="ID yêu cầu từ client để tương quan phiên")
    model_version: str = Field("1.0.0-mlp", description="Phiên bản mô hình phía client")
    feature_schema_version: str = Field("1.0.0", description="Phiên bản feature schema")
    viewport: str = Field("mobile", description="Loại viewport (mobile hoặc desktop)")
    consent: ConsentConfig = Field(default_factory=ConsentConfig)

    @field_validator("url")
    @classmethod
    def validate_url(cls, v: str) -> str:
        trimmed = v.trim() if hasattr(v, "trim") else v.strip()
        parsed = urllib.parse.urlsplit(trimmed)
        if parsed.scheme.lower() not in ("http", "https"):
            raise ValueError("Chỉ chấp nhận URL giao thức HTTP hoặc HTTPS")
        if not parsed.netloc:
            raise ValueError("URL thiếu tên miền hoặc địa chỉ máy chủ")
        # Từ chối userinfo khi gửi online theo hợp đồng an toàn
        if "@" in parsed.netloc:
            raise ValueError("URL chứa thông tin xác thực userinfo không được phép gửi trực tuyến")
        return trimmed

class ReasonItem(BaseModel):
    code: str
    title: str
    message: str
    severity: str
    source: str

class ProviderResult(BaseModel):
    status: str # "malicious", "not_found", "unknown", "unavailable", "rate_limited"
    checked_at: str
    cache_age_seconds: int = 0
    evidence_type: str = "threat_feed"
    error_code: Optional[str] = None

class AnalyzeUrlResponse(BaseModel):
    request_id: str
    url: str
    score: Optional[int]
    verdict: str # "malicious", "suspicious", "safe", "unknown"
    ai_reasons: List[ReasonItem] = []
    heuristic_reasons: List[ReasonItem] = []
    third_party_results: Dict[str, ProviderResult] = {}
    screenshot_status: str = "none" # "none", "pending", "ready", "unavailable"
    screenshot_url: Optional[str] = None
    screenshot_base64: Optional[str] = None
    lookup_urls: Dict[str, str] = {}
    timestamps: Dict[str, float] = {}
    policy_version: str = "1.0.0"
    model_version: str = "1.0.0-mlp"

class HealthResponse(BaseModel):
    status: str = "healthy"
    version: str = "1.0.0"
    service: str = "AppChongLuaDao-Backend"
