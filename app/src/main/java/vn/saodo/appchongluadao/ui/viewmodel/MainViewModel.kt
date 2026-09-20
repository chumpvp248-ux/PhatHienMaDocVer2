package vn.saodo.appchongluadao.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import vn.saodo.appchongluadao.data.local.AppDatabase
import vn.saodo.appchongluadao.data.local.ScanHistoryEntity
import vn.saodo.appchongluadao.data.local.SecurityHelper
import vn.saodo.appchongluadao.data.model.TFLiteModelRunner
import vn.saodo.appchongluadao.data.remote.OnlineAnalysisClient
import vn.saodo.appchongluadao.domain.model.RiskLevel
import vn.saodo.appchongluadao.domain.model.ScanReason
import vn.saodo.appchongluadao.domain.model.UrlAnalysisResult
import vn.saodo.appchongluadao.domain.usecase.AnalyzeOfflineUseCase
import vn.saodo.appchongluadao.domain.usecase.ExtractFeaturesUseCase
import vn.saodo.appchongluadao.domain.usecase.ValidateUrlUseCase

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val securityHelper = SecurityHelper(application)
    private val modelRunner = TFLiteModelRunner(application)
    private val validateUseCase = ValidateUrlUseCase()
    private val extractUseCase = ExtractFeaturesUseCase()
    private val analyzeOfflineUseCase = AnalyzeOfflineUseCase(validateUseCase, extractUseCase, modelRunner)
    private val domainEnrichmentService = vn.saodo.appchongluadao.data.remote.DomainEnrichmentService()
    private val decisionFusionUseCase = vn.saodo.appchongluadao.domain.usecase.DecisionFusionUseCase()

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private val _enrichmentState = MutableStateFlow<EnrichmentState>(EnrichmentState.Idle)
    val enrichmentState: StateFlow<EnrichmentState> = _enrichmentState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val scanHistory: StateFlow<List<ScanHistoryEntity>> = db.historyFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun analyzeUrl(rawUrl: String) {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Validating

            val offlineResult = analyzeOfflineUseCase.execute(rawUrl)
            if (offlineResult is AnalyzeOfflineUseCase.Result.Error) {
                _analysisState.value = AnalysisState.Error(offlineResult.message)
                return@launch
            }

            val analysis = (offlineResult as AnalyzeOfflineUseCase.Result.Success).analysis
            _analysisState.value = AnalysisState.OfflineReady(analysis)

            // Lưu vào SQLite nếu được bật trong cài đặt
            if (securityHelper.isHistorySaveEnabled) {
                saveAnalysisToHistory(analysis)
            }

            // Hoàn tất phân tích tức thì trên thiết bị (Offline-First 100%)
            _analysisState.value = AnalysisState.Complete(analysis)

            // Tự động làm giàu thông tin tên miền (WHOIS & bên thứ ba) ngầm trên mạng
            _enrichmentState.value = EnrichmentState.Loading
            launch(Dispatchers.IO) {
                try {
                    val result = domainEnrichmentService.enrich(analysis.host, analysis.rawUrl, analysis.score)
                    _enrichmentState.value = EnrichmentState.Success(result)

                    // Tổng hợp quyết định AI Fusion: Cập nhật kết luận và điểm số đa nguồn
                    val fused = decisionFusionUseCase.fuse(analysis, result)
                    _analysisState.value = AnalysisState.Complete(fused)
                    if (securityHelper.isHistorySaveEnabled) {
                        saveAnalysisToHistory(fused)
                    }
                } catch (e: Exception) {
                    _enrichmentState.value = EnrichmentState.Unavailable
                }
            }
        }
    }

    fun enrichFromHistory(baseResult: UrlAnalysisResult) {
        _enrichmentState.value = EnrichmentState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = domainEnrichmentService.enrich(baseResult.host, baseResult.rawUrl, baseResult.score)
                _enrichmentState.value = EnrichmentState.Success(result)

                val fused = decisionFusionUseCase.fuse(baseResult, result)
                _analysisState.value = AnalysisState.Complete(fused)
            } catch (e: Exception) {
                _enrichmentState.value = EnrichmentState.Unavailable
            }
        }
    }

    fun handleSharedText(text: String) {
        // Trích xuất URL đầu tiên từ văn bản chia sẻ
        val words = text.split("\\s+".toRegex())
        val foundUrl = words.firstOrNull { it.startsWith("http://", ignoreCase = true) || it.startsWith("https://", ignoreCase = true) }
            ?: words.firstOrNull { it.contains(".") && !it.contains(" ") }
            ?: text.trim()
        analyzeUrl(foundUrl)
    }

    fun resetState() {
        _analysisState.value = AnalysisState.Idle
        _enrichmentState.value = EnrichmentState.Idle
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            db.deleteById(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            db.deleteAll()
        }
    }

    private suspend fun saveAnalysisToHistory(result: UrlAnalysisResult) {
        val reasonsArray = JSONArray()
        for (r in result.reasons) {
            val obj = JSONObject().apply {
                put("code", r.code)
                put("title", r.title)
                put("message", r.message)
                put("severity", r.severity)
                put("source", r.source)
            }
            reasonsArray.put(obj)
        }

        val entity = ScanHistoryEntity(
            id = result.scanId,
            displayUrl = result.displayUrl,
            rawUrl = result.rawUrl,
            host = result.host,
            score = result.score,
            riskLevel = result.riskLevel.name,
            reasonsJson = reasonsArray.toString(),
            timestamp = result.timestamp,
            modelVersion = result.modelVersion
        )
        db.insert(entity)
    }

    override fun onCleared() {
        super.onCleared()
        modelRunner.close()
    }
}
