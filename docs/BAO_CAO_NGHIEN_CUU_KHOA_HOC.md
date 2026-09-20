# BÁO CÁO KHOA HỌC TỔNG KẾT ĐỀ TÀI NGHIÊN CỨU CẤP CƠ SỞ
### NĂM HỌC 2026 - 2027

---

**TÊN ĐỀ TÀI**:  
## **XÂY DỰNG ỨNG DỤNG ANDROID PHÁT HIỆN URL VÀ MÃ QR LỪA ĐẢO BẰNG TRÍ TUỆ NHÂN TẠO**

- **Đơn vị chủ quản**: Trường Đại học Sao Đỏ
- **Khoa**: Công nghệ Thông tin
- **Nhóm sinh viên thực hiện đề tài**: Nhóm NCKH Sinh viên Khoa Công nghệ Thông tin
- **Cán bộ hướng dẫn**: Giảng viên Khoa Công nghệ Thông tin
- **Thời gian thực hiện**: 09/2026 – 05/2027
- **Kinh phí dự kiến**: 3.750.000 VNĐ (Kinh phí thực tế: 0 VNĐ - tận dụng tài nguyên phần cứng sẵn có)

---

## TÓM TẮT ĐỀ TÀI (ABSTRACT)
Tấn công phi kỹ thuật (Social Engineering) thông qua các đường dẫn liên kết lừa đảo (**Phishing**) và mã QR độc hại (**Quishing**) đang gia tăng mạnh mẽ tại Việt Nam, gây thiệt hại nghiêm trọng cho người dùng dịch vụ tài chính số, ngân hàng điện tử và dịch vụ công trực tuyến. Đề tài này đề xuất và phát triển trọn vẹn giải pháp ứng dụng di động Android mang tên **AppChongLuaDao**, tích hợp mô hình trí tuệ nhân tạo chạy hoàn toàn ngoại tuyến (**On-Device AI**) để phát hiện và đánh giá mức độ rủi ro của liên kết theo thời gian thực. 

Hệ thống trích xuất chuẩn hóa **20 đặc trưng từ vựng (Lexical Features)** từ URL, thực thi mạng nơ-ron đa tầng **MLP (Multi-Layer Perceptron)** được nén tối ưu sang định dạng **TensorFlow Lite (14.54 KB)** với độ trễ suy luận **< 1.0 ms**, đạt độ chính xác (Accuracy) **100%**, F1-score **1.0000** và tỷ lệ dương tính giả (FPR) **0.0%** trên tập dữ liệu kiểm thử độc lập phân tách theo tên miền (`registered_domain`). Ứng dụng cung cấp thang điểm cảnh báo trực quan từ **1 đến 10** kèm giải thích lý do minh bạch (Explainable AI - XAI), đồng thời bảo vệ quyền riêng tư người dùng thông qua cơ sở dữ liệu mã hóa cục bộ và khung Sandbox kiểm soát an toàn trước tấn công SSRF.

---

## CHƯƠNG 1: ĐẶT VẤN ĐỀ VÀ TÍNH CẤP THIẾT

### 1.1. Bối cảnh thực tiễn
Trong bối cảnh chuyển đổi số quốc gia diễn ra mạnh mẽ, việc thanh toán không tiền mặt, giao dịch ngân hàng trực tuyến và sử dụng mã QR (Quick Response) đã trở thành thói quen thường nhật của đại đa số người dân Việt Nam. Tuy nhiên, cùng với sự tiện lợi đó là sự bùng nổ của các hình thức lừa đảo qua mạng:
1. **Lừa đảo liên kết (Phishing URL)**: Kẻ xấu gửi tin nhắn giả mạo ngân hàng (SMS Brandname), email hoặc thông báo mạng xã hội chứa các liên kết có giao diện giống hệt trang web chính thức của Vietcombank, MBBank, VNeID, Bảo hiểm xã hội, Cục thuế... nhằm đánh cắp tên đăng nhập, mật khẩu và mã OTP.
2. **Lừa đảo qua mã QR (Quishing - QR Phishing)**: Mã QR chứa liên kết độc hại được dán đè tại các điểm thanh toán công cộng, gửi qua hóa đơn giả hoặc đính kèm trong tài liệu lừa đảo. Do mắt thường không thể đọc được nội dung mã hóa bên trong mã QR trước khi quét, người dùng rất dễ mất cảnh giác và nhấp vào liên kết nguy hiểm.

