package vn.saodo.appchongluadao.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurityHelper(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback sang SharedPreferences thông thường trong môi trường test/thiết bị cũ
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    var isOnlineAnalysisEnabled: Boolean
        get() = prefs.getBoolean(KEY_ONLINE_CONSENT, true)
        set(value) = prefs.edit().putBoolean(KEY_ONLINE_CONSENT, value).apply()

    var serverBaseUrl: String
        get() = prefs.getString(KEY_SERVER_URL, "http://10.0.2.2:8000") ?: "http://10.0.2.2:8000"
        set(value) = prefs.edit().putString(KEY_SERVER_URL, value).apply()

    var isHistorySaveEnabled: Boolean
        get() = prefs.getBoolean(KEY_HISTORY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_HISTORY_ENABLED, value).apply()

    var historyRetentionDays: Int
        get() = prefs.getInt(KEY_RETENTION_DAYS, 30)
        set(value) = prefs.edit().putInt(KEY_RETENTION_DAYS, value).apply()

    companion object {
        private const val PREFS_NAME = "secure_app_settings"
        private const val KEY_ONLINE_CONSENT = "key_online_consent"
        private const val KEY_SERVER_URL = "key_server_url"
        private const val KEY_HISTORY_ENABLED = "key_history_enabled"
        private const val KEY_RETENTION_DAYS = "key_retention_days"
    }
}
