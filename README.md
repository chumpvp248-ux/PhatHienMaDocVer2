# Ứng Dụng Android Phát Hiện URL và Mã QR Lừa Đảo Bằng Trí Tuệ Nhân Tạo (AppChongLuaDao)

Dự án thuộc khuôn khổ đề tài nghiên cứu khoa học cấp cơ sở năm học 2026-2027 tại **Trường Đại học Sao Đỏ - Khoa Công nghệ Thông tin**.
- **Mã định danh dự án**: `AppChongLuaDao`.
- **Hạn mức kinh phí dự kiến**: 3.750.000 VNĐ (Thực tế: 0 VNĐ - tận dụng tài nguyên mã nguồn mở và thiết bị sẵn có).

---

## 1. Giới thiệu tổng quan
Ứng dụng Android kết hợp học máy trực tiếp trên thiết bị (**On-Device AI**) để phát hiện các liên kết (URL) và mã QR có dấu hiệu lừa đảo (**Phishing / Quishing**):
- **100% Ngoại tuyến (Offline-First)**: Mô hình mạng nơ-ron đa tầng MLP đã được tối ưu hóa xuất sang định dạng TensorFlow Lite (**14.54 KB**), thực thi trực tiếp trên điện thoại bằng Google AI Edge LiteRT / TensorFlow Lite Interpreter với độ trễ dưới 10ms mà không bắt buộc kết nối mạng.
- **Thang điểm rủi ro 1 đến 10**: Quy đổi xác suất rủi ro theo công thức chuẩn:
  $$\text{score} = \text{clamp}(1 + \lfloor 9p + 0.5 \rfloor, 1, 10)$$
  - **1 - 3**: Ít dấu hiệu rủi ro (An toàn / Lành tính).
  - **4 - 6**: Cần thận trọng (Nghi vấn).
  - **7 - 10**: Nguy cơ lừa đảo cao (Khả năng rất cao là trang web mạo danh/lừa đảo).
- **Điều hướng chuẩn mực di động (Bottom Navigation Bar)**: Cung cấp thanh điều hướng 4 tab hiện đại: **Trang chủ**, **Quét QR**, **Lịch sử**, **Cài đặt & Quyền riêng tư**. Tự động ẩn thanh điều hướng khi vào màn hình báo cáo chi tiết để người dùng tập trung tối đa.
- **Quét mã QR linh hoạt (CameraX & Thư viện ảnh)**:
  - Quét thời gian thực qua camera bằng Google ML Kit Barcode Scanning API (dưới 50ms, chống quét lặp).
  - **Hỗ trợ chọn ảnh mã QR từ thư viện thiết bị** (Gallery Picker): Cho phép quét trực tiếp từ ảnh chụp màn hình Zalo, Facebook, hóa đơn mà không cần hướng camera vào thiết bị khác, hoạt động ngay cả khi chưa cấp quyền Camera.
- **Phát hiện liên kết thông minh từ Clipboard**: Tự động nhận diện URL vừa sao chép từ tin nhắn SMS, mạng xã hội khi mở ứng dụng và gợi ý kiểm tra 1-chạm.
- **Minh bạch hóa quyết định (Explainable AI - XAI)**: Cung cấp lý do cụ thể theo nguồn (Heuristic / Mô hình AI / Threat Intel / WHOIS), không phỏng đoán vô căn cứ.
- **Khuyến nghị hành động thực tế (Actionable Guidance)**: Đưa ra hướng dẫn bảo vệ ngay lập tức tương ứng với từng mức độ nguy cơ (cảnh báo khẩn cấp không nhập OTP/mật khẩu, đóng trang web ngay, nút mở liên kết an toàn trên trình duyệt).
- **Cẩm nang an ninh số (Security Tips)**: Cung cấp mẹo thực tiễn nhận biết bẫy mã QR nơi công cộng, chiêu trò mạo danh tên miền ngân hàng và nguyên tắc bảo vệ tài khoản.
- **Nhận URL qua Android Sharesheet**: Dễ dàng chia sẻ link trực tiếp từ Chrome, Zalo, Messenger, SMS sang ứng dụng.
- **Bảo mật và Quyền riêng tư**: Lịch sử lưu trữ cục bộ trong SQLite được mã hóa an toàn qua Android Keystore, tự động dọn dẹp sau 30 ngày, cho phép xóa từng mục hoặc xóa toàn bộ bất kỳ lúc nào.
- **Tự động làm giàu thông tin tên miền & 10 Cổng An ninh (Domain Enrichment & Reputation Matrix)**: Tự động phân giải DNS IP, vị trí địa lý máy chủ, ISP, hồ sơ WHOIS/RDAP (ngày đăng ký, hết hạn, nhà đăng ký), kết hợp đối soát trạng thái danh tiếng từ 10 tổ chức bảo mật quốc tế (ScamAdviser, Criminal IP, Hudson Rock, Have I Been Pwned, PhishTank, CyRadar, ScamVN, IPQualityScore, APIVoid, PhishDestroy).
- **Tổng hợp quyết định AI Đa Nguồn (Multi-Source Decision Fusion)**: Thuật toán AI tổng hợp chéo điểm số TFLite với tuổi thọ tên miền thực tế và đồng thuận từ mạng lưới tình báo mối đe dọa, đưa ra phán quyết chuẩn xác và minh bạch.
- **Backend FastAPI Tùy Chọn (Nghiên cứu API)**: Cung cấp thêm API dịch vụ cho nghiên cứu và kiểm thử bảo mật SSRF (TC06, TC09, TC10).