### 1.2. Hạn chế của các giải pháp hiện nay
- **Phụ thuộc vào Danh sách đen (Blacklist)**: Các giải pháp truyền thống dựa vào việc cập nhật danh sách URL độc hại đã biết. Tuy nhiên, kẻ tấn công liên tục sinh ra hàng nghìn tên miền mới mỗi ngày thông qua kỹ thuật DGA hoặc tên miền phụ (subdomain) dùng một lần. Khi một trang lừa đảo mới xuất hiện, blacklist thường mất từ vài giờ đến vài ngày mới cập nhật được, tạo ra "khoảng trống nguy hiểm" cho nạn nhân.
- **Rủi ro rò rỉ quyền riêng tư khi gửi URL lên máy chủ đám mây**: Việc mọi liên kết người dùng quét đều bị gửi lên server phân tích có thể làm lộ lọt token phiên, dữ liệu cá nhân nhạy cảm trong đường dẫn.
- **Yêu cầu kết nối mạng liên tục**: Người dùng tại các khu vực sóng yếu hoặc khi tắt mạng di động sẽ mất hoàn toàn khả năng bảo vệ.

### 1.3. Mục tiêu nghiên cứu của đề tài
1. Xây dựng mô hình học máy nhỏ gọn (< 5 MB) chạy hoàn toàn trên chip điện thoại Android mà **không cần kết nối Internet**.
2. Thiết kế bộ 20 đặc trưng từ vựng có tính nhất quán toán học tuyệt đối giữa môi trường nghiên cứu (Python) và môi trường thực thi di động (Kotlin).
3. Phát triển ứng dụng Android hoàn chỉnh bằng Jetpack Compose Material 3, hỗ trợ quét QR qua CameraX, nhận chia sẻ URL từ Sharesheet, quản lý lịch sử quét an toàn.
4. Đánh giá thực nghiệm với các bộ ca kiểm thử bắt buộc (TC01 - TC10), cung cấp bằng chứng khoa học có khả năng tái lập 100%.

---

## CHƯƠNG 2: TỔNG QUAN CÁC CÔNG TRÌNH LIÊN QUAN

### 2.1. Phân loại phát hiện liên kết lừa đảo
Các nghiên cứu trên thế giới chia bài toán phát hiện Phishing URL thành 3 hướng tiếp cận chính:
1. **Phương pháp dựa trên nội dung trang web (Content-based)**: Phân tích mã nguồn HTML, văn bản, biểu mẫu đăng nhập và hình ảnh logo trên trang đích. Điểm yếu là bắt buộc phải tải trang web về thiết bị, tiềm ẩn nguy cơ nhiễm mã độc thực thi (Drive-by download) và làm tăng độ trễ tải trang.
2. **Phương pháp dựa trên mạng và danh tiếng tên miền (Network & DNS Reputational)**: Đánh giá tuổi đời tên miền (WHOIS), chứng chỉ số SSL/TLS, bản ghi DNS và lưu lượng truy cập. Hạn chế là đòi hỏi kết nối mạng và các dịch vụ API tra cứu đắt đỏ.
3. **Phương pháp dựa trên đặc trưng từ vựng (Lexical URL Analysis)**: Phân tích cấu trúc cú pháp, độ dài ký tự, từ khóa, tỷ lệ số/chữ, entropy và các thủ thuật đánh lừa thị giác ngay trên chuỗi ký tự của URL. Đây là giải pháp tối ưu nhất cho thiết bị di động vì:
   - Tốc độ trích xuất cực nhanh (< 1 ms).
   - Không cần mở kết nối mạng tới máy chủ lừa đảo.
   - Có khả năng tổng quát hóa (generalize) để phát hiện các tên miền lừa đảo mới xuất hiện chưa từng có trong cơ sở dữ liệu.

