package vn.saodo.appchongluadao.domain.model

enum class RiskLevel(val title: String, val minScore: Int, val maxScore: Int) {
    LOW("Ít dấu hiệu rủi ro", 1, 3),
    MEDIUM("Cần thận trọng", 4, 6),
    HIGH("Nguy cơ lừa đảo cao", 7, 10);

    companion object {
        fun fromScore(score: Int): RiskLevel {
            return when (score) {
                in 1..3 -> LOW
                in 4..6 -> MEDIUM
                else -> HIGH
            }
        }
    }
}
