"""
Script sinh contracts/golden_vectors.json với hơn 100 URL đa dạng và vectors đặc trưng tương ứng.
"""

import json
import os
from ml.features import extract_features, features_to_vector, normalize_and_parse

URL_CANDIDATES = [
    # 1-15: Standard Benign Websites (Global & Vietnam)
    "https://google.com",
    "https://www.google.com/search?q=antigravity+ai&hl=vi",
    "https://saodo.edu.vn",
    "https://fit.saodo.edu.vn/gioi-thieu-khoa-cntt",
    "https://vnexpress.net/thoi-su",
    "https://dantri.com.vn/giao-duc/sinh-vien-nckh-20260919.htm",
    "https://github.com/torvalds/linux",
    "https://en.wikipedia.org/wiki/Phishing",
    "https://youtube.com/watch?v=dQw4w9WgXcQ",
    "https://stackoverflow.com/questions/tagged/kotlin",
    "https://developer.android.com/jetpack/compose",
    "https://vov.vn/tin-tuc-24h",
    "https://tuoitre.vn/cong-nghe.htm",
    "https://chinhphu.vn",
    "https://moet.gov.vn",

    # 16-30: Benign with complex paths, multiple query params, hyphens
    "https://aws.amazon.com/ec2/pricing/on-demand/?nc1=h_ls&loc=1&cat=compute",
    "https://www.microsoft.com/en-us/software-download/windows11?sortby=date&order=desc",
    "https://store.steampowered.com/app/1091500/Cyberpunk_2077/?snr=1_7_7_230_150_1",
    "https://maps.google.com/maps?q=Hanoi+Vietnam&z=14&t=m",
    "https://shopee.vn/search?keyword=dien%20thoai%20android&page=1",
    "https://tiki.vn/laptop-gaming-chinh-hang/c8095?src=mega-menu&brand=asus",
    "https://lazada.vn/catalog/?q=ban+phim+co&_keyori=ss&from=input",
    "https://medium.com/@user/how-to-build-android-apps-in-2026-a-complete-guide-89712ab",
    "https://huggingface.co/models?pipeline_tag=text-classification&sort=trending",
    "https://pypi.org/project/ai-edge-litert/#description",
    "https://hub.docker.com/_/python/tags?page=1&name=3.13",
    "https://reddit.com/r/AndroidDev/comments/xyz123/compose_material_3_best_practices/?depth=2",
    "https://news.ycombinator.com/item?id=12345678",
    "https://gitlab.com/gitlab-org/gitlab/-/issues/12345",
    "https://coursera.org/learn/machine-learning?specialization=deep-learning-ai",

    # 31-45: Phishing with Raw IP Hosts
    "http://192.168.1.100/login.html",
    "http://203.0.113.45/bank/verify-account.php?id=8831",
    "https://198.51.100.12:8443/secure/update-credentials.asp",
    "http://192.0.2.1/webmail/login?user=admin&token=992813",
    "http://103.20.10.5/vietcombank-login-online/auth.htm",
    "http://14.225.10.99/mb-bank-xac-thuc-smart-otp/update.php",
    "http://172.16.0.4/secure/account/verification",
    "https://10.0.0.1:8080/admin/login?redirect=dashboard",
    "http://45.33.32.156/paypal/signin/index.php",
    "http://185.199.110.153/apple-id/verify-security",
    "http://[::1]/test/path",
    "http://[2001:db8::1]/secure/login.php",
    "https://[2001:0db8:85a3:0000:0000:8a2e:0370:7334]:8080/bank/verify",
    "http://[fe80::1ff:fe23:4567:890a]/update",
    "http://[::ffff:192.0.2.128]/account/login",

    # 46-60: Phishing with Userinfo Trick (@ sign)
    "http://www.vietcombank.com.vn@evil-scam-server.xyz/login.php",
    "https://online.mbbank.com.vn@103.155.10.22/secure/verify",
    "http://paypal.com@verify-identity-billing-account.com/index.html",
    "https://accounts.google.com@auth-recovery-session-service.net/login",
    "http://support.apple.com@icloud-account-locked-alert.org/unlock.php",
    "http://bidv.com.vn@phishing-gateway.cc/auth",
    "https://techcombank.com.vn@otp-verification-center.info/update",
    "http://netflix.com@billing-payment-declined-alert.com/reactivate",
    "https://facebook.com@security-checkpoint-notification.com/confirm",
    "http://microsoft.com@office365-password-expiration-warning.top/renew",
    "http://user:password@scam-site.org/portal",
    "https://admin:token123@192.168.1.1/dashboard",
    "http://support:urgent@banking-alert.club/action",
    "https://security:verify@account-lock.online/login",
    "http://victim%40gmail.com:123456@phishing-target.biz/collect",

    # 61-75: Phishing with Multiple Subdomains & Brand Impersonation
    "http://vietcombank.com.vn.ebanking.verify-account.security-portal.xyz/login",
    "https://mbbank.com.vn.online.smart-otp.auth-service.top/index.php",
    "http://techcombank.com.vn.identity-confirmation.portal-update.club/secure",
    "http://login.microsoftonline.com.oauth2.auth.sso-gateway.info/signin",
    "https://secure.paypal.com.us.webapps.mpp.account-recovery.click/webscr",
    "http://appleid.apple.com.manage.account-locked.verify-service.live/sign-in",
    "http://chongluadao.vn.security-report.check-phishing-tool.tk/scan",
    "https://vneid.gov.vn.dinh-danh-dien-tu.kich-hoat-tai-khoan.cc/app.apk",
    "http://baohiemxahoi.gov.vn.tra-cuu-tro-cap-that-nghiep.top/nhan-tien",
    "http://cuc-thue.gov.vn.hoan-thue-thu-nhap.xac-minh.info/refund",
    "http://zalopay.vn.tang-qua-tri-an.nhan-tien-mat.fun/qua",
    "https://mo-mo.vn.lien-ket-ngan-hang.nhan-voucher.online/claim",
    "http://vpbank.com.vn.vay-von-nhanh.giai-ngan-trong-ngay.site/dang-ky",
    "http://tpbank.com.vn.livebank.xac-thuc-thong-tin.biz/cap-nhat",
    "https://acb.com.vn.online-banking.bao-mat-hai-lop.club/login",

    # 76-85: High Entropy DGA & Obfuscated URLs
    "http://xk91jf02ms98zq11.biz/gate.php?id=92147",
    "https://a8f7c9b2e1d0f4a3.cc/update/service",
    "http://z9y8x7w6v5u4t3s2.xyz/index.html?token=a8b7c6",
    "http://q1w2e3r4t5y6u7i8o9p0.top/click?ref=sms_scam",
    "https://998877665544332211.info/bank/auth",
    "http://ab01cd23ef45gh67.club/login.php",
    "https://m1n2b3v4c5x6z7.online/verify",
    "http://p0o9i8u7y6t5r4e3.site/security",
    "https://llkkjjhhggffeedd.net/account",
    "http://1a2b3c4d5e6f7g8h.org/update",

    # 86-95: Punycode / IDN / Unicode Homograph
    "https://xn--b-5m5a.vn",
    "http://xn--apple-43a.com/login",
    "https://xn--googl-r4a.com/search",
    "http://xn--vcb-online-x7a.vn/verify",
    "https://xn--mbbank-online-v3a.com/auth",
    "http://xn--techcombnh-y3a.vn/account",
    "https://xn--chng-la-o-8ya81a7g.vn",
    "http://xn--ngn-hng-x-k7a41a.com/bank",
    "https://xn--facebook-login-k6a.biz/verify",
    "http://xn--bảo-mật-ngân-hàng.com/update",

    # 96-105: Percent Encoding, Strange Characters, Ports
    "http://example.com/%20%20%20/login.html",
    "https://scam.org/path%2Fwith%2Fescaped%2Fslashes?param=%3Cscript%3E",
    "http://bank.com:8080/online/banking/login",
    "https://secure-portal.net:8443/verify?user=test%40gmail.com&action=confirm",
    "http://test.com/path?a=1&b=2&c=3&d=4&e=5&f=6&g=7&h=8&i=9&j=10",
    "https://short.ly/xyz889?utm_source=sms&utm_medium=scam",
    "http://tinyurl.com/bank-urgent-alert",
    "https://bit.ly/3xY7Z9q",
    "http://t.co/fakeLoginLink",
    "https://is.gd/verifyMyAccountNow",
]


