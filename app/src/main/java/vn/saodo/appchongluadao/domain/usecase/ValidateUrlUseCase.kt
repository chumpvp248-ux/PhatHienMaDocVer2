package vn.saodo.appchongluadao.domain.usecase

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class ValidateUrlUseCase {

    sealed class ValidationResult {
        data class Valid(
            val normalizedUrl: String,
            val displayUrl: String,
            val scheme: String,
            val host: String,
            val path: String,
            val query: String,
            val hasUserinfo: Boolean
        ) : ValidationResult()

        data class Invalid(val reason: String) : ValidationResult()
    }

    fun execute(rawInput: String?): ValidationResult {
        if (rawInput.isNullOrBlank()) {
            return ValidationResult.Invalid("URL không được để trống")
        }

        val trimmed = rawInput.trim()
        if (trimmed.length > 4096) {
            return ValidationResult.Invalid("URL vượt quá giới hạn độ dài cho phép (tối đa 4096 ký tự)")
        }

        // Tự động bổ sung https:// nếu người dùng chỉ nhập domain thông thường (ví dụ: google.com)
        val candidate = if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            if (trimmed.contains("://")) {
                return ValidationResult.Invalid("Giao thức không được hỗ trợ. Ứng dụng chỉ phân tích HTTP và HTTPS")
            }
            "https://$trimmed"
        } else {
            trimmed
        }

        return try {
            val uri = URI(candidate)
            val scheme = uri.scheme?.lowercase() ?: return ValidationResult.Invalid("Thiếu scheme giao thức")
            if (scheme != "http" && scheme != "https") {
                return ValidationResult.Invalid("Chỉ hỗ trợ giao thức HTTP và HTTPS")
            }

            val host = uri.host ?: return ValidationResult.Invalid("Không tìm thấy tên miền hoặc máy chủ hợp lệ")
            val asciiHost = java.net.IDN.toASCII(host).lowercase()
            val path = uri.rawPath ?: ""
            val query = uri.rawQuery ?: ""
            val hasUserinfo = uri.userInfo != null

            // Tạo Display URL che các tham số nhạy cảm (token, password, key, secret)
            val displayUrl = maskSensitiveQueryParams(candidate)

            ValidationResult.Valid(
                normalizedUrl = candidate,
                displayUrl = displayUrl,
                scheme = scheme,
                host = asciiHost,
                path = path,
                query = query,
                hasUserinfo = hasUserinfo
            )
        } catch (e: Exception) {
            ValidationResult.Invalid("Cú pháp URL không hợp lệ: ${e.localizedMessage}")
        }
    }

    private fun maskSensitiveQueryParams(url: String): String {
        val sensitiveKeys = listOf("token", "pass", "password", "secret", "key", "auth", "otp", "code", "session")
        var masked = url
        for (key in sensitiveKeys) {
            val regex = Regex("""(?i)([?&]$key=)([^&#]+)""")
            masked = regex.replace(masked) { matchResult ->
                "${matchResult.groupValues[1]}***"
            }
        }
        return masked
    }
}
