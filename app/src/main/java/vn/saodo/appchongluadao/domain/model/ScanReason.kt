package vn.saodo.appchongluadao.domain.model

data class ScanReason(
    val code: String,
    val title: String,
    val message: String,
    val severity: String, // "info", "low", "medium", "high", "critical"
    val source: String   // "heuristic", "model", "threat_intel"
)
