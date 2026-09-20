# QUYẾT ĐỊNH KIẾN TRÚC VÀ KỸ THUẬT (ARCHITECTURAL DECISION RECORDS - ADR)

## ADR-001: Cấu trúc dự án Monorepo tương thích Android Studio
- **Bối cảnh**: Dự án gồm Ứng dụng Android (Kotlin), Backend tra cứu Threat Intel (FastAPI Python), Pipeline ML (Python/LiteRT) và Hợp đồng dữ liệu (JSON). Cần đảm bảo khi mở Android Studio tại thư mục gốc `AppChongLuaDao`, Android Studio tự động nhận diện và build Gradle ngay lập tức mà không cần cấu hình phức tạp.
- **Quyết định**: Tổ chức monorepo với root chứa Gradle files (`build.gradle.kts`, `settings.gradle.kts`, `gradle/libs.versions.toml`) và module `:app`. Các phân hệ khác được đặt trong các thư mục ngang cấp: `backend/`, `ml/`, `contracts/`, `docs/`, `scripts/`.
- **Hệ quả**: Thuận tiện tối đa cho nhà phát triển khi mở dự án trên Android Studio hoặc VS Code; CI/CD và script có thể chạy độc lập từng phân hệ.

---

## ADR-002: Mô hình On-Device Offline-First bằng TFLite / LiteRT
- **Bối cảnh**: Người dùng quét QR hoặc nhập URL thường xuyên tại các điểm không có mạng hoặc mạng chập chờn. Yêu cầu bắt buộc là bảo vệ quyền riêng tư và hoạt động trơn tru trong chế độ máy bay (offline).
- **Quyết định**: Sử dụng kiến trúc mạng nơ-ron MLP (Multi-Layer Perceptron) với 20 đặc trưng từ vựng URL, xuất ra định dạng TensorFlow Lite (`model.tflite`) kích thước < 5 MB (thực tế ~100-300 KB). Mô hình chạy trực tiếp trên Android bằng TFLite Interpreter / Google LiteRT.
- **Hệ quả**: Suy luận tức thì (< 30 ms), không phụ thuộc mạng, không rò rỉ URL người dùng ra ngoài trừ khi có sự đồng ý (consent).

---

## ADR-003: Parity tuyệt đối giữa Python và Kotlin cho 20 đặc trưng
- **Bối cảnh**: Sai lệch dù chỉ 1 đặc trưng giữa Python (lúc train) và Kotlin (lúc chạy trên Android) sẽ làm sai lệch phân phối đầu vào của mô hình, dẫn tới dự đoán sai.
- **Quyết định**: Định nghĩa schema nghiêm ngặt tại `contracts/feature_schema.json` và kiểm thử tự động với tối thiểu 100 Golden Vectors. Sai lệch số nguyên và nhị phân phải bằng 0; sai lệch số thực `<= 1e-6`.
- **Hệ quả**: Đảm bảo kết quả suy luận trên điện thoại khớp hoàn toàn với kết quả đánh giá trong phòng thí nghiệm.

---

## ADR-004: Xử lý Adapter Threat Intelligence khi thiếu API Key (`NOT_CONFIGURED`)
- **Bối cảnh**: Trong môi trường nghiên cứu hoặc khi chưa có tài khoản trả phí từ các bên thứ ba (VirusTotal, PhishTank, OpenPhish), backend không được giả vờ đã tra cứu hoặc trả về kết quả giả.
- **Quyết định**: Khi thiếu key, adapter trả về trạng thái rõ ràng `unavailable` với mã `NOT_CONFIGURED`. Ứng dụng Android hiển thị "Chưa tra được (Chưa cấu hình API key)" và bảo toàn điểm đánh giá ngoại tuyến từ mô hình TFLite.
- **Hệ quả**: Minh bạch, trung thực trong nghiên cứu khoa học, không phát sinh chi phí và không che giấu trạng thái hệ thống.

---

## ADR-005: Khung Sandbox cô lập cho ảnh chụp màn hình (P2)
- **Bối cảnh**: Việc chụp ảnh màn hình URL độc hại từ người dùng có nguy cơ bị tấn công SSRF (Server-Side Request Forgery), truy cập mạng nội bộ hoặc mã độc.
- **Quyết định**: Module worker cô lập kiểm tra nghiêm ngặt địa chỉ IP đích (chặn dải IP private, loopback, link-local, cloud metadata). Ở chế độ nghiên cứu mặc định khi chưa có container cô lập riêng, backend chỉ cho phép chụp demo các trang do người dùng kiểm soát an toàn.
- **Hệ quả**: Đảm bảo an toàn tuyệt đối cho hệ thống máy chủ và mạng nội bộ.
