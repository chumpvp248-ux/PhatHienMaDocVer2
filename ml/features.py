"""
Feature Extractor cho 20 đặc trưng từ vựng URL.
Tuân thủ nghiêm ngặt contracts/feature_schema.json.
Đảm bảo Parity tuyệt đối với bộ trích xuất Kotlin trên Android.
"""

import math
import ipaddress
import urllib.parse
from typing import Dict, Any, List, Optional, Tuple

# Danh sách multi-part public suffixes phổ biến đã khóa phiên bản
TWO_PART_PUBLIC_SUFFIXES = {
    "com.vn", "edu.vn", "gov.vn", "net.vn", "org.vn", "int.vn", "ac.vn",
    "co.uk", "org.uk", "me.uk", "ltd.uk", "plc.uk", "net.uk", "sch.uk", "ac.uk", "gov.uk",
    "com.au", "net.au", "org.au", "edu.au", "gov.au",
    "co.jp", "ne.jp", "or.jp", "ac.jp", "ed.jp", "go.jp",
    "com.br", "net.br", "org.br", "gov.br",
    "co.nz", "net.nz", "org.nz", "govt.nz",
    "com.sg", "edu.sg", "gov.sg", "net.sg", "org.sg",
    "co.in", "net.in", "org.in", "gen.in", "firm.in", "ind.in",
    "com.tw", "org.tw", "net.tw", "edu.tw", "gov.tw",
    "com.hk", "edu.hk", "gov.hk", "idv.hk", "net.hk", "org.hk",
    "co.kr", "ne.kr", "or.kr", "re.kr", "pe.kr", "go.kr",
    "co.za", "net.za", "org.za", "web.za",
    "com.mx", "net.mx", "org.mx", "edu.mx", "gob.mx",
    "com.my", "net.my", "org.my", "gov.my", "edu.my",
    "com.ph", "net.ph", "org.ph", "gov.ph", "edu.ph",
    "com.tr", "net.tr", "org.tr", "gov.tr", "edu.tr",
}

SENSITIVE_KEYWORDS = ["login", "verify", "bank", "secure", "account", "update"]

FEATURE_NAMES = [
    "url_length",
    "host_length",
    "path_length",
    "query_length",
    "host_dot_count",
    "host_hyphen_count",
    "url_at_count",
    "url_percent_count",
    "query_amp_count",
    "query_equal_count",
    "url_digit_ratio",
    "url_letter_ratio",
    "host_digit_ratio",
    "host_is_ip",
    "subdomain_count",
    "uses_https",
    "host_has_punycode",
    "sensitive_keyword_count",
    "host_entropy",
    "has_userinfo",
]


def is_valid_ip(host: str) -> bool:
    """Kiểm tra host có phải là IPv4 hoặc IPv6 hợp lệ hay không."""
    clean_host = host.strip("[]")
    try:
        ipaddress.ip_address(clean_host)
        return True
    except ValueError:
        return False


def calculate_shannon_entropy(s: str) -> float:
    """Tính toán entropy Shannon cơ số 2 của chuỗi ký tự ASCII."""
    if not s:
        return 0.0
    length = len(s)
    freq: Dict[str, int] = {}
    for c in s:
        freq[c] = freq.get(c, 0) + 1
    entropy = 0.0
    for count in freq.values():
        p = count / length
        entropy -= p * math.log2(p)
    return round(entropy, 6)


def calculate_subdomains(host: str) -> int:
    """
    Tính số nhãn subdomain trước registered_domain theo Public Suffix List chuẩn.
    Nếu host là IP hoặc không đủ nhãn thì trả về 0.
    """
    if not host or is_valid_ip(host):
        return 0
    labels = host.strip(".").split(".")
    n = len(labels)
    if n <= 1:
        return 0

    # Kiểm tra 2 nhãn cuối có phải là two-part suffix không
    if n >= 2:
        last_two = f"{labels[-2]}.{labels[-1]}".lower()
        if last_two in TWO_PART_PUBLIC_SUFFIXES:
            # Suffix chiếm 2 nhãn, domain chiếm 1 nhãn nữa -> registered domain cần 3 nhãn
            # Số subdomain là số nhãn trước 3 nhãn này
            return max(0, n - 3)

    # Ngược lại suffix chiếm 1 nhãn (TLD), registered domain chiếm 2 nhãn cuối
    return max(0, n - 2)


