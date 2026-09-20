package vn.saodo.appchongluadao.data.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    private val mutex = Mutex()
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _historyFlow = MutableStateFlow<List<ScanHistoryEntity>>(emptyList())
    val historyFlow: Flow<List<ScanHistoryEntity>> = _historyFlow.asStateFlow()

    init {
        // Tự động nạp dữ liệu ban đầu và xóa dữ liệu cũ quá 30 ngày theo chính sách lưu trữ
        scope.launch {
            deleteOlderThan(System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000)
            refreshCache()
        }
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_HISTORY (
                $COL_ID TEXT PRIMARY KEY,
                $COL_DISPLAY_URL TEXT NOT NULL,
                $COL_RAW_URL TEXT NOT NULL,
                $COL_HOST TEXT NOT NULL,
                $COL_SCORE INTEGER NOT NULL,
                $COL_RISK_LEVEL TEXT NOT NULL,
                $COL_REASONS_JSON TEXT NOT NULL,
                $COL_TIMESTAMP INTEGER NOT NULL,
                $COL_MODEL_VERSION TEXT NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_host ON $TABLE_HISTORY($COL_HOST)")
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_timestamp ON $TABLE_HISTORY($COL_TIMESTAMP)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_HISTORY")
        onCreate(db)
    }

    suspend fun insert(scan: ScanHistoryEntity) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val values = ContentValues().apply {
                put(COL_ID, scan.id)
                put(COL_DISPLAY_URL, scan.displayUrl)
                put(COL_RAW_URL, scan.rawUrl)
                put(COL_HOST, scan.host)
                put(COL_SCORE, scan.score)
                put(COL_RISK_LEVEL, scan.riskLevel)
                put(COL_REASONS_JSON, scan.reasonsJson)
                put(COL_TIMESTAMP, scan.timestamp)
                put(COL_MODEL_VERSION, scan.modelVersion)
            }
            writableDatabase.insertWithOnConflict(
                TABLE_HISTORY,
                null,
                values,
                SQLiteDatabase.CONFLICT_REPLACE
            )
        }
        refreshCache()
    }

    suspend fun deleteById(id: String) = withContext(Dispatchers.IO) {
        mutex.withLock {
            writableDatabase.delete(TABLE_HISTORY, "$COL_ID = ?", arrayOf(id))
        }
        refreshCache()
    }

    suspend fun deleteAll() = withContext(Dispatchers.IO) {
        mutex.withLock {
            writableDatabase.delete(TABLE_HISTORY, null, null)
        }
        refreshCache()
    }

    suspend fun deleteOlderThan(cutoffTimestamp: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            writableDatabase.delete(
                TABLE_HISTORY,
                "$COL_TIMESTAMP < ?",
                arrayOf(cutoffTimestamp.toString())
            )
        }
        refreshCache()
    }

    fun searchByHost(query: String): Flow<List<ScanHistoryEntity>> {
        return historyFlow.map { list ->
            if (query.isBlank()) list
            else list.filter { it.host.contains(query, ignoreCase = true) }
        }
    }

    private suspend fun refreshCache() = withContext(Dispatchers.IO) {
        val list = mutableListOf<ScanHistoryEntity>()
        mutex.withLock {
            val cursor = readableDatabase.query(
                TABLE_HISTORY,
                null,
                null,
                null,
                null,
                null,
                "$COL_TIMESTAMP DESC"
            )
            cursor.use {
                val idxId = it.getColumnIndexOrThrow(COL_ID)
                val idxDisplay = it.getColumnIndexOrThrow(COL_DISPLAY_URL)
                val idxRaw = it.getColumnIndexOrThrow(COL_RAW_URL)
                val idxHost = it.getColumnIndexOrThrow(COL_HOST)
                val idxScore = it.getColumnIndexOrThrow(COL_SCORE)
                val idxRisk = it.getColumnIndexOrThrow(COL_RISK_LEVEL)
                val idxReasons = it.getColumnIndexOrThrow(COL_REASONS_JSON)
                val idxTime = it.getColumnIndexOrThrow(COL_TIMESTAMP)
                val idxModel = it.getColumnIndexOrThrow(COL_MODEL_VERSION)

                while (it.moveToNext()) {
                    list.add(
                        ScanHistoryEntity(
                            id = it.getString(idxId),
                            displayUrl = it.getString(idxDisplay),
                            rawUrl = it.getString(idxRaw),
                            host = it.getString(idxHost),
                            score = it.getInt(idxScore),
                            riskLevel = it.getString(idxRisk),
                            reasonsJson = it.getString(idxReasons),
                            timestamp = it.getLong(idxTime),
                            modelVersion = it.getString(idxModel)
                        )
                    )
                }
            }
        }
        _historyFlow.value = list
    }

    companion object {
        private const val DATABASE_NAME = "app_chong_lua_dao.db"
        private const val DATABASE_VERSION = 1

        private const val TABLE_HISTORY = "scan_history"
        private const val COL_ID = "id"
        private const val COL_DISPLAY_URL = "display_url"
        private const val COL_RAW_URL = "raw_url"
        private const val COL_HOST = "host"
        private const val COL_SCORE = "score"
        private const val COL_RISK_LEVEL = "risk_level"
        private const val COL_REASONS_JSON = "reasons_json"
        private const val COL_TIMESTAMP = "timestamp"
        private const val COL_MODEL_VERSION = "model_version"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppDatabase(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
