# DATA CARD: Phishing & Benign URL Dataset

## 1. Thông tin tập dữ liệu
- **Tên tập dữ liệu**: `AppChongLuaDao-URL-Dataset-v1`
- **Phiên bản**: `1.0.0`
- **Ngày thu thập / đóng gói**: 19/09/2026
- **Quản trị dữ liệu**: Nhóm NCKH Sinh viên Khoa Công nghệ Thông tin - Trường Đại học Sao Đỏ.
- **Mục đích**: Huấn luyện và kiểm thử mô hình học máy phát hiện URL lừa đảo trên thiết bị di động.

---

## 2. Nguồn dữ liệu (Data Sourcing)
1. **Mẫu lừa đảo (Phishing URLs)**:
   - Dữ liệu mô phỏng và mẫu thu thập định dạng tương thích PhishTank, OpenPhish và URLhaus.
   - Các hình thức lừa đảo phổ biến:
     - Giả mạo ngân hàng Việt Nam (Vietcombank, MBBank, Techcombank, BIDV, ACB, VPBank, TPBank...)
     - Lừa đảo thông báo trúng thưởng, tặng quà tri ân (ZaloPay, MoMo, Shopee...)
     - Mạo danh cơ quan nhà nước và dịch vụ công (VNeID, Bảo hiểm xã hội, Cục thuế...)
     - Typosquatting, Punycode/IDN homograph (`xn--...`)
     - DGA (Domain Generation Algorithm) ký tự ngẫu nhiên có độ hỗn loạn (Entropy) cao
     - Sử dụng trực tiếp địa chỉ IPv4 / IPv6 thô kèm port
     - Kỹ thuật che giấu đích đến qua Userinfo (`user@real-domain.com`)
2. **Mẫu lành tính (Benign URLs)**:
   - Các trang web phổ biến hàng đầu thế giới và Việt Nam (Alexa/Tranco top domains).
   - Trang tin tức, giáo dục (.edu.vn, saodo.edu.vn, hust.edu.vn, vnu.edu.vn).
   - Trang thương mại điện tử với đường dẫn sâu, nhiều tham số query và percent encoding.
   - Cổng thông tin chính phủ (.gov.vn).

---

## 3. Chiến lược phân tách dữ liệu (Split Strategy)
- **Phương pháp phân chia**: Phân tách nghiêm ngặt theo **`registered_domain`** (Tên miền đã đăng ký).
  - *Lý do*: Nếu phân chia ngẫu nhiên từng URL, các URL của cùng một tên miền sẽ lọt vào cả tập Train và tập Test (Data Leakage), dẫn đến độ chính xác bị thổi phồng.
  - Phân tách theo `registered_domain` đảm bảo tập Test chứa các tên miền hoàn toàn mới mà mô hình chưa từng nhìn thấy lúc huấn luyện.
- **Tỷ lệ phân chia**:
  - **Tập Huấn luyện (Train)**: 400 mẫu (~70%)
  - **Tập Hiệu chuẩn (Validation)**: 84 mẫu (~15%)
  - **Tập Kiểm thử độc lập (Test)**: 88 mẫu (~15%)
- **Tổng số mẫu**: 572 mẫu (Benign: 272, Phishing: 300).
- **Tập Golden Vectors đối sánh Parity**: 105 mẫu đa dạng kiểm tra sự nhất quán giữa Python và Kotlin.

---

## 4. Quyền riêng tư và Bảo vệ dữ liệu
- Không chứa bất kỳ thông tin nhận dạng cá nhân (PII), mật khẩu thật hay token phiên đăng nhập thật của người dùng.
- Toàn bộ tham số nhạy cảm trong URL đều được chuẩn hóa và loại bỏ.
- Lưu trữ manifest tại `artifacts/data/manifest.json`.
