package vn.saodo.appchongluadao.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import vn.saodo.appchongluadao.domain.model.ThreatIntelSource
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

class OnlineAnalysisClient(private var configuredBaseUrl: String = "http://10.0.2.2:8000") {

    data class OnlineResponse(
        val success: Boolean,
        val threatIntelStatus: String,
        val screenshotUrl: String?,
        val screenshotBase64: String?,
        val screenshotStatus: String,
        val onlineScore: Int?,
        val threatIntelSources: List<ThreatIntelSource> = emptyList(),
        val errorMessage: String? = null
    )

    private fun getCandidateUrls(): List<String> {
        val list = mutableListOf(configuredBaseUrl)
        if (!list.contains("http://192.168.1.181:8000")) list.add("http://192.168.1.181:8000")
        if (!list.contains("http://127.0.0.1:8000")) list.add("http://127.0.0.1:8000")
        if (!list.contains("http://10.0.2.2:8000")) list.add("http://10.0.2.2:8000")
        return list.distinct()
    }

    suspend fun analyzeUrl(
        url: String,
        consentThreatLookup: Boolean = true,
        consentScreenshot: Boolean = true
    ): OnlineResponse = withContext(Dispatchers.IO) {
        val candidates = getCandidateUrls()
        var lastException: Exception? = null

        for (targetBaseUrl in candidates) {
            var connection: HttpURLConnection? = null
            try {
                val endpoint = URL("$targetBaseUrl/api/v1/analyze-url")
                connection = (endpoint.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 3000
                    readTimeout = 7000
                    doOutput = true
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }

                val requestBody = JSONObject().apply {
                    put("url", url)
                    put("client_request_id", UUID.randomUUID().toString())
                    put("model_version", "1.0.0-mlp")
                    put("feature_schema_version", "1.0.0")
                    put("consent", JSONObject().apply {
                        put("threat_lookup", consentThreatLookup)
                        put("screenshot", consentScreenshot)
                    })
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(requestBody.toString())
                    writer.flush()
                }

                val responseCode = connection.responseCode
                if (responseCode in 200..202) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line)
                    }
                    reader.close()

                    val json = JSONObject(sb.toString())
                    val verdict = json.optString("verdict", "not_found")
                    val screenshotUrl = if (json.has("screenshot_url") && !json.isNull("screenshot_url")) {
                        json.getString("screenshot_url")
                    } else null
                    val screenshotBase64 = if (json.has("screenshot_base64") && !json.isNull("screenshot_base64")) {
                        json.getString("screenshot_base64")
                    } else null
                    val screenshotStatus = json.optString("screenshot_status", "none")
                    val score = if (json.has("score") && !json.isNull("score")) json.getInt("score") else null

                    val lookupUrlsJson = json.optJSONObject("lookup_urls")
                    val thirdPartyJson = json.optJSONObject("third_party_results")

                    val sources = mutableListOf<ThreatIntelSource>()

                    // 1. VirusTotal
                    val vtStatus = thirdPartyJson?.optJSONObject("VirusTotal")?.optString("status", "ready") ?: "ready"
                    val vtUrl = lookupUrlsJson?.optString("VirusTotal")
                    sources.add(
                        ThreatIntelSource(
                            name = "VirusTotal (90+ Engines)",
                            description = "Quét đa tầng từ hơn 90 hãng an ninh bảo mật hàng đầu",
                            statusText = if (vtStatus == "malicious") "Cảnh báo độc hại" else "Sẵn sàng tra cứu trực tiếp",
                            isMalicious = vtStatus == "malicious",
                            lookupUrl = vtUrl
                        )
                    )

                    // 2. Google Safe Browsing
                    val gsbUrl = lookupUrlsJson?.optString("Google Safe Browsing")
                    sources.add(
                        ThreatIntelSource(
                            name = "Google Safe Browsing",
                            description = "Cơ sở dữ liệu duyệt web an toàn toàn cầu của Google",
                            statusText = "Sẵn sàng đối soát minh bạch",
                            isMalicious = false,
                            lookupUrl = gsbUrl
                        )
                    )

                    // 3. OpenPhish Live Feed
                    val opStatus = thirdPartyJson?.optJSONObject("OpenPhish")?.optString("status", "not_found") ?: "not_found"
                    val opIsMalicious = opStatus == "malicious"
                    sources.add(
                        ThreatIntelSource(
                            name = "OpenPhish Community Feed",
                            description = "Dữ liệu lừa đảo theo thời gian thực (Zero-day Phishing)",
                            statusText = if (opIsMalicious) "Phát hiện lừa đảo trong feed" else "Đã kiểm tra: Không có trong danh sách đen",
                            isMalicious = opIsMalicious,
                            lookupUrl = "https://openphish.com"
                        )
                    )

                    // 4. Chống Lừa Đảo Việt Nam
                    val cldUrl = lookupUrlsJson?.optString("Chống Lừa Đảo VN") ?: "https://chongluadao.vn"
                    sources.add(
                        ThreatIntelSource(
                            name = "Chống Lừa Đảo Việt Nam",
                            description = "Hệ sinh thái bảo vệ người dùng mạng tại Việt Nam",
                            statusText = "Tra cứu cổng ChongLuaDao.vn",
                            isMalicious = false,
                            lookupUrl = cldUrl
                        )
                    )

                    return@withContext OnlineResponse(
                        success = true,
                        threatIntelStatus = "Trực tuyến: $verdict",
                        screenshotUrl = screenshotUrl,
                        screenshotBase64 = screenshotBase64,
                        screenshotStatus = screenshotStatus,
                        onlineScore = score,
                        threatIntelSources = sources
                    )
                }
            } catch (e: Exception) {
                lastException = e
            } finally {
                connection?.disconnect()
            }
        }

        // Nếu tất cả candidate server đều không kết nối được (offline hoàn toàn)
        OnlineResponse(
            success = false,
            threatIntelStatus = "Chưa kết nối server backend / Ngoại tuyến",
            screenshotUrl = null,
            screenshotBase64 = null,
            screenshotStatus = "unavailable",
            onlineScore = null,
            errorMessage = lastException?.localizedMessage
        )
    }
}