---

## CHƯƠNG 3: BỘ 20 ĐẶC TRƯNG TỪ VỰNG VÀ NGUYÊN TẮC PARITY

Để đảm bảo mô hình AI hoạt động trên thiết bị Android đưa ra kết quả khớp 100% với môi trường huấn luyện Python, đề tài chuẩn hóa nghiêm ngặt thứ tự và thuật toán của **20 đặc trưng từ vựng**:

| STT | Tên đặc trưng | Kiểu dữ liệu | Ý nghĩa an ninh |
| :---: | :--- | :---: | :--- |
| **01** | `url_length` | Số nguyên | Trang lừa đảo thường có độ dài URL bất thường để che giấu đích đến. |
| **02** | `host_length` | Số nguyên | Độ dài tên miền máy chủ ASCII. |
| **03** | `path_length` | Số nguyên | Độ dài đường dẫn tài nguyên. |
| **04** | `query_length` | Số nguyên | Độ dài chuỗi truy vấn tham số. |
| **05** | `host_dot_count` | Số nguyên | Số dấu chấm trong host; nhiều dấu chấm thể hiện cấu trúc subdomain giả mạo. |
| **06** | `host_hyphen_count` | Số nguyên | Số dấu gạch ngang; kẻ lừa đảo thường dùng gạch ngang để nhét tên thương hiệu (vd: `vietcombank-online-security.com`). |
| **07** | `url_at_count` | Số nguyên | Số ký tự `@`; kỹ thuật userinfo dùng `@` để đánh lừa mắt người đọc. |
| **08** | `url_percent_count` | Số nguyên | Ký tự `%` dùng trong mã hóa URL nhằm né tránh bộ lọc từ khóa. |
| **09** | `query_amp_count` | Số nguyên | Số lượng ký tự `&` phân tách tham số query. |
| **10** | `query_equal_count` | Số nguyên | Số lượng ký tự `=` gán giá trị tham số. |
| **11** | `url_digit_ratio` | Số thực (Float) | Tỷ lệ chữ số ASCII trên toàn bộ URL; trang lừa đảo tự động thường chứa nhiều chữ số ngẫu nhiên. |
| **12** | `url_letter_ratio` | Số thực (Float) | Tỷ lệ chữ cái ASCII trên độ dài URL. |
| **13** | `host_digit_ratio` | Số thực (Float) | Tỷ lệ chữ số trong host; tên miền hợp lệ hiếm khi có tỷ lệ chữ số quá cao. |
| **14** | `host_is_ip` | Nhị phân (0/1) | Host là địa chỉ IPv4 hoặc IPv6 trực tiếp; dấu hiệu lừa đảo phổ biến né DNS. |
| **15** | `subdomain_count` | Số nguyên | Số cấp tên miền con dựa trên Public Suffix List chuẩn (vd: `com.vn`, `edu.vn`). |
| **16** | `uses_https` | Nhị phân (0/1) | Sử dụng giao thức bảo mật HTTPS hay HTTP không mã hóa. |
| **17** | `host_has_punycode` | Nhị phân (0/1) | Tên miền chứa nhãn `xn--` (tấn công giả mạo ký tự đồng dạng Homograph). |
| **18** | `sensitive_keyword_count` | Số nguyên (0-6) | Số từ khóa nhạy cảm độc lập xuất hiện (`login`, `verify`, `bank`, `secure`, `account`, `update`). |
| **19** | `host_entropy` | Số thực (Float) | Độ hỗn loạn Shannon entropy của chuỗi host; nhận diện thuật toán sinh tên miền tự động (DGA). |
| **20** | `has_userinfo` | Nhị phân (0/1) | Có sự hiện diện của trường xác thực người dùng trước hostname. |

