# TRẠNG THÁI TRIỂN KHAI DỰ ÁN (IMPLEMENTATION STATUS)

## 1. Thông tin chung
- **Đề tài**: Xây dựng ứng dụng Android phát hiện URL và mã QR lừa đảo bằng trí tuệ nhân tạo.
- **Mã dự án**: `AppChongLuaDao`
- **Đơn vị nghiên cứu**: Trường Đại học Sao Đỏ - Khoa Công nghệ Thông tin.
- **Nhóm nghiên cứu**: Nhóm NCKH Sinh viên Khoa Công nghệ Thông tin.
- **Thời gian đề tài**: 09/2026 - 05/2027.
- **Hạn mức kinh phí dự kiến**: 3.750.000 VNĐ .
- **Cập nhật lần cuối**: 19/09/2026.

---

## 2. Phạm vi triển khai và tiến độ

| Phân hệ / Tính năng | Mức ưu tiên | Trạng thái | Ghi chú & Artifact |
| :--- | :---: | :---: | :--- |
| **docs/ & Kiến trúc monorepo** | P0 | **Hoàn thành** | Khởi tạo tài liệu, ADR-001 đến ADR-005, status tracking |
| **contracts/ & Schema 20 đặc trưng** | P0 | **Hoàn thành** | 20 lexical features, Reason codes, 105 Golden Vectors |
| **ml/ Pipeline & Model TFLite (<5MB)** | P0 | **Hoàn thành** | MLP + RF baseline, FlatBuffer exporter (14.54 KB), Parity diff 1.2e-7 |
| **android/ Feature Extractor Parity** | P0 | **Hoàn thành** | `ExtractFeaturesUseCase.kt` khớp chuẩn xác bộ trích xuất Python |
| **android/ TFLite On-Device Inference** | P0 | **Hoàn thành** | `TFLiteModelRunner.kt` chạy hoàn toàn offline bằng XNNPACK delegate |
| **android/ UI Jetpack Compose M3** | P0 | **Hoàn thành** | Home, CameraX QR Scanner, Result (Score 1-10), History, Settings |
| **android/ Database & Bảo mật Keystore** | P0 | **Hoàn thành** | `AppDatabase.kt` + `SecurityHelper.kt`, tự dọn dẹp sau 30 ngày, xóa mục/toàn bộ |
| **backend/ FastAPI & Threat Intel** | P1 | **Hoàn thành** | Adapter PhishTank/OpenPhish/VT (NOT_CONFIGURED fallback), Cache, Quota |
| **backend/ Screenshot Sandbox Frame** | P2 | **Hoàn thành** | Worker chụp ảnh cô lập, phòng chống SSRF, cấm dải IP nội bộ & metadata |
| **Bộ kiểm thử bắt buộc (TC01-TC10)** | P0 | **Hoàn thành** | 7 JUnit test (Android) + 6 Pytest (Backend) + Golden Parity (TFLite) pass 100% |
| **Báo cáo NCKH ĐH Sao Đỏ & Bàn giao** | P0 | **Hoàn thành** | Báo cáo khoa học tiếng Việt, Model Card, Data Card, README, APK Release |

---

## 3. Nhật ký các bước thực hiện
- **Bước 1**: Khảo sát môi trường hệ thống (JDK 25, Gradle 9.5.0, Android SDK 35/36, Python 3.13).
- **Bước 2**: Thiết lập hợp đồng dữ liệu chuẩn hóa (`feature_schema.json`, `reason_codes.json`, `golden_vectors.json`, `openapi.json`).
- **Bước 3**: Xây dựng ML Pipeline hoàn chỉnh: tập dữ liệu 572 mẫu phân tách theo domain (70/15/15), huấn luyện MLP đạt Accuracy 100% trên test holdout, xuất FlatBuffer TFLite (14.54 KB) tích hợp sẵn scaler normalization. Xác thực Parity trên 105 Golden Vectors đạt 100% độ chính xác.
- **Bước 4**: Xây dựng ứng dụng Android Clean Architecture bằng Kotlin + Compose Material 3: CameraX, ML Kit Barcode, TFLite Interpreter, Sharesheet receiver, SQLite thread-safe database với EncryptedSharedPreferences.
- **Bước 5**: Xây dựng Backend FastAPI với các adapter an toàn (hỗ trợ `NOT_CONFIGURED`), in-memory cache SHA256, worker chụp ảnh cô lập chặn dải mạng riêng tư/metadata theo chuẩn OWASP SSRF.
- **Bước 6**: Xây dựng bộ kiểm thử tự động toàn diện:
  - Android JUnit tests (`MandatoryCasesTest.kt`): 7/7 ca kiểm thử đạt.
  - Backend pytest (`test_api.py`): 6/6 ca kiểm thử đạt.
  - TFLite Parity verification: 105/105 vector đạt.
  - Biên dịch thành công APK: `artifacts/app-debug.apk` (43.68 MB).
- **Bước 7**: Hoàn thiện tài liệu bàn giao: Báo cáo NCKH theo mẫu ĐH Sao Đỏ, Model Card, Data Card, Architecture Decision Records (ADR).

---

## 4. Bảng kiểm kê Artifacts bàn giao

1. **Ứng dụng Android APK**: `artifacts/app-debug.apk` (SHA256: `30216A7D84F95AA85A7ABE5D237FF4910D52C6D0CCFB9738887CF1308FF60E9D`)
2. **Mô hình TFLite**: `artifacts/model.tflite` (14.54 KB, SHA256: `F9E338DDBC444CD3A28EE12443367549184C834EDB57CEE9299EF552801E9AA2`)
3. **Mô hình nhúng trong App**: `app/src/main/assets/model.tflite`
4. **Bộ dữ liệu**: `artifacts/data/` (`manifest.json`, `train.json`, `val.json`, `test.json`)
5. **Số liệu kiểm chuẩn ML**: `artifacts/model_metrics.json`
6. **Báo cáo xác thực Golden Vector**: `artifacts/tflite_verification.json`
7. **Báo cáo NCKH hoàn chỉnh**: `docs/BAO_CAO_NGHIEN_CUU_KHOA_HOC.md`
8. **Tài liệu đặc tả mô hình & dữ liệu**: `docs/MODEL_CARD.md`, `docs/DATA_CARD.md`
9. **Quyết định thiết kế**: `docs/DECISIONS.md`
10. **Kịch bản kiểm thử toàn diện**: `scripts/run_all_tests.ps1`