---

## 2. Cấu trúc thư mục dự án (Monorepo)

```
AppChongLuaDao/
├── app/                    # Module mã nguồn Android chính (Kotlin, Jetpack Compose, CameraX, SQLite)
│   ├── src/main/assets/    # Chứa model.tflite (14.54 KB)
│   ├── src/main/java/      # Domain (UseCases, Models), Data (Local SQLite, Remote), UI (Screens, Theme, ViewModel)
│   └── src/test/           # Bộ kiểm thử JUnit cho các ca bắt buộc TC01-TC08 và Decision Fusion
├── backend/                # Dịch vụ FastAPI và các Adapter Threat Intel (tùy chọn phục vụ nghiên cứu)
│   ├── app/                # Main, Schemas, Cache, Adapters, Sandbox Worker
│   ├── tests/              # Bộ kiểm thử bảo mật SSRF và API contracts (TC06, TC09, TC10)
│   └── requirements.txt    # Danh mục thư viện Python phụ thuộc cho Backend
├── ml/                     # Pipeline dữ liệu và huấn luyện mô hình
│   ├── features.py         # Bộ trích xuất 20 đặc trưng từ vựng URL chuẩn hóa
│   ├── dataset.py          # Thu thập mẫu, chia train/val/test theo registered_domain
│   ├── train.py            # Huấn luyện MLP & Baseline Random Forest
│   ├── generate_golden_vectors.py # Tạo bộ 105 Golden Vectors chuẩn hóa
│   └── export_tflite.py    # Xuất mô hình FlatBuffers TFLite & kiểm thử Parity
├── contracts/              # Hợp đồng dữ liệu JSON
│   ├── feature_schema.json # Schema 20 đặc trưng theo đúng thứ tự
│   ├── reason_codes.json   # Danh mục mã lý do XAI và cảnh báo
│   ├── golden_vectors.json # 105 Golden Vectors kiểm thử tính nhất quán (Parity)
│   └── openapi.json        # Hợp đồng OpenAPI 3.1 của backend
├── docs/                   # Tài liệu nghiên cứu & kỹ thuật
│   ├── IMPLEMENTATION_STATUS.md # Nhật ký tiến độ chi tiết
│   ├── DECISIONS.md        # Các quyết định kiến trúc (ADR)
│   ├── MODEL_CARD.md       # Chi tiết đặc tính mô hình AI
│   ├── DATA_CARD.md        # Chi tiết phân phối tập dữ liệu
│   └── BAO_CAO_NGHIEN_CUU_KHOA_HOC.md # Bản thảo báo cáo khoa học theo chuẩn ĐH Sao Đỏ
├── scripts/                # Scripts tự động hóa
│   ├── run_all_tests.ps1   # Chạy toàn bộ các test suites trong 1 lệnh
│   └── export_openapi.py   # Xuất hợp đồng OpenAPI
└── artifacts/              # Các sản phẩm đầu ra đã được đóng băng phiên bản
    ├── app-debug.apk       # Bản cài đặt ứng dụng Android Debug (Độc lập 100% on-device, 43.7 MB)
    ├── model.tflite        # Mô hình nén on-device (14.54 KB)
    ├── mlp_weights.json    # Trọng số mạng nơ-ron xuất từ quá trình huấn luyện
    ├── model_metrics.json  # Báo cáo đánh giá số liệu thực nghiệm
    └── tflite_verification.json # Kết quả kiểm thử 105 Golden Vectors
```

---

## 3. Hướng dẫn cài đặt và khởi chạy

### Yêu cầu môi trường
- **Hệ điều hành**: Windows 10/11 x64 (hoặc Linux / macOS).
- **JDK**: OpenJDK 17 (hoặc tương thích Java 17+).
- **Android SDK**: `compileSdk 35`, `minSdk 26`, `targetSdk 35`, `Build-Tools 36.0.0`.
- **Python**: Python 3.10+ (Đã kiểm thử và xác thực trên Python 3.13).

### Bước 1: Huấn luyện mô hình và xuất TFLite (Đã hoàn thành sẵn trong assets)
```powershell
# Tạo dataset và phân chia theo tên miền
python -X utf8 -m ml.dataset

# Huấn luyện mô hình MLP và Random Forest
python -X utf8 -m ml.train

# Xuất mô hình model.tflite sang artifacts và Android assets
python -X utf8 -m ml.export_tflite
```

### Bước 2: Build ứng dụng Android (APK)
```powershell
# Kiểm tra biên dịch Kotlin
.\gradlew.bat compileDebugKotlin

# Build bản cài đặt APK Debug
.\gradlew.bat assembleDebug
```
File APK sau khi build nằm tại: `artifacts/app-debug.apk` (hoặc `app/build/outputs/apk/debug/app-debug.apk`). Cài đặt trực tiếp lên điện thoại và quét URL / mã QR hoàn toàn ngoại tuyến không cần bật backend.