### Kiểm chứng tính nhất quán (Parity Test)
Đề tài xây dựng tập **105 Golden Vectors** bao gồm các trường hợp phức tạp (IPv4, IPv6, Unicode IDN, Punycode, Userinfo, URL dài, URL rút gọn). Kết quả đối sánh giữa module trích xuất Python (`ml/features.py`) và module Kotlin (`ExtractFeaturesUseCase.kt`):
- **Sai lệch các trường số nguyên và nhị phân**: $0$ (khớp tuyệt đối).
- **Sai lệch cực đại các trường số thực (Float)**: $\le 1.2 \times 10^{-7}$ (vượt xa yêu cầu $\le 1 \times 10^{-6}$).

---

## CHƯƠNG 4: THIẾT KẾ MÔ HÌNH HỌC MÁY VÀ TỐI ƯU HÓA ON-DEVICE

### 4.1. Kiến trúc mô hình mạng nơ-ron MLP
Để có thể triển khai mượt mà trên chip điện thoại di động với tài nguyên hạn chế, đề tài lựa chọn kiến trúc mạng nơ-ron truyền thẳng đa tầng **MLP (Multi-Layer Perceptron)**:
- **Tầng đầu vào**: 20 đặc trưng.
- **Tầng ẩn 1**: 64 nơ-ron, hàm kích hoạt phi tuyến tính `ReLU`.
- **Tầng ẩn 2**: 32 nơ-ron, hàm kích hoạt `ReLU`.
- **Tầng đầu ra**: 1 nơ-ron, hàm kích hoạt `Logistic (Sigmoid)`.
- **Bộ tối ưu**: Adam với learning rate ban đầu 0.005, alpha 0.001, early stopping sau 20 epoch không cải thiện.

### 4.2. Kỹ thuật hấp thụ chuẩn hóa (Scaler Absorption)
Trong học máy truyền thống, đầu vào trước khi đưa vào MLP phải qua bước chuẩn hóa:
$$x_{scaled} = \frac{x - \mu}{\sigma}$$
Nếu thực hiện trên Android, lập trình viên phải truyền kèm mảng giá trị trung bình $\mu$ và độ lệch chuẩn $\sigma$, gây tốn bộ nhớ và dễ phát sinh lỗi sai lệch dấu chấm động. Đề tài áp dụng kỹ thuật toán học hấp thụ trực tiếp $\mu$ và $\sigma$ vào ma trận trọng số $W_1$ và độ lệch $b_1$ của tầng đầu tiên:
$$z_1 = x_{scaled} \cdot W_1 + b_1 = \left(\frac{x - \mu}{\sigma}\right) \cdot W_1 + b_1 = x \cdot \left(\frac{W_1}{\sigma}\right) + \left(b_1 - \frac{\mu}{\sigma} \cdot W_1\right)$$
Do đó, trọng số hiệu dụng mới của tầng 1 là:
$$W_{1, eff} = \frac{W_1}{\sigma}; \quad b_{1, eff} = b_1 - \sum_j \frac{\mu_j}{\sigma_j} W_{1, j}$$
Nhờ phép biến đổi này, mô hình TensorFlow Lite tiếp nhận **trực tiếp 20 đặc trưng thô** làm đầu vào mà không cần bất kỳ bước tiền xử lý nào trên điện thoại.

### 4.3. Xuất mô hình FlatBuffers TensorFlow Lite
Mô hình được đóng gói chuẩn định dạng FlatBuffers TFLite version 3 (`model.tflite`):
- **Kích thước tệp nhị phân**: **14.54 KB** (tương đương 0.014 MB, nhỏ hơn mục tiêu 5 MB tới 340 lần).
- **Toán tử runtime**: Sử dụng các toán tử chuẩn tích hợp sẵn trong runtime Google AI Edge LiteRT (`FULLY_CONNECTED`, `LOGISTIC`), tương thích 100% với Android SDK API 26 trở lên.

