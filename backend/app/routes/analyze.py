"""
Endpoint phân tích URL và tra cứu Threat Intelligence.
"""

import time
import asyncio
import urllib.parse
from fastapi import APIRouter, HTTPException, status, Query
from typing import Dict, Any

from backend.app.schemas import (
    AnalyzeUrlRequest,
    AnalyzeUrlResponse,
    ProviderResult,
    ReasonItem
)
from backend.app.cache import global_cache
from backend.app.adapters.phishtank import PhishTankAdapter
from backend.app.adapters.openphish import OpenPhishAdapter
from backend.app.adapters.virustotal import VirusTotalAdapter
from backend.app.sandbox.screenshot_worker import ScreenshotWorker

router = APIRouter(prefix="/api/v1", tags=["Analysis"])

phishtank_adapter = PhishTankAdapter()
openphish_adapter = OpenPhishAdapter()
virustotal_adapter = VirusTotalAdapter()
screenshot_worker = ScreenshotWorker()

@router.post(
    "/analyze-url",
    response_model=AnalyzeUrlResponse,
    status_code=status.HTTP_200_OK,
    summary="Phân tích URL trực tuyến với Threat Intelligence và Sandbox cô lập"
)
async def analyze_url(req: AnalyzeUrlRequest):
    start_time = time.time()
    url = req.url

    encoded_url = urllib.parse.quote(url, safe="")
    lookup_urls = {
        "VirusTotal": f"https://www.virustotal.com/gui/search/{encoded_url}",
        "Google Safe Browsing": f"https://transparencyreport.google.com/safe-browsing/search?url={encoded_url}",
        "Chống Lừa Đảo VN": "https://chongluadao.vn",
        "URLhaus": "https://urlhaus.abuse.ch/browse/"
    }

    # 1. Kiểm tra cache trước
    cached_data = global_cache.get(url)
    if cached_data:
        cached_data["request_id"] = req.client_request_id
        cached_data["lookup_urls"] = lookup_urls
        cached_data["timestamps"]["completed_at"] = time.time()
        return AnalyzeUrlResponse(**cached_data)

    # 2. Tra cứu đa nguồn Threat Intelligence bất đồng bộ nếu có consent
    provider_results: Dict[str, ProviderResult] = {}
    verdict = "safe"
    final_score: int = 1
    reasons = []

    if req.consent.threat_lookup:
        results = await asyncio.gather(
            phishtank_adapter.lookup(url),
            openphish_adapter.lookup(url),
            virustotal_adapter.lookup(url),
            return_exceptions=True
        )

        adapters = [phishtank_adapter, openphish_adapter, virustotal_adapter]
        for adapter, res in zip(adapters, results):
            if isinstance(res, Exception):
                provider_results[adapter.name] = ProviderResult(
                    status="unavailable",
                    checked_at=time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime()),
                    error_code="INTERNAL_ERROR"
                )
            else:
                provider_results[adapter.name] = ProviderResult(**res)

        # Policy hợp nhất: Nếu có bất kỳ nguồn nào phát hiện "malicious" -> Nâng score lên tối thiểu 9
        is_malicious = any(r.status == "malicious" for r in provider_results.values())
        if is_malicious:
            verdict = "malicious"
            final_score = 9
            reasons.append(
                ReasonItem(
                    code="PROVIDER_MALICIOUS",
                    title="Cơ sở dữ liệu an ninh cảnh báo độc hại",
                    message="Liên kết đã bị nhận diện là lừa đảo trong hệ sinh thái Threat Intelligence.",
                    severity="critical",
                    source="threat_intel"
                )
            )
        else:
            verdict = "not_found"
            final_score = 1

    # 3. Xử lý tác vụ chụp màn hình Sandbox (P2)
    screenshot_status = "none"
    screenshot_url = None
    screenshot_base64 = None
    if req.consent.screenshot:
        screenshot_status, screenshot_url = await screenshot_worker.capture_screenshot(url)
        screenshot_base64 = screenshot_url

    response_data = {
        "request_id": req.client_request_id,
        "url": url,
        "score": final_score,
        "verdict": verdict,
        "ai_reasons": reasons,
        "heuristic_reasons": [],
        "third_party_results": provider_results,
        "screenshot_status": screenshot_status,
        "screenshot_url": screenshot_url,
        "screenshot_base64": screenshot_base64,
        "lookup_urls": lookup_urls,
        "timestamps": {
            "received_at": start_time,
            "completed_at": time.time(),
            "duration_ms": round((time.time() - start_time) * 1000, 2)
        },
        "policy_version": "1.0.0",
        "model_version": req.model_version
    }

    # Lưu cache nếu thành công
    global_cache.set(url, response_data)

    return AnalyzeUrlResponse(**response_data)

@router.get("/sandbox/screenshot", summary="Tạo hoặc lấy ảnh chụp màn hình sandbox theo yêu cầu")
async def get_sandbox_screenshot(url: str = Query(..., description="URL cần chụp an toàn")):
    status_str, img_data = await screenshot_worker.capture_screenshot(url)
    return {
        "url": url,
        "status": status_str,
        "screenshot_base64": img_data
    }

@router.get("/analyses/{job_id}", summary="Kiểm tra trạng thái tác vụ phân tích bất đồng bộ")
async def get_analysis_job(job_id: str):
    return {
        "job_id": job_id,
        "status": "completed",
        "message": "Phân tích tức thì đã hoàn tất"
    }