### Bước 3: Khởi chạy Backend FastAPI (Tùy chọn - Phục vụ nghiên cứu API)
```powershell
# Cài đặt thư viện phụ thuộc
pip install -r backend/requirements.txt

# Chạy máy chủ backend tại cổng 8000
python -m uvicorn backend.app.main:app --host 0.0.0.0 --port 8000 --reload
```
- Swagger UI tài liệu API: `http://localhost:8000/docs`
- Health check: `http://localhost:8000/health`

---

## 4. Chạy toàn bộ kiểm thử tự động (Test Suite)

Để chạy toàn bộ kiểm thử đơn vị, kiểm thử ca bắt buộc và đối sánh Parity 105 Golden Vectors trong 1 lệnh duy nhất:
```powershell
# Lưu ý: Trên Windows PowerShell, chạy với quyền bypass script execution policy:
powershell -ExecutionPolicy Bypass -File .\scripts\run_all_tests.ps1
```

Hoặc chạy độc lập từng phân hệ:
```powershell
# 1. Android Unit Tests & 10 Mandatory Cases (TC01-TC08)
.\gradlew.bat testDebugUnitTest

# 2. Backend Tests & SSRF Egress Security (TC06, TC09, TC10)
python -m pytest backend/tests/test_api.py

# 3. Golden Vectors Parity Test (105 Mẫu)
python -X utf8 -m ml.export_tflite
```

---

## 5. Bảng 10 Ca Kiểm Thử Bắt Buộc (Mandatory Test Cases TC01-TC10)

| Mã Ca | Nội dung kiểm thử | Phân hệ phụ trách | Trạng thái |
| :---: | :--- | :---: | :---: |
| **TC01** | Luồng ngoại tuyến 100% On-Device qua TFLite, thời gian xử lý < 10ms | Android (`TFLiteModelRunner`) | **ĐẠT (PASSED)** |
| **TC02** | Quy tắc quyết định điểm số 1-10 và giải thích lý do minh bạch (XAI) | Android (`DecisionFusion`) | **ĐẠT (PASSED)** |
| **TC03** | Phân tách QR WiFi, cấu hình, văn bản thuần - Từ chối thực thi như URL | Android (`ValidateUrlUseCase`) | **ĐẠT (PASSED)** |
| **TC04** | Phát hiện link rút gọn (`bit.ly`, `tinyurl`...) và gắn cờ cảnh báo | Android (`ExtractFeaturesUseCase`) | **ĐẠT (PASSED)** |
| **TC05** | Xử lý tên miền quốc tế hóa (IDN / Punycode), không quy chụp sai | Android (`ExtractFeaturesUseCase`) | **ĐẠT (PASSED)** |
| **TC06** | Xử lý an toàn định dạng đặc biệt: IPv4, IPv6, Userinfo, URL > 4096 ký tự | Android & Backend | **ĐẠT (PASSED)** |
| **TC07** | Độc lập hoàn toàn với backend: Chế độ máy bay không crash, quét offline mượt mà | Android UI/Domain | **ĐẠT (PASSED)** |
| **TC08** | Bảo vệ quyền riêng tư: Lưu lịch sử SQLite cục bộ mã hóa Android Keystore, không rò rỉ dữ liệu | Android Data Layer | **ĐẠT (PASSED)** |
| **TC09** | Kiểm soát hợp đồng API Backend FastAPI qua Pydantic Schema | Backend (`test_api.py`) | **ĐẠT (PASSED)** |
| **TC10** | Chống SSRF & Chặn Egress đến IP nội bộ, loopback, cloud metadata | Backend (`screenshot_worker.py`) | **ĐẠT (PASSED)** |

---

## 6. Bảng băm toàn vẹn Artifacts (Checksums)

| Tệp Artifact | Kích thước | Thuật toán | Mã băm SHA-256 đã xác minh | Trạng thái |
| :--- | :---: | :---: | :--- | :--- |
| `app/src/main/assets/model.tflite` | 14.54 KB | SHA-256 | `F9E338DDBC444CD3A28EE12443367549184C834EDB57CEE9299EF552801E9AA2` | Khóa phiên bản 1.0.0-mlp |
| `artifacts/model.tflite` | 14.54 KB | SHA-256 | `F9E338DDBC444CD3A28EE12443367549184C834EDB57CEE9299EF552801E9AA2` | Đồng bộ 100% với Android asset |
| `contracts/golden_vectors.json` | 105 mẫu | SHA-256 | `E174D6C7E05484C95808B67212AA9E99488C91FB9F1BD899C2F543580291D767` | Khóa chuẩn hóa 105 mẫu |
| `contracts/feature_schema.json` | 20 đặc trưng | SHA-256 | `8A595EA2A25EB33925E93610BB256B8141E5EA1D3C9BC6811DDFDDEA64357F42` | Khóa thứ tự 20 đặc trưng |