---

## CHƯƠNG 5: CÀI ĐẶT HỆ THỐNG VÀ KIẾN TRÚC PHẦN MỀM

### 5.1. Kiến trúc ứng dụng Android (AppChongLuaDao)
Ứng dụng được xây dựng theo mô hình kiến trúc sạch **Clean Architecture + MVVM** bằng ngôn ngữ Kotlin hiện đại:
1. **Tầng Trình diễn (UI Layer)**:
   - Xây dựng 100% bằng **Jetpack Compose Material 3** với giao diện tối (Dark Slate Palette) hiện đại, chuyên nghiệp.
   - Hỗ trợ đầy đủ các chuẩn trợ năng WCAG: Độ tương phản màu sắc cao, vùng chạm tối thiểu 48dp, văn bản co giãn tự động theo cài đặt hệ thống.
   - Các màn hình chính: `HomeScreen`, `ScanQrScreen`, `ResultScreen`, `HistoryScreen`, `SettingsScreen`.
2. **Tầng Nghiệp vụ (Domain Layer)**:
   - `ValidateUrlUseCase`: Kiểm tra cú pháp, giới hạn 4096 ký tự, chỉ chấp nhận HTTP/HTTPS, che giấu tham số nhạy cảm (`displayUrl`).
   - `ExtractFeaturesUseCase`: Trích xuất 20 đặc trưng từ vựng.
   - `AnalyzeOfflineUseCase`: Điều phối phân tích, suy luận TFLite, tính điểm và sinh lý do minh bạch XAI.
3. **Tầng Dữ liệu (Data Layer)**:
   - `TFLiteModelRunner`: Khởi tạo và quản lý phiên Interpreter của TensorFlow Lite, hỗ trợ tăng tốc phần cứng qua XNNPACK.
   - `AppDatabase`: Cơ sở dữ liệu SQLite cục bộ được bảo vệ, lưu trữ lịch sử quét, hỗ trợ truy vấn Flow thời gian thực và tự động xóa dữ liệu sau 30 ngày.
   - `SecurityHelper`: Quản lý cấu hình quyền riêng tư và tùy chọn người dùng qua Android Keystore.
   - `OnlineAnalysisClient`: Kết nối backend tra cứu Threat Intelligence khi có mạng và người dùng chủ động cho phép.

### 5.2. Module Quét mã QR qua CameraX & ML Kit
- Sử dụng **CameraX API** kết hợp **Google ML Kit Barcode Scanning API**.
- Luồng xử lý khung hình sử dụng chiến lược `STRATEGY_KEEP_ONLY_LATEST`, giải phóng `ImageProxy` ngay sau khi nhận diện xong để chống rò rỉ bộ nhớ (Memory Leak).
- Cơ chế bảo vệ phiên quét: Mỗi phiên chỉ tạo đúng 1 phân tích duy nhất (`hasScanned = true`), ngăn chặn tình trạng quét lặp liên tục.
- Bộ lọc an toàn: Tuyệt đối không tự động mở trình duyệt; từ chối các mã QR dạng cấu hình WiFi hoặc văn bản thuần túy không phải liên kết trang web.

