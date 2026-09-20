package vn.saodo.appchongluadao.domain.model

data class FeatureVector(
    val urlLength: Int,
    val hostLength: Int,
    val pathLength: Int,
    val queryLength: Int,
    val hostDotCount: Int,
    val hostHyphenCount: Int,
    val urlAtCount: Int,
    val urlPercentCount: Int,
    val queryAmpCount: Int,
    val queryEqualCount: Int,
    val urlDigitRatio: Float,
    val urlLetterRatio: Float,
    val hostDigitRatio: Float,
    val hostIsIp: Int,
    val subdomainCount: Int,
    val usesHttps: Int,
    val hostHasPunycode: Int,
    val sensitiveKeywordCount: Int,
    val hostEntropy: Float,
    val hasUserinfo: Int
) {
    fun toFloatArray(): FloatArray {
        return floatArrayOf(
            urlLength.toFloat(),
            hostLength.toFloat(),
            pathLength.toFloat(),
            queryLength.toFloat(),
            hostDotCount.toFloat(),
            hostHyphenCount.toFloat(),
            urlAtCount.toFloat(),
            urlPercentCount.toFloat(),
            queryAmpCount.toFloat(),
            queryEqualCount.toFloat(),
            urlDigitRatio,
            urlLetterRatio,
            hostDigitRatio,
            hostIsIp.toFloat(),
            subdomainCount.toFloat(),
            usesHttps.toFloat(),
            hostHasPunycode.toFloat(),
            sensitiveKeywordCount.toFloat(),
            hostEntropy,
            hasUserinfo.toFloat()
        )
    }
}