def generate_golden_dataset():
    vectors = []
    print(f"Bắt đầu trích xuất đặc trưng cho {len(URL_CANDIDATES)} URL mẫu...")

    for i, raw_url in enumerate(URL_CANDIDATES):
        feat = extract_features(raw_url)
        if feat is None:
            print(f"Cảnh báo: URL thứ {i+1} không thể trích xuất: {raw_url}")
            continue

        vec = features_to_vector(feat)
        norm_info = normalize_and_parse(raw_url)
        normalized_url = norm_info["normalized_url"] if norm_info else raw_url

        vectors.append({
            "id": i + 1,
            "raw_url": raw_url,
            "normalized_url": normalized_url,
            "features": feat,
            "vector": vec,
        })

    output_path = os.path.join("contracts", "golden_vectors.json")
    os.makedirs("contracts", exist_ok=True)
    with open(output_path, "w", encoding="utf-8") as f:
        json.dump({
            "version": "1.0.0",
            "count": len(vectors),
            "description": "Tập 105 Golden Vectors dùng để kiểm thử tính nhất quán (Parity) giữa Python và Android Kotlin",
            "items": vectors
        }, f, indent=2, ensure_ascii=False)

    print(f"Đã tạo thành công {output_path} với {len(vectors)} mẫu!")


if __name__ == "__main__":
    generate_golden_dataset()
