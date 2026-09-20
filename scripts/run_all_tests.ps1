# Script tu dong chay toan bo cac bo kiem thu cua du an AppChongLuaDao
Write-Host "====================================================" -ForegroundColor Cyan
Write-Host "   KIEM THU TOAN DIEN DU AN APP CHONG LUA DAO AI    " -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

# 1. Kiem thu Unit Test & Ca bat buoc (TC01-TC08) tren Android
Write-Host "`n[1/3] Dang chay Android JUnit Unit Tests..." -ForegroundColor Yellow
& .\gradlew.bat testDebugUnitTest --quiet
if ($LASTEXITCODE -eq 0) {
    Write-Host "  -> Android Unit Tests: DAT (PASSED)" -ForegroundColor Green
} else {
    Write-Host "  -> Android Unit Tests: THAT BAI (FAILED)" -ForegroundColor Red
    exit 1
}

# 2. Kiem thu Backend API & Bao mat SSRF (TC06, TC09, TC10)
Write-Host "`n[2/3] Dang chay Backend API & Security Tests..." -ForegroundColor Yellow
python -m pytest backend/tests/test_api.py --quiet
if ($LASTEXITCODE -eq 0) {
    Write-Host "  -> Backend Tests: DAT (PASSED)" -ForegroundColor Green
} else {
    Write-Host "  -> Backend Tests: THAT BAI (FAILED)" -ForegroundColor Red
    exit 1
}

# 3. Kiem thu xac thuc mo hinh TFLite tren 105 Golden Vectors
Write-Host "`n[3/3] Dang kiem thu xac thuc mo hinh TFLite tren 105 Golden Vectors..." -ForegroundColor Yellow
python -X utf8 -m ml.export_tflite
if ($LASTEXITCODE -eq 0) {
    Write-Host "  -> TFLite Parity Verification: DAT (PASSED)" -ForegroundColor Green
} else {
    Write-Host "  -> TFLite Parity Verification: THAT BAI (FAILED)" -ForegroundColor Red
    exit 1
}

Write-Host "`n====================================================" -ForegroundColor Green
Write-Host "   TAT CA CAC BO KIEM THU DEU DAT THANH CONG 100%   " -ForegroundColor Green
Write-Host "====================================================" -ForegroundColor Green
