package vn.saodo.appchongluadao.ui.viewmodel

import vn.saodo.appchongluadao.domain.model.FullEnrichmentResult
import vn.saodo.appchongluadao.domain.model.UrlAnalysisResult

sealed class AnalysisState {
    object Idle : AnalysisState()
    object Validating : AnalysisState()
    object OfflineAnalyzing : AnalysisState()
    data class OfflineReady(val result: UrlAnalysisResult) : AnalysisState()
    data class OnlinePending(val offlineResult: UrlAnalysisResult) : AnalysisState()
    data class Complete(val result: UrlAnalysisResult) : AnalysisState()
    data class Error(val message: String) : AnalysisState()
}

sealed class EnrichmentState {
    object Idle : EnrichmentState()
    object Loading : EnrichmentState()
    data class Success(val data: FullEnrichmentResult) : EnrichmentState()
    object Unavailable : EnrichmentState()
}
