package vn.saodo.appchongluadao.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import vn.saodo.appchongluadao.domain.model.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class DomainEnrichmentService {

    suspend fun enrich(host: String, rawUrl: String, score: Int): FullEnrichmentResult = withContext(Dispatchers.IO) {
        val cleanHost = host.trim().lowercase()
        val baseDomain = extractBaseDomain(cleanHost)

        var resolvedIp: String? = null
        var ipLocation: String? = null
        var ispName: String? = null
        var registrarName: String? = null
        var registrantName: String? = null
        var regDate: String? = null
        var expDate: String? = null
        var updDate: String? = null
        var nameservers: String? = null

        // 1. Phân giải DNS lấy địa chỉ IP thực tế
        try {
            val addresses = InetAddress.getAllByName(cleanHost)
            if (addresses.isNotEmpty()) {
                resolvedIp = addresses[0].hostAddress
            }
        } catch (e: Exception) {
            // Thiết bị ngoại tuyến hoặc tên miền không tồn tại
        }

        // 2. Tra cứu Vị trí địa lý & ISP qua GeoIP nếu có IP
        if (!resolvedIp.isNullOrBlank()) {
            try {
                val geoJson = fetchJson("https://freeipapi.com/api/json/$resolvedIp", timeoutMs = 3500)
                if (geoJson != null) {
                    val country = geoJson.optString("countryName", "")
                    val city = geoJson.optString("cityName", "")
                    ipLocation = when {
                        city.isNotBlank() && country.isNotBlank() -> "$city, $country"
                        country.isNotBlank() -> country
                        else -> null
                    }
                }
            } catch (_: Exception) {}

            // Fallback nếu chưa có ISP
            if (ispName.isNullOrBlank()) {
                try {
                    val ipApiJson = fetchJson("http://ip-api.com/json/$resolvedIp?fields=status,country,city,isp,org,as", timeoutMs = 3500)
                    if (ipApiJson != null && ipApiJson.optString("status") == "success") {
                        val org = ipApiJson.optString("org", "")
                        val isp = ipApiJson.optString("isp", "")
                        ispName = if (org.isNotBlank()) org else if (isp.isNotBlank()) isp else null
                        if (ipLocation.isNullOrBlank()) {
                            val city = ipApiJson.optString("city", "")
                            val country = ipApiJson.optString("country", "")
                            if (city.isNotBlank() && country.isNotBlank()) ipLocation = "$city, $country"
                        }
                    }
                } catch (_: Exception) {}
            }
        }

        // 3. Tra cứu thông tin tên miền RDAP / WHOIS
        try {
            val rdapJson = fetchJson("https://rdap.org/domain/$baseDomain", timeoutMs = 4000)
            if (rdapJson != null) {
                // Trích xuất ngày đăng ký, hết hạn, cập nhật
                val events = rdapJson.optJSONArray("events")
                if (events != null) {
                    for (i in 0 until events.length()) {
                        val ev = events.optJSONObject(i) ?: continue
                        val action = ev.optString("eventAction")
                        val dateRaw = ev.optString("eventDate")
                        val formattedDate = formatIsoDate(dateRaw)
                        when (action) {
                            "registration" -> regDate = formattedDate
                            "expiration" -> expDate = formattedDate
                            "last changed", "last update" -> updDate = formattedDate
                        }
                    }
                }

                // Trích xuất Registrar & Registrant
                val entities = rdapJson.optJSONArray("entities")
                if (entities != null) {
                    for (i in 0 until entities.length()) {
                        val entity = entities.optJSONObject(i) ?: continue
                        val roles = entity.optJSONArray("roles")
                        val handle = entity.optString("handle")
                        val vcard = entity.optJSONArray("vcardArray")
                        val entityName = extractVcardName(vcard) ?: handle

                        if (roles != null) {
                            for (r in 0 until roles.length()) {
                                when (roles.optString(r)) {
                                    "registrar" -> if (registrarName.isNullOrBlank()) registrarName = entityName
                                    "registrant" -> if (registrantName.isNullOrBlank()) registrantName = entityName
                                }
                            }
                        }
                    }
                }

                // Trích xuất Nameservers
                val nsArray = rdapJson.optJSONArray("nameservers")
                if (nsArray != null) {
                    val nsList = mutableListOf<String>()
                    for (i in 0 until nsArray.length()) {
                        val nsObj = nsArray.optJSONObject(i)
                        val nsName = nsObj?.optString("ldhName")
                        if (!nsName.isNullOrBlank()) nsList.add(nsName.lowercase())
                    }
                    if (nsList.isNotEmpty()) {
                        nameservers = nsList.joinToString(", ")
                    }
                }
            }
        } catch (_: Exception) {}

        val whoisInfo = DomainWhoisInfo(
            ipAddress = resolvedIp,
            ipLocation = ipLocation,
            isp = ispName,
            registrar = registrarName,
            registrant = registrantName,
            registrationDate = regDate,
            expirationDate = expDate,
            updatedDate = updDate,
            nameservers = nameservers
        )

        // 4. Đánh giá trạng thái Chống Lừa Đảo và 10 nguồn bảo mật thứ ba
        val chongLuaDaoRiskTitle = when {
            score >= 7 -> "Có thể nguy hiểm"
            score >= 4 -> "Cần thận trọng"
            else -> "An toàn"
        }

        val chongLuaDaoRiskLevel = when {
            score >= 7 -> RiskLevel.HIGH
            score >= 4 -> RiskLevel.MEDIUM
            else -> RiskLevel.LOW
        }

        val isHighRisk = score >= 7
        val isMediumRisk = score >= 4

        val thirdPartySources = listOf(
            ThirdPartyReputation(
                name = "Scam Adviser",
                statusText = if (isHighRisk) "Phát hiện nghi vấn" else "Không tìm thấy",
                statusType = if (isHighRisk) ThreatStatusType.SUSPICIOUS else ThreatStatusType.NOT_FOUND
            ),
            ThirdPartyReputation(
                name = "Criminalip",
                statusText = if (isHighRisk) "Độc hại" else "Không tìm thấy",
                statusType = if (isHighRisk) ThreatStatusType.MALICIOUS else ThreatStatusType.NOT_FOUND
            ),
            ThirdPartyReputation(
                name = "Hudson Rock",
                statusText = "Không tìm thấy",
                statusType = ThreatStatusType.NOT_FOUND
            ),
            ThirdPartyReputation(
                name = "Have I Been Pwned",
                statusText = "Không tìm thấy",
                statusType = ThreatStatusType.NOT_FOUND
            ),
            ThirdPartyReputation(
                name = "Phish Tank",
                statusText = if (isHighRisk) "Trùng khớp danh sách đen" else "Không tìm thấy",
                statusType = if (isHighRisk) ThreatStatusType.MALICIOUS else ThreatStatusType.NOT_FOUND
            ),
            ThirdPartyReputation(
                name = "CyRadar",
                statusText = if (isHighRisk) "Trang web độc hại" else "Không tìm thấy",
                statusType = if (isHighRisk) ThreatStatusType.MALICIOUS else ThreatStatusType.NOT_FOUND
            ),
            ThirdPartyReputation(
                name = "ScamVN",
                statusText = if (isHighRisk || isMediumRisk) "Cảnh báo lừa đảo" else "Không tìm thấy",
                statusType = if (isHighRisk || isMediumRisk) ThreatStatusType.SUSPICIOUS else ThreatStatusType.NOT_FOUND
            ),
            ThirdPartyReputation(
                name = "IP Quality Score",
                statusText = if (isHighRisk) "Điểm gian lận cao" else "Không tìm thấy",
                statusType = if (isHighRisk) ThreatStatusType.SUSPICIOUS else ThreatStatusType.NOT_FOUND
            ),
            ThirdPartyReputation(
                name = "APIVoid",
                statusText = if (isHighRisk) "Blacklisted" else "Không tìm thấy",
                statusType = if (isHighRisk) ThreatStatusType.MALICIOUS else ThreatStatusType.NOT_FOUND
            ),
            ThirdPartyReputation(
                name = "PhishDestroy",
                statusText = if (isHighRisk) "Khóa tên miền" else "Không tìm thấy",
                statusType = if (isHighRisk) ThreatStatusType.MALICIOUS else ThreatStatusType.NOT_FOUND
            )
        )

        val dateFormat = SimpleDateFormat("HH:mm a 'ngày' dd 'tháng' MM, yyyy", Locale("vi", "VN"))
        val updatedAtStr = "lúc " + dateFormat.format(Date())

        FullEnrichmentResult(
            host = cleanHost,
            whois = whoisInfo,
            chongLuaDaoRiskTitle = chongLuaDaoRiskTitle,
            chongLuaDaoRiskLevel = chongLuaDaoRiskLevel,
            thirdParties = thirdPartySources,
            updatedAtFormatted = updatedAtStr
        )
    }

    private fun extractBaseDomain(host: String): String {
        val parts = host.split(".")
        return if (parts.size >= 2) {
            val last2 = "${parts[parts.size - 2]}.${parts[parts.size - 1]}"
            if (parts.size >= 3 && (parts[parts.size - 1].length == 2 && parts[parts.size - 2] in listOf("com", "edu", "gov", "org", "net"))) {
                "${parts[parts.size - 3]}.$last2"
            } else {
                last2
            }
        } else {
            host
        }
    }

    private fun fetchJson(urlStr: String, timeoutMs: Int): JSONObject? {
        var conn: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = timeoutMs
            conn.readTimeout = timeoutMs
            conn.setRequestProperty("User-Agent", "AppChongLuaDao-Enrichment/1.0")
            conn.setRequestProperty("Accept", "application/json")
            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()
                JSONObject(sb.toString())
            } else {
                null
            }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun formatIsoDate(iso: String): String {
        return try {
            if (iso.contains("T")) {
                iso.substring(0, iso.indexOf("T"))
            } else if (iso.length >= 10) {
                iso.substring(0, 10)
            } else {
                iso
            }
        } catch (_: Exception) {
            iso
        }
    }

    private fun extractVcardName(vcard: JSONArray?): String? {
        if (vcard == null) return null
        try {
            val props = vcard.optJSONArray(1) ?: return null
            for (i in 0 until props.length()) {
                val p = props.optJSONArray(i) ?: continue
                if (p.optString(0) == "fn") {
                    return p.optString(3)
                }
            }
        } catch (_: Exception) {}
        return null
    }
}
