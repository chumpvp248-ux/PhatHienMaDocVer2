package vn.saodo.appchongluadao.domain.usecase

import vn.saodo.appchongluadao.data.model.TFLiteModelRunner
import vn.saodo.appchongluadao.domain.model.FeatureVector
import vn.saodo.appchongluadao.domain.model.RiskLevel
import vn.saodo.appchongluadao.domain.model.ScanReason
import vn.saodo.appchongluadao.domain.model.UrlAnalysisResult
import java.util.UUID

class AnalyzeOfflineUseCase(
    private val validateUrlUseCase: ValidateUrlUseCase,
    private val extractFeaturesUseCase: ExtractFeaturesUseCase,
    private val modelRunner: TFLiteModelRunner
) {
    private val shortenerHosts = setOf(
        "bit.ly", "tinyurl.com", "t.co", "goo.gl", "is.gd", "buff.ly", "ow.ly", "short.ly"
    )

    sealed class Result {
        data class Success(val analysis: UrlAnalysisResult) : Result()
        data class Error(val message: String) : Result()
    }

    fun execute(rawInput: String): Result {
        val validation = validateUrlUseCase.execute(rawInput)
        if (validation is ValidateUrlUseCase.ValidationResult.Invalid) {
            return Result.Error(validation.reason)
        }

        val valid = validation as ValidateUrlUseCase.ValidationResult.Valid
        val features = extractFeaturesUseCase.execute(valid.normalizedUrl)
            ?: return Result.Error("Không thể trích xuất đặc trưng từ URL này")

        val prediction = modelRunner.predict(features)
        val score = prediction.riskScore
        val riskLevel = RiskLevel.fromScore(score)
        val reasons = generateReasons(valid.host, features, score)

        val encodedUrl = try {
            java.net.URLEncoder.encode(valid.normalizedUrl, "UTF-8")
        } catch (e: Exception) {
            valid.normalizedUrl
        }

        val threatSources = listOf(
            vn.saodo.appchongluadao.domain.model.ThreatIntelSource(
                name = "VirusTotal (90+ Engines)",
                description = "Đối soát với hơn 90 công cụ bảo mật hàng đầu thế giới",
                statusText = "Sẵn sàng tra cứu trực tiếp",
                isMalicious = false,
                lookupUrl = "https://www.virustotal.com/gui/search/$encodedUrl"
            ),
            vn.saodo.appchongluadao.domain.model.ThreatIntelSource(
                name = "Google Safe Browsing",
                description = "Hệ thống bảo vệ duyệt web an toàn toàn cầu của Google",
                statusText = "Sẵn sàng tra cứu minh bạch",
                isMalicious = false,
                lookupUrl = "https://transparencyreport.google.com/safe-browsing/search?url=$encodedUrl"
            ),
            vn.saodo.appchongluadao.domain.model.ThreatIntelSource(
                name = "OpenPhish Community Feed",
                description = "Dữ liệu lừa đảo theo thời gian thực (Zero-day Phishing)",
                statusText = "Sẵn sàng phân tích sâu",
                isMalicious = false,
                lookupUrl = "https://openphish.com"
            ),
            vn.saodo.appchongluadao.domain.model.ThreatIntelSource(
                name = "Chống Lừa Đảo Việt Nam",
                description = "Cơ sở dữ liệu phòng chống lừa đảo mạng tại Việt Nam",
                statusText = "Tra cứu cổng ChongLuaDao.vn",
                isMalicious = false,
                lookupUrl = "https://chongluadao.vn"
            )
        )

        val analysis = UrlAnalysisResult(
            scanId = UUID.randomUUID().toString(),
            rawUrl = valid.normalizedUrl,
            displayUrl = valid.displayUrl,
            host = valid.host,
            score = score,
            probability = prediction.probability,
            riskLevel = riskLevel,
            reasons = reasons,
            modelVersion = "1.0.0-mlp",
            timestamp = System.currentTimeMillis(),
            isOffline = true,
            threatIntelStatus = "Offline (Sẵn sàng tra cứu trực tuyến)",
            threatIntelSources = threatSources,
            screenshotStatus = "none"
        )

        return Result.Success(analysis)
    }

    private fun generateReasons(
        host: String,
        f: FeatureVector,
        score: Int
    ): List<ScanReason> {
        val list = mutableListOf<ScanReason>()

        // 1. Heuristic reasons
        if (f.hostIsIp == 1) {
            list.add(
                ScanReason(
                    code = "HEUR_HOST_IS_IP",
                    title = "Địa chỉ IP thô",
                    message = "Trang web sử dụng trực tiếp địa chỉ IP ($host) thay vì tên miền chuẩn.",
                    severity = "high",
                    source = "heuristic"
                )
            )
        }

        if (f.hasUserinfo == 1) {
            list.add(
                ScanReason(
                    code = "HEUR_HAS_USERINFO",
                    title = "Chứa thông tin xác thực giả mạo",
                    message = "URL chứa ký tự '@' trước tên miền để ngụy trang đánh lừa người dùng.",
                    severity = "high",
                    source = "heuristic"
                )
            )
        }

        if (f.hostHasPunycode == 1) {
            list.add(
                ScanReason(
                    code = "HEUR_PUNYCODE",
                    title = "Tên miền Punycode (Ký tự quốc tế)",
                    message = "Tên miền chứa tiền tố 'xn--', có khả năng giả mạo ký tự đồng dạng.",
                    severity = "medium",
                    source = "heuristic"
                )
            )
        }

        if (f.sensitiveKeywordCount > 0) {
            list.add(
                ScanReason(
                    code = "HEUR_SENSITIVE_KEYWORDS",
                    title = "Từ khóa nhạy cảm trong đường dẫn",
                    message = "Đường dẫn chứa ${f.sensitiveKeywordCount} từ khóa liên quan đến đăng nhập, ngân hàng, bảo mật.",
                    severity = "high",
                    source = "heuristic"
                )
            )
        }

        if (f.subdomainCount >= 3) {
            list.add(
                ScanReason(
                    code = "HEUR_HIGH_SUBDOMAINS",
                    title = "Nhiều cấp tên miền con bất thường",
                    message = "Tên miền có ${f.subdomainCount} cấp subdomain, thường dùng để mạo danh cấu trúc các thương hiệu lớn.",
                    severity = "medium",
                    source = "heuristic"
                )
            )
        }

        if (f.hostEntropy >= 3.8f && host.length >= 10) {
            list.add(
                ScanReason(
                    code = "HEUR_HIGH_ENTROPY",
                    title = "Độ hỗn loạn ký tự cao",
                    message = "Tên miền chứa chuỗi ký tự ngẫu nhiên bất thường (entropy: ${f.hostEntropy}).",
                    severity = "medium",
                    source = "heuristic"
                )
            )
        }

        if (f.hostHyphenCount >= 3) {
            list.add(
                ScanReason(
                    code = "HEUR_EXCESSIVE_HYPHENS",
                    title = "Nhiều dấu gạch ngang",
                    message = "Tên miền chứa ${f.hostHyphenCount} dấu gạch ngang, dấu hiệu chèn từ khóa giả mạo.",
                    severity = "medium",
                    source = "heuristic"
                )
            )
        }

        if (shortenerHosts.contains(host)) {
            list.add(
                ScanReason(
                    code = "HEUR_SHORTENER",
                    title = "Dịch vụ liên kết rút gọn",
                    message = "Liên kết rút gọn ($host) che giấu địa chỉ trang đích thực tế.",
                    severity = "medium",
                    source = "heuristic"
                )
            )
        }

        if (f.usesHttps == 0) {
            list.add(
                ScanReason(
                    code = "HEUR_NO_HTTPS",
                    title = "Không sử dụng HTTPS",
                    message = "Liên kết sử dụng HTTP thông thường, không được mã hóa an toàn.",
                    severity = "low",
                    source = "heuristic"
                )
            )
        }

        // 2. Model attribution reasons
        when {
            score >= 7 -> {
                list.add(
                    ScanReason(
                        code = "MODEL_HIGH_RISK",
                        title = "AI đánh giá nguy cơ cao",
                        message = "Mạng nơ-ron TFLite phân tích 20 đặc trưng từ vựng và kết luận liên kết này có nguy cơ lừa đảo cao.",
                        severity = "high",
                        source = "model"
                    )
                )
            }
            score in 4..6 -> {
                list.add(
                    ScanReason(
                        code = "MODEL_MODERATE_RISK",
                        title = "AI khuyến nghị thận trọng",
                        message = "Cấu trúc URL có một số đặc điểm đáng ngờ, người dùng nên kiểm tra kỹ trước khi thao tác.",
                        severity = "medium",
                        source = "model"
                    )
                )
            }
            else -> {
                list.add(
                    ScanReason(
                        code = "MODEL_LOW_RISK",
                        title = "Ít dấu hiệu bất thường",
                        message = "Cấu trúc URL tương đồng với các trang web thông thường, không phát hiện dấu hiệu lừa đảo nổi bật.",
                        severity = "info",
                        source = "model"
                    )
                )
            }
        }

        return list
    }
}
