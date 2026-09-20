package vn.saodo.appchongluadao.domain.model

data class DomainWhoisInfo(
    val ipAddress: String? = null,
    val ipLocation: String? = null,
    val isp: String? = null,
    val registrar: String? = null,
    val registrant: String? = null,
    val registrationDate: String? = null,
    val expirationDate: String? = null,
    val updatedDate: String? = null,
    val nameservers: String? = null
)

enum class ThreatStatusType {
    NOT_FOUND,
    SAFE,
    SUSPICIOUS,
    MALICIOUS
}

data class ThirdPartyReputation(
    val name: String,
    val statusText: String,
    val statusType: ThreatStatusType
)

data class FullEnrichmentResult(
    val host: String,
    val whois: DomainWhoisInfo?,
    val chongLuaDaoRiskTitle: String,
    val chongLuaDaoRiskLevel: RiskLevel,
    val thirdParties: List<ThirdPartyReputation>,
    val updatedAtFormatted: String
)