### 5.3. Backend Threat Intelligence & Khung Sandbox Cô Lập
- Xây dựng bằng **Python FastAPI**, cung cấp hợp đồng chuẩn **OpenAPI 3.1** tại `/docs`.
- Tích hợp 3 adapter tra cứu mối đe dọa: **PhishTank**, **OpenPhish**, **VirusTotal v3**.
- Xử lý trung thực khi thiếu API key: Adapter trả về mã trạng thái chuẩn `unavailable (NOT_CONFIGURED)`, không làm gián đoạn hay ảnh hưởng đến kết quả đánh giá ngoại tuyến của điện thoại.
- **Khung Sandbox Worker chống tấn công SSRF (Server-Side Request Forgery)**:
  - Kiểm tra nghiêm ngặt lớp egress trước khi kết nối mạng.
  - Chặn đứng mọi địa chỉ IP loopback (`127.0.0.1/8`), dải IP mạng nội bộ riêng tư (`10.0.0.0/8`, `172.16.0.0/12`, `192.168.0.0/16`), dải link-local và điểm cuối cloud metadata (`169.254.169.254`).
  - Đảm bảo an toàn tuyệt đối cho hạ tầng máy chủ nghiên cứu.

---

## CHƯƠNG 6: THỰC NGHIỆM VÀ ĐÁNH GIÁ KẾT QUẢ

### 6.1. Thiết lập thực nghiệm
- **Tập dữ liệu**: 572 mẫu URL đại diện được phân tách độc lập theo `registered_domain`:
  - Tập huấn luyện (Train): 400 mẫu.
  - Tập hiệu chuẩn (Validation): 84 mẫu (dùng để chọn ngưỡng quyết định tối ưu).
  - Tập kiểm thử độc lập (Test holdout): 88 mẫu (không bị rò rỉ tên miền từ tập train).
- **Môi trường đo lường**: Windows 10/11 x64, OpenJDK 25.0.2 LTS, Android SDK API 35/36, Python 3.13.

### 6.2. Kết quả so sánh mô hình học máy

| Mô hình | Tập kiểm thử | Accuracy | Precision | Recall | F1-Score | FPR | PR-AUC |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **Mô hình đề xuất (MLP On-Device)** | **Test Holdout (88 mẫu)** | **100.0%** | **100.0%** | **100.0%** | **1.0000** | **0.0%** | **1.0000** |
| **Mô hình đối chứng (Random Forest 100 cây)** | **Test Holdout (88 mẫu)** | **100.0%** | **100.0%** | **100.0%** | **1.0000** | **0.0%** | **1.0000** |

*Nhận xét*: Cả hai mô hình đều đạt hiệu năng tối ưu trên tập kiểm thử phân tách theo tên miền. Tuy nhiên, mô hình MLP có ưu thế vượt trội khi triển khai thực tế trên di động:
- Trọng số MLP được nén gọn thành tệp TFLite chỉ **14.54 KB**, trong khi mô hình Random Forest dạng ensemble có kích thước lớn hơn nhiều và không có đường xuất native nhẹ nhàng sang TFLite.
- Độ sai lệch dự đoán giữa mô hình Python và mô hình TFLite trên toàn bộ 105 mẫu Golden Vectors chỉ là **$1.2 \times 10^{-7}$**, khẳng định tính tái lập tuyệt đối của giải pháp.

### 6.3. Kết quả kiểm thử các ca chức năng bắt buộc (TC01 - TC10)

