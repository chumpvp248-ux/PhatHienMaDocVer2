package vn.saodo.appchongluadao

import org.junit.Assert.*
import org.junit.Test
import vn.saodo.appchongluadao.domain.model.*
import vn.saodo.appchongluadao.domain.usecase.DecisionFusionUseCase
import java.text.SimpleDateFormat
import java.util.*

class DecisionFusionTest {

    private val fusionUseCase = DecisionFusionUseCase()

    private fun createBaseResult(score: Int, host: String = "example.com"): UrlAnalysisResult {
        val riskLevel = when {
            score >= 7 -> RiskLevel.HIGH
            score >= 4 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }
        return UrlAnalysisResult(
            scanId = UUID.randomUUID().toString(),
            rawUrl = "https://$host",
            displayUrl = "https://$host",
            host = host,
            score = score,
            probability = score / 10.0f,
            riskLevel = riskLevel,
            reasons = listOf(ScanReason("BASE", "Base Title", "Base Message", "info", "model"))
        )
    }

    @Test
    fun testFusionWithMaliciousThirdParty_locksScoreToCritical() {
        val base = createBaseResult(score = 2) // Ban đầu AI đánh giá an toàn
        val enrichment = FullEnrichmentResult(
            host = "phishing-bank.xyz",
            whois = DomainWhoisInfo(ipAddress = "1.2.3.4"),
            chongLuaDaoRiskTitle = "Có thể nguy hiểm",
            chongLuaDaoRiskLevel = RiskLevel.HIGH,
            thirdParties = listOf(
                ThirdPartyReputation("Phish Tank", "Độc hại", ThreatStatusType.MALICIOUS),
                ThirdPartyReputation("Scam Adviser", "Không tìm thấy", ThreatStatusType.NOT_FOUND)
            ),
            updatedAtFormatted = "lúc 10:00"
        )

        val fused = fusionUseCase.fuse(base, enrichment)

        assertTrue("Điểm số sau khi tổng hợp phải từ 9 trở lên khi có đối tác xác nhận độc hại", fused.score >= 9)
        assertEquals(RiskLevel.HIGH, fused.riskLevel)
        assertTrue(fused.isFused)
        assertTrue(fused.reasons.any { it.code == "FUSION_THIRD_PARTY_MALICIOUS" })
    }

    @Test
    fun testFusionWithBrandNewDomain_triggersDomainAgeCritical() {
        val base = createBaseResult(score = 4) // Ban đầu mức trung bình
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(System.currentTimeMillis() - 2 * 24 * 3600 * 1000L)) // 2 ngày trước
        val enrichment = FullEnrichmentResult(
            host = "new-promo-bank.com",
            whois = DomainWhoisInfo(
                ipAddress = "104.21.1.1",
                registrationDate = today
            ),
            chongLuaDaoRiskTitle = "Cần thận trọng",
            chongLuaDaoRiskLevel = RiskLevel.MEDIUM,
            thirdParties = emptyList(),
            updatedAtFormatted = "lúc 10:00"
        )

        val fused = fusionUseCase.fuse(base, enrichment)

        assertTrue("Tên miền mới 2 ngày tuổi phải nâng điểm rủi ro >= 8", fused.score >= 8)
        assertEquals(RiskLevel.HIGH, fused.riskLevel)
        assertTrue(fused.reasons.any { it.code == "FUSION_DOMAIN_AGE_CRITICAL" })
    }

    @Test
    fun testFusionWithMatureDomain_reinforcesSafety() {
        val base = createBaseResult(score = 3)
        val enrichment = FullEnrichmentResult(
            host = "google.com",
            whois = DomainWhoisInfo(
                ipAddress = "142.250.190.46",
                registrationDate = "1997-09-15" // Hơn 25 năm tuổi
            ),
            chongLuaDaoRiskTitle = "An toàn",
            chongLuaDaoRiskLevel = RiskLevel.LOW,
            thirdParties = listOf(
                ThirdPartyReputation("Phish Tank", "Không tìm thấy", ThreatStatusType.NOT_FOUND),
                ThirdPartyReputation("Scam Adviser", "Không tìm thấy", ThreatStatusType.NOT_FOUND)
            ),
            updatedAtFormatted = "lúc 10:00"
        )

        val fused = fusionUseCase.fuse(base, enrichment)

        assertTrue("Tên miền lâu năm không dính danh sách đen được củng cố điểm rủi ro <= 2", fused.score <= 2)
        assertEquals(RiskLevel.LOW, fused.riskLevel)
        assertTrue(fused.reasons.any { it.code == "FUSION_DOMAIN_MATURE" })
    }
}
