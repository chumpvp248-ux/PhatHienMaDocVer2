"""
FastAPI Main Application.
"""

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from backend.app.routes import health, analyze

app = FastAPI(
    title="AppChongLuaDao Threat Intelligence API",
    description="Backend API hỗ trợ tra cứu Threat Intelligence và Sandbox an toàn cho đề tài NCKH ĐH Sao Đỏ",
    version="1.0.0"
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(analyze.router)

@app.get("/")
async def root():
    return {
        "service": "AppChongLuaDao API",
        "docs": "/docs",
        "health": "/health"
    }