def normalize_and_parse(raw_url: str) -> Optional[Dict[str, Any]]:
    """
    Chuẩn hóa và parse URL theo quy tắc nghiêm ngặt:
    - Loại bỏ khoảng trắng đầu/cuối
    - Chiều dài <= 4096 code points
    - Scheme chỉ chấp nhận http hoặc https
    - Chuyển scheme và host sang lowercase
    - Xử lý Punycode/IDN
    - Tách biệt userinfo, host, path, query
    """
    if not raw_url:
        return None
    url = raw_url.strip()
    if len(url) > 4096:
        return None

    # Phải có scheme
    parsed = urllib.parse.urlsplit(url)
    scheme = parsed.scheme.lower()
    if scheme not in ("http", "https"):
        return None

    netloc = parsed.netloc
    if not netloc:
        return None

    has_userinfo = 1 if "@" in netloc else 0
    userinfo = ""
    host_port = netloc
    if "@" in netloc:
        userinfo, host_port = netloc.split("@", 1)

    # Tách port nếu có (chú ý IPv6 [::1]:8080)
    if host_port.startswith("["):
        bracket_end = host_port.find("]")
        if bracket_end != -1:
            raw_host = host_port[:bracket_end + 1]
        else:
            raw_host = host_port
    else:
        raw_host = host_port.split(":", 1)[0]

    # Convert IDN host sang punycode ASCII nếu có
    try:
        ascii_host = raw_host.encode("idna").decode("ascii").lower()
    except Exception:
        ascii_host = raw_host.lower()

    # Reconstruct normalized URL string
    normalized_netloc = f"{userinfo}@{ascii_host}" if userinfo else ascii_host
    if ":" in host_port and not host_port.startswith("["):
        port_part = host_port.split(":", 1)[1]
        normalized_netloc = f"{normalized_netloc}:{port_part}"
    elif host_port.startswith("[") and "]:" in host_port:
        port_part = host_port.split("]:", 1)[1]
        normalized_netloc = f"{normalized_netloc}:{port_part}"

    normalized_url = urllib.parse.urlunsplit((
        scheme,
        normalized_netloc,
        parsed.path,
        parsed.query,
        parsed.fragment
    ))

    return {
        "raw_url": raw_url,
        "normalized_url": normalized_url,
        "scheme": scheme,
        "ascii_host": ascii_host,
        "path": parsed.path,
        "query": parsed.query,
        "fragment": parsed.fragment,
        "has_userinfo": has_userinfo,
    }


def extract_features(raw_url: str) -> Optional[Dict[str, Any]]:
    """
    Trích xuất đúng 20 đặc trưng từ raw_url.
    Trả về dict gồm 20 đặc trưng nếu URL hợp lệ, None nếu URL không hợp lệ/vượt ngưỡng.
    """
    parsed_info = normalize_and_parse(raw_url)
    if not parsed_info:
        return None

    norm_url = parsed_info["normalized_url"]
    ascii_host = parsed_info["ascii_host"]
    path = parsed_info["path"]
    query = parsed_info["query"]
    scheme = parsed_info["scheme"]
    has_userinfo = parsed_info["has_userinfo"]

    # 01: url_length
    url_length = len(norm_url)

    # 02: host_length
    host_length = len(ascii_host)

    # 03: path_length
    path_length = len(path)

    # 04: query_length
    query_length = len(query)

    # 05: host_dot_count
    host_dot_count = ascii_host.count(".")

    # 06: host_hyphen_count
    host_hyphen_count = ascii_host.count("-")

    # 07: url_at_count
    url_at_count = norm_url.count("@")

    # 08: url_percent_count
    url_percent_count = norm_url.count("%")

    # 09: query_amp_count
    query_amp_count = query.count("&")

    # 10: query_equal_count
    query_equal_count = query.count("=")

    # 11: url_digit_ratio (mẫu số >= 1)
    digit_count = sum(1 for c in norm_url if c.isdigit())
    url_digit_ratio = round(digit_count / max(1, url_length), 6)

    # 12: url_letter_ratio (mẫu số >= 1)
    letter_count = sum(1 for c in norm_url if c.isalpha())
    url_letter_ratio = round(letter_count / max(1, url_length), 6)

    # 13: host_digit_ratio (mẫu số >= 1)
    host_digit_count = sum(1 for c in ascii_host if c.isdigit())
    host_digit_ratio = round(host_digit_count / max(1, host_length), 6)

    # 14: host_is_ip
    host_is_ip = 1 if is_valid_ip(ascii_host) else 0

    # 15: subdomain_count
    subdomain_count = calculate_subdomains(ascii_host)

    # 16: uses_https
    uses_https = 1 if scheme == "https" else 0

    # 17: host_has_punycode
    host_has_punycode = 1 if any(part.startswith("xn--") for part in ascii_host.split(".")) else 0

    # 18: sensitive_keyword_count (distinct keywords in lowercase URL)
    url_lower = norm_url.lower()
    sensitive_keyword_count = sum(1 for kw in SENSITIVE_KEYWORDS if kw in url_lower)

    # 19: host_entropy
    host_entropy = calculate_shannon_entropy(ascii_host)

    # 20: has_userinfo
    # has_userinfo = has_userinfo (0 hoặc 1)

    return {
        "url_length": url_length,
        "host_length": host_length,
        "path_length": path_length,
        "query_length": query_length,
        "host_dot_count": host_dot_count,
        "host_hyphen_count": host_hyphen_count,
        "url_at_count": url_at_count,
        "url_percent_count": url_percent_count,
        "query_amp_count": query_amp_count,
        "query_equal_count": query_equal_count,
        "url_digit_ratio": url_digit_ratio,
        "url_letter_ratio": url_letter_ratio,
        "host_digit_ratio": host_digit_ratio,
        "host_is_ip": host_is_ip,
        "subdomain_count": subdomain_count,
        "uses_https": uses_https,
        "host_has_punycode": host_has_punycode,
        "sensitive_keyword_count": sensitive_keyword_count,
        "host_entropy": host_entropy,
        "has_userinfo": has_userinfo,
    }


def features_to_vector(feat_dict: Dict[str, Any]) -> List[float]:
    """Chuyển đổi dict đặc trưng thành mảng 20 số float theo đúng feature_order."""
    return [float(feat_dict[name]) for name in FEATURE_NAMES]
