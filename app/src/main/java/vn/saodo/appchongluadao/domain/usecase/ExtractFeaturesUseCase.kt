package vn.saodo.appchongluadao.domain.usecase

import vn.saodo.appchongluadao.domain.model.FeatureVector
import java.net.InetAddress
import java.net.URI
import kotlin.math.ln
import kotlin.math.max

class ExtractFeaturesUseCase {

    private val twoPartPublicSuffixes = setOf(
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
        "com.tr", "net.tr", "org.tr", "gov.tr", "edu.tr"
    )

    private val sensitiveKeywords = listOf("login", "verify", "bank", "secure", "account", "update")

    fun execute(normalizedUrl: String): FeatureVector? {
        val uri = try {
            URI(normalizedUrl)
        } catch (e: Exception) {
            return null
        }

        val rawHost = uri.host ?: return null
        val asciiHost = try {
            java.net.IDN.toASCII(rawHost).lowercase()
        } catch (e: Exception) {
            rawHost.lowercase()
        }

        val path = uri.rawPath ?: ""
        val query = uri.rawQuery ?: ""
        val scheme = uri.scheme?.lowercase() ?: ""
        val hasUserinfo = if (uri.userInfo != null || normalizedUrl.contains("@")) 1 else 0

        val urlLength = normalizedUrl.length
        val hostLength = asciiHost.length
        val pathLength = path.length
        val queryLength = query.length

        val hostDotCount = asciiHost.count { it == '.' }
        val hostHyphenCount = asciiHost.count { it == '-' }
        val urlAtCount = normalizedUrl.count { it == '@' }
        val urlPercentCount = normalizedUrl.count { it == '%' }
        val queryAmpCount = query.count { it == '&' }
        val queryEqualCount = query.count { it == '=' }

        val digitCount = normalizedUrl.count { it.isDigit() }
        val urlDigitRatio = (digitCount.toFloat() / max(1, urlLength)).round6()

        val letterCount = normalizedUrl.count { it in 'a'..'z' || it in 'A'..'Z' }
        val urlLetterRatio = (letterCount.toFloat() / max(1, urlLength)).round6()

        val hostDigitCount = asciiHost.count { it.isDigit() }
        val hostDigitRatio = (hostDigitCount.toFloat() / max(1, hostLength)).round6()

        val hostIsIp = if (isIpAddress(asciiHost)) 1 else 0
        val subdomainCount = calculateSubdomains(asciiHost, hostIsIp == 1)
        val usesHttps = if (scheme == "https") 1 else 0

        val hostHasPunycode = if (asciiHost.split('.').any { it.startsWith("xn--") }) 1 else 0

        val urlLower = normalizedUrl.lowercase()
        val sensitiveKeywordCount = sensitiveKeywords.count { kw -> urlLower.contains(kw) }

        val hostEntropy = calculateShannonEntropy(asciiHost).round6()

        return FeatureVector(
            urlLength = urlLength,
            hostLength = hostLength,
            pathLength = pathLength,
            queryLength = queryLength,
            hostDotCount = hostDotCount,
            hostHyphenCount = hostHyphenCount,
            urlAtCount = urlAtCount,
            urlPercentCount = urlPercentCount,
            queryAmpCount = queryAmpCount,
            queryEqualCount = queryEqualCount,
            urlDigitRatio = urlDigitRatio,
            urlLetterRatio = urlLetterRatio,
            hostDigitRatio = hostDigitRatio,
            hostIsIp = hostIsIp,
            subdomainCount = subdomainCount,
            usesHttps = usesHttps,
            hostHasPunycode = hostHasPunycode,
            sensitiveKeywordCount = sensitiveKeywordCount,
            hostEntropy = hostEntropy,
            hasUserinfo = hasUserinfo
        )
    }

    private fun isIpAddress(host: String): Boolean {
        val clean = host.trim().removeSurrounding("[", "]")
        // IPv4 regex check
        val ipv4Regex = Regex("""^(\d{1,3}\.){3}\d{1,3}$""")
        if (ipv4Regex.matches(clean)) {
            val parts = clean.split('.')
            if (parts.size == 4 && parts.all { it.toIntOrNull() in 0..255 }) {
                return true
            }
        }
        // IPv6 check
        return try {
            if (clean.contains(':')) {
                InetAddress.getByName(clean)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun calculateSubdomains(host: String, isIp: Boolean): Int {
        if (isIp || host.isBlank()) return 0
        val labels = host.trim('.').split('.')
        val n = labels.size
        if (n <= 1) return 0

        if (n >= 2) {
            val lastTwo = "${labels[n - 2]}.${labels[n - 1]}".lowercase()
            if (twoPartPublicSuffixes.contains(lastTwo)) {
                return max(0, n - 3)
            }
        }
        return max(0, n - 2)
    }

    private fun calculateShannonEntropy(s: String): Float {
        if (s.isEmpty()) return 0.0f
        val freq = mutableMapOf<Char, Int>()
        for (c in s) {
            freq[c] = (freq[c] ?: 0) + 1
        }
        val len = s.length.toDouble()
        val log2 = ln(2.0)
        var entropy = 0.0
        for (count in freq.values) {
            val p = count / len
            entropy -= p * (ln(p) / log2)
        }
        return entropy.toFloat()
    }

    private fun Float.round6(): Float {
        return (Math.round(this * 1_000_000.0) / 1_000_000.0).toFloat()
    }
}
