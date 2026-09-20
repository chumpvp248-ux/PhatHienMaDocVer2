package vn.saodo.appchongluadao.domain.model

data class ThreatIntelSource(
    val name: String,
    val description: String,
    val statusText: String,
    val isMalicious: Boolean,
    val lookupUrl: String? = null
)

data class UrlAnalysisResult(
    val scanId: String,
    val rawUrl: String,
    val displayUrl: String,
    val host: String,
    val score: Int, // 1..10
    val probability: Float,
    val riskLevel: RiskLevel,
    val reasons: List<ScanReason>,
    val modelVersion: String = "1.0.0-mlp",
    val timestamp: Long = System.currentTimeMillis(),
    val isOffline: Boolean = true,
    val threatIntelStatus: String? = null,
    val threatIntelSources: List<ThreatIntelSource> = emptyList(),
    val screenshotUrl: String? = null,
    val screenshotBase64: String? = null,
    val screenshotStatus: String = "none", // "none", "pending", "ready", "unavailable"
    val isFused: Boolean = false,
    val fusionSummary: String? = null
)
