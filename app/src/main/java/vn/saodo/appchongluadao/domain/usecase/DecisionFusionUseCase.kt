package vn.saodo.appchongluadao.domain.usecase

import vn.saodo.appchongluadao.domain.model.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max
import kotlin.math.min

class DecisionFusionUseCase {

    fun fuse(
        baseResult: UrlAnalysisResult,
        enrichment: FullEnrichmentResult
    ): UrlAnalysisResult {
        var fusedScore = baseResult.score
        val additionalReasons = mutableListOf<ScanReason>()
        val summaryPoints = mutableListOf<String>()

        // 1. Phân tích kết quả từ 10 bên thứ ba (Threat Intelligence Consensus)
        val maliciousSources = enrichment.thirdParties.filter { it.statusType == ThreatStatusType.MALICIOUS }
        val suspiciousSources = enrichment.thirdParties.filter { it.statusType == ThreatStatusType.SUSPICIOUS }

        if (maliciousSources.isNotEmpty()) {
            fusedScore = max(fusedScore, 9)
            val names = maliciousSources.joinToString(", ") { it.name }
            additionalReasons.add(
                ScanReason(
                    code = "FUSION_THIRD_PARTY_MALICIOUS",
                    title = "Xác nhận độc hại bởi đối tác an ninh",
                    message = "Liên kết đã bị nhận diện và đưa vào danh sách đen lừa đảo bởi $names.",
                    severity = "critical",
                    source = "threat_intel"
                )
            )
            summaryPoints.add("Xác nhận lừa đảo bởi $names")
        } else if (suspiciousSources.isNotEmpty()) {
            fusedScore = max(fusedScore, min(fusedScore + 2, 8))
            val names = suspiciousSources.joinToString(", ") { it.name }
            additionalReasons.add(
                ScanReason(
                    code = "FUSION_THIRD_PARTY_SUSPICIOUS",
                    title = "Cảnh báo nghi vấn từ mạng lưới bảo mật",
                    message = "Đối tác an ninh ($names) ghi nhận các dấu hiệu bất thường liên quan đến gian lận.",
                    severity = "high",
                    source = "threat_intel"
                )
            )
            summaryPoints.add("Nghi vấn từ $names")
        }

        // 2. Phân tích tuổi đời tên miền (Domain Age từ WHOIS)
        val whois = enrichment.whois
        val regDateStr = whois?.registrationDate
        if (!regDateStr.isNullOrBlank()) {
            val ageDays = calculateAgeDays(regDateStr)
            if (ageDays != null && ageDays >= 0) {
                if (ageDays <= 7) {
                    // Tên miền cực kỳ mới: < 7 ngày
                    fusedScore = max(fusedScore, 8)
                    additionalReasons.add(
                        ScanReason(
                            code = "FUSION_DOMAIN_AGE_CRITICAL",
                            title = "Tên miền vừa tạo (Dưới 7 ngày tuổi)",
                            message = "Tên miền chỉ mới đăng ký $ageDays ngày trước. Hơn 95% trang web lừa đảo trực tuyến sử dụng tên miền ngắn ngày để phát tán mã độc rồi xóa bỏ.",
                            severity = "critical",
                            source = "whois"
                        )
                    )
                    summaryPoints.add("Tên miền mới $ageDays ngày tuổi")
                } else if (ageDays <= 30) {
                    // Tên miền mới: 8 - 30 ngày
                    fusedScore = max(fusedScore, min(fusedScore + 2, 7))
                    additionalReasons.add(
                        ScanReason(
                            code = "FUSION_DOMAIN_AGE_NEW",
                            title = "Tên miền mới hoạt động (Dưới 30 ngày)",
                            message = "Tên miền mới được kích hoạt $ageDays ngày trước, chưa có thời gian tích lũy lịch sử uy tín.",
                            severity = "high",
                            source = "whois"
                        )
                    )
                    summaryPoints.add("Tên miền mới $ageDays ngày")
                } else if (ageDays > 365 * 5 && maliciousSources.isEmpty() && suspiciousSources.isEmpty()) {
                    // Tên miền lâu đời (> 5 năm) không dính danh sách đen
                    val ageYears = ageDays / 365
                    fusedScore = min(fusedScore, 2)
                    additionalReasons.add(
                        ScanReason(
                            code = "FUSION_DOMAIN_MATURE",
                            title = "Tên miền thâm niên có độ tín nhiệm cao",
                            message = "Tên miền đã duy trì hoạt động ổn định hơn $ageYears năm và không ghi nhận vi phạm tại các mạng lưới an ninh mạng quốc tế.",
                            severity = "low",
                            source = "whois"
                        )
                    )
                    summaryPoints.add("Tên miền uy tín $ageYears năm")
                }
            }
        }

        // 3. Phân tích thời hạn đăng ký và đuôi tên miền rủi ro
        val expDateStr = whois?.expirationDate
        if (!expDateStr.isNullOrBlank() && !regDateStr.isNullOrBlank()) {
            val regDays = calculateAgeDays(regDateStr)
            if (regDays != null && regDays in 0..60) {
                val hostLower = baseResult.host.lowercase()
                val isRiskyTld = listOf(".tk", ".ml", ".ga", ".cf", ".gq", ".top", ".xyz", ".club", ".work", ".site")
                    .any { hostLower.endsWith(it) }
                if (isRiskyTld) {
                    fusedScore = max(fusedScore, min(fusedScore + 1, 8))
                    additionalReasons.add(
                        ScanReason(
                            code = "FUSION_DISPOSABLE_DOMAIN",
                            title = "Đuôi tên miền có nguy cơ lừa đảo cao",
                            message = "Trang web sử dụng đuôi tên miền giá rẻ/miễn phí thường xuyên bị tội phạm mạng lạm dụng.",
                            severity = "medium",
                            source = "whois"
                        )
                    )
                }
            }
        }

        // 4. Ghép nối danh sách căn cứ giải thích (XAI)
        val combinedReasons = (baseResult.reasons + additionalReasons).distinctBy { it.code }

        // Chốt điểm chuẩn trong khoảng 1..10
        val finalScore = fusedScore.coerceIn(1, 10)
        val finalRiskLevel = when {
            finalScore >= 7 -> RiskLevel.HIGH
            finalScore >= 4 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val summaryStr = if (summaryPoints.isNotEmpty()) {
            summaryPoints.joinToString(" • ")
        } else {
            "Đã đồng bộ kiểm tra chéo từ WHOIS và 10 đối tác an ninh"
        }

        return baseResult.copy(
            score = finalScore,
            probability = finalScore / 10.0f,
            riskLevel = finalRiskLevel,
            reasons = combinedReasons,
            isFused = true,
            fusionSummary = summaryStr
        )
    }

    private fun calculateAgeDays(dateStr: String): Long? {
        val formats = listOf(
            SimpleDateFormat("yyyy-MM-dd", Locale.US),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US),
            SimpleDateFormat("yyyy/MM/dd", Locale.US)
        )
        for (f in formats) {
            try {
                val date = f.parse(dateStr)
                if (date != null) {
                    val diff = System.currentTimeMillis() - date.time
                    return diff / (1000 * 60 * 60 * 24)
                }
            } catch (_: Exception) {}
        }
        return null
    }
}