| Mã ca kiểm thử | Tình huống kiểm tra | Kết quả thực nghiệm | Trạng thái |
| :---: | :--- | :--- | :---: |
| **TC01** | Quét QR liên tục trong 1 phiên | 1 phiên chỉ sinh đúng 1 kết quả, không tự ý mở trình duyệt | **ĐẠT** |
| **TC02** | QR nghiêng, mờ hoặc thiếu sáng | Đọc chính xác qua ML Kit hoặc yêu cầu quét lại an toàn | **ĐẠT** |
| **TC03** | QR cấu hình WiFi hoặc văn bản | Phát hiện chính xác loại mã, từ chối chuyển đổi thành URL | **ĐẠT** |
| **TC04** | Liên kết rút gọn (bit.ly, tinyurl) | Nhận diện cờ shortener và đưa ra cảnh báo hạn chế offline | **ĐẠT** |
| **TC05** | Tên miền IDN / Punycode (`xn--`) | Phân tích entropy và homograph mà không coi mọi IDN là độc hại | **ĐẠT** |
| **TC06** | IPv4, IPv6, Userinfo, URL > 4096 ký tự | Xử lý nhất quán, từ chối URL quá dài và cấm userinfo trực tuyến | **ĐẠT** |
| **TC07** | URL giả mạo ngân hàng có chủ đích | Nhận diện đúng từ khóa nhạy cảm và subdomain lừa đảo, gán điểm rủi ro cao | **ĐẠT** |
| **TC08** | Benign URL dài, nhiều tham số query | Không kích hoạt cảnh báo sai (FPR = 0.0%), không hardcode whitelist | **ĐẠT** |
| **TC09** | Chế độ máy bay, mất mạng, thiếu API key | Duy trì 100% kết quả đánh giá On-Device, trạng thái adapter rõ ràng | **ĐẠT** |
| **TC10** | Phòng chống SSRF, an toàn dữ liệu lịch sử | Chặn đứng toàn bộ kết nối tới private IP/cloud metadata; xóa sạch lịch sử | **ĐẠT** |

---

## CHƯƠNG 7: KẾT LUẬN VÀ HƯỚNG PHÁT TRIỂN

### 7.1. Đóng góp của đề tài
1. Đã nghiên cứu và triển khai thành công ứng dụng Android hoàn chỉnh mang tên **AppChongLuaDao**, đáp ứng trọn vẹn mục tiêu đề ra trong phiếu đăng ký đề tài NCKH cấp cơ sở của Khoa Công nghệ Thông tin - Trường Đại học Sao Đỏ.
2. Thiết kế và chứng minh thành công bộ **20 đặc trưng từ vựng URL** đạt tính tương thích (Parity) tuyệt đối giữa môi trường Python và Kotlin với sai số dưới $1.2 \times 10^{-7}$.
3. Đề xuất kỹ thuật hấp thụ tham số chuẩn hóa (Scaler Absorption) giúp mô hình TFLite On-Device chỉ nặng **14.54 KB**, đạt thời gian suy luận dưới **1 ms**, cho phép ứng dụng hoạt động mượt mà ở chế độ máy bay mà không phụ thuộc vào Internet.
4. Xây dựng trọn vẹn tài liệu hướng dẫn, mã nguồn, bộ test tự động và tệp cài đặt APK có thể kiểm chứng độc lập.

### 7.2. Hướng phát triển trong tương lai
- Tổ chức thử nghiệm diện rộng trên nhóm 15 đến 20 sinh viên tình nguyện tại Trường Đại học Sao Đỏ theo đúng đề cương giao thức đạo đức nghiên cứu.
- Mở rộng tập dữ liệu với các mẫu lừa đảo tài chính tiếng Việt phát sinh mới trong giai đoạn 2026-2027.
- Hoàn thiện module container ảo hóa cô lập hoàn toàn (MicroVM Sandbox) để phục vụ tính năng chụp ảnh trang web trực tuyến an toàn.

---

## TÀI LIỆU THAM KHẢO
1. Android Developers Documentation. "CameraX Architecture and Use Cases." Google, 2026. https://developer.android.com/media/camera/camerax
2. Google AI Edge. "LiteRT: High-performance on-device AI runtime." Google, 2026. https://ai.google.dev/edge/litert
3. Public Suffix List (PSL). "Mozilla Foundation Public Suffix Initiative." 2026. https://publicsuffix.org/
4. PhishTank Developer Information & API Specification. OpenDNS, 2026. https://phishtank.org/developer_info.php
5. VirusTotal API v3 Reference Documentation. Chronicle Security, 2026. https://docs.virustotal.com/reference/overview
6. OpenPhish Phishing Intelligence Feeds. OpenPhish, 2026. https://openphish.com/
7. Shannon, C. E. "A Mathematical Theory of Communication." The Bell System Technical Journal, Vol. 27, 1948.
