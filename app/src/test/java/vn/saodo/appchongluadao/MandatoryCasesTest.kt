package vn.saodo.appchongluadao

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import vn.saodo.appchongluadao.domain.model.RiskLevel
import vn.saodo.appchongluadao.domain.usecase.ExtractFeaturesUseCase
import vn.saodo.appchongluadao.domain.usecase.ValidateUrlUseCase

class MandatoryCasesTest {

    private lateinit var validateUrlUseCase: ValidateUrlUseCase
    private lateinit var extractFeaturesUseCase: ExtractFeaturesUseCase

    @Before
    fun setUp() {
        validateUrlUseCase = ValidateUrlUseCase()
        extractFeaturesUseCase = ExtractFeaturesUseCase()
    }

    @Test
    fun testTC03_QrNonUrlAndWifiRejected() {
        // TC03: QR dạng WiFi, văn bản không được tự chuyển thành URL
        val wifiPayload = "WIFI:S:MyHomeWiFi;T:WPA;P:SecretPassword123;;"
        val resultWifi = validateUrlUseCase.execute(wifiPayload)
        assertTrue(resultWifi is ValidateUrlUseCase.ValidationResult.Invalid)

        val plainText = "Chao ban day la tin nhan van ban khong phai lien ket"
        val resultText = validateUrlUseCase.execute(plainText)
        assertTrue(resultText is ValidateUrlUseCase.ValidationResult.Invalid)
    }

    @Test
    fun testTC04_ShortenerDetection() {
        // TC04: Link rút gọn (bit.ly, tinyurl...)
        val shortUrl = "https://bit.ly/3xY7Z9q"
        val validation = validateUrlUseCase.execute(shortUrl)
        assertTrue(validation is ValidateUrlUseCase.ValidationResult.Valid)
        val valid = validation as ValidateUrlUseCase.ValidationResult.Valid
        assertEquals("bit.ly", valid.host)
    }

    @Test
    fun testTC05_PunycodeAndInternationalDomain() {
        // TC05: IDN/Punycode không coi mọi IDN là độc hại
        val punycodeUrl = "https://xn--b-5m5a.vn/tin-tuc"
        val validation = validateUrlUseCase.execute(punycodeUrl)
        assertTrue(validation is ValidateUrlUseCase.ValidationResult.Valid)
        val valid = validation as ValidateUrlUseCase.ValidationResult.Valid
        val features = extractFeaturesUseCase.execute(valid.normalizedUrl)
        assertNotNull(features)
        assertEquals(1, features!!.hostHasPunycode)
    }

    @Test
    fun testTC06_IpIpv6UserinfoAndUrlTooLong() {
        // TC06: IPv4, IPv6, userinfo, scheme sai, URL > 4096
        val ipv4Url = "http://192.168.1.100/login.php"
        val valIpv4 = validateUrlUseCase.execute(ipv4Url)
        assertTrue(valIpv4 is ValidateUrlUseCase.ValidationResult.Valid)
        val fIpv4 = extractFeaturesUseCase.execute((valIpv4 as ValidateUrlUseCase.ValidationResult.Valid).normalizedUrl)
        assertNotNull(fIpv4)
        assertEquals(1, fIpv4!!.hostIsIp)

        val ipv6Url = "http://[::1]/admin"
        val valIpv6 = validateUrlUseCase.execute(ipv6Url)
        assertTrue(valIpv6 is ValidateUrlUseCase.ValidationResult.Valid)
        val fIpv6 = extractFeaturesUseCase.execute((valIpv6 as ValidateUrlUseCase.ValidationResult.Valid).normalizedUrl)
        assertNotNull(fIpv6)
        assertEquals(1, fIpv6!!.hostIsIp)

        val userinfoUrl = "http://user:pass@victim-bank.com/portal"
        val valUserinfo = validateUrlUseCase.execute(userinfoUrl)
        assertTrue(valUserinfo is ValidateUrlUseCase.ValidationResult.Valid)
        val fUserinfo = extractFeaturesUseCase.execute((valUserinfo as ValidateUrlUseCase.ValidationResult.Valid).normalizedUrl)
        assertNotNull(fUserinfo)
        assertEquals(1, fUserinfo!!.hasUserinfo)

        // Scheme sai (ftp, javascript, mailto)
        val ftpUrl = "ftp://files.example.com/data.zip"
        val valFtp = validateUrlUseCase.execute(ftpUrl)
        assertTrue(valFtp is ValidateUrlUseCase.ValidationResult.Invalid)

        // URL quá dài (> 4096 code points)
        val longUrl = "https://example.com/path?" + "a=1&".repeat(1200)
        assertTrue(longUrl.length > 4096)
        val valLong = validateUrlUseCase.execute(longUrl)
        assertTrue(valLong is ValidateUrlUseCase.ValidationResult.Invalid)
    }

    @Test
    fun testTC07_PhishingSignalsExtraction() {
        // TC07: URL Phishing chứa từ khóa nhạy cảm và nhiều subdomain
        val phishingUrl = "http://vietcombank.com.vn.ebanking.verify-account.security-portal.xyz/login"
        val validation = validateUrlUseCase.execute(phishingUrl)
        assertTrue(validation is ValidateUrlUseCase.ValidationResult.Valid)
        val f = extractFeaturesUseCase.execute((validation as ValidateUrlUseCase.ValidationResult.Valid).normalizedUrl)
        assertNotNull(f)
        assertTrue(f!!.sensitiveKeywordCount >= 2) // login, verify, security
        assertTrue(f.subdomainCount >= 3)
    }

    @Test
    fun testTC08_BenignUrlWithManyQueryParameters() {
        // TC08: Benign URL dài, nhiều query (Google Maps, Shopee)
        val benignUrl = "https://www.google.com/search?q=antigravity+ai&hl=vi&source=hp&biw=1920&bih=1080"
        val validation = validateUrlUseCase.execute(benignUrl)
        assertTrue(validation is ValidateUrlUseCase.ValidationResult.Valid)
        val f = extractFeaturesUseCase.execute((validation as ValidateUrlUseCase.ValidationResult.Valid).normalizedUrl)
        assertNotNull(f)
        assertEquals(0, f!!.hostIsIp)
        assertEquals(0, f.hasUserinfo)
        assertEquals(1, f.usesHttps)
    }

    @Test
    fun testRiskScoreFormula() {
        // Công thức chuẩn: clamp(1 + floor(9p + 0.5), 1, 10)
        fun calculateScore(p: Float): Int {
            return (1 + Math.floor(9.0 * p + 0.5)).toInt().coerceIn(1, 10)
        }

        assertEquals(1, calculateScore(0.0f))
        assertEquals(1, calculateScore(0.05f))
        assertEquals(2, calculateScore(0.12f))
        assertEquals(6, calculateScore(0.50f))
        assertEquals(9, calculateScore(0.90f))
        assertEquals(10, calculateScore(0.95f))
        assertEquals(10, calculateScore(1.0f))

        assertEquals(RiskLevel.LOW, RiskLevel.fromScore(2))
        assertEquals(RiskLevel.MEDIUM, RiskLevel.fromScore(5))
        assertEquals(RiskLevel.HIGH, RiskLevel.fromScore(9))
    }
}
