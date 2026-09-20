package vn.saodo.appchongluadao.data.local

data class ScanHistoryEntity(
    val id: String,
    val displayUrl: String,
    val rawUrl: String,
    val host: String,
    val score: Int,
    val riskLevel: String,
    val reasonsJson: String,
    val timestamp: Long,
    val modelVersion: String
)
