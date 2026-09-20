package vn.saodo.appchongluadao

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import org.json.JSONArray
import vn.saodo.appchongluadao.data.local.ScanHistoryEntity
import vn.saodo.appchongluadao.domain.model.RiskLevel
import vn.saodo.appchongluadao.domain.model.ScanReason
import vn.saodo.appchongluadao.domain.model.UrlAnalysisResult
import vn.saodo.appchongluadao.ui.screens.*
import vn.saodo.appchongluadao.ui.theme.*
import vn.saodo.appchongluadao.ui.viewmodel.AnalysisState
import vn.saodo.appchongluadao.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Xử lý khi nhận chia sẻ URL qua Sharesheet (Intent.ACTION_SEND)
        handleIntent(intent)

        setContent {
            AppChongLuaDaoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                viewModel.handleSharedText(sharedText)
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val enrichmentState by viewModel.enrichmentState.collectAsStateWithLifecycle()
    val historyList by viewModel.scanHistory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()

    var activeResult by remember { mutableStateOf<UrlAnalysisResult?>(null) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(
        BottomNavItem("home", "Trang chủ", Icons.Filled.Shield, Icons.Outlined.Shield),
        BottomNavItem("scan_qr", "Quét QR", Icons.Filled.QrCodeScanner, Icons.Outlined.QrCodeScanner),
        BottomNavItem("history", "Lịch sử", Icons.Filled.History, Icons.Outlined.History),
        BottomNavItem("settings", "Cài đặt", Icons.Filled.Settings, Icons.Outlined.Settings)
    )

    val shouldShowBottomBar = currentRoute in listOf("home", "history", "settings")

    // Chức năng nạp và mở xem lại kết quả từ bản ghi lịch sử
    val handleSelectHistoryItem: (ScanHistoryEntity) -> Unit = { entity ->
        val reasons = mutableListOf<ScanReason>()
        try {
            val arr = JSONArray(entity.reasonsJson)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                reasons.add(
                    ScanReason(
                        code = obj.getString("code"),
                        title = obj.getString("title"),
                        message = obj.getString("message"),
                        severity = obj.getString("severity"),
                        source = obj.getString("source")
                    )
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val res = UrlAnalysisResult(
            scanId = entity.id,
            rawUrl = entity.rawUrl,
            displayUrl = entity.displayUrl,
            host = entity.host,
            score = entity.score,
            probability = entity.score / 10.0f,
            riskLevel = RiskLevel.valueOf(entity.riskLevel),
            reasons = reasons,
            modelVersion = entity.modelVersion,
            timestamp = entity.timestamp,
            isOffline = true
        )
        activeResult = res
        viewModel.enrichFromHistory(res)
        navController.navigate("result")
    }

    // Tự động chuyển màn hình khi có kết quả phân tích
    LaunchedEffect(analysisState) {
        when (val state = analysisState) {
            is AnalysisState.OfflineReady -> {
                activeResult = state.result
                navController.navigate("result") {
                    popUpTo("home") { inclusive = false }
                }
            }
            is AnalysisState.Complete -> {
                activeResult = state.result
                if (navController.currentDestination?.route != "result") {
                    navController.navigate("result") {
                        popUpTo("home") { inclusive = false }
                    }
                }
            }
            is AnalysisState.Error -> {
                navController.popBackStack("home", inclusive = false)
            }
            else -> {}
        }
    }

    Scaffold(
        bottomBar = {
            if (shouldShowBottomBar) {
                NavigationBar(
                    containerColor = SurfaceDark,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { item ->
                        val isSelected = currentRoute == item.route
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = {
                                if (currentRoute != item.route) {
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = {
                                Text(
                                    text = item.title,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = PrimaryCyan,
                                selectedTextColor = PrimaryCyan,
                                unselectedIconColor = TextMuted,
                                unselectedTextColor = TextMuted,
                                indicatorColor = PrimaryCyan.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    recentScans = historyList.take(3),
                    onNavigateToScanQr = { navController.navigate("scan_qr") },
                    onNavigateToHistory = { navController.navigate("history") },
                    onNavigateToSettings = { navController.navigate("settings") },
                    onAnalyzeUrl = { url -> viewModel.analyzeUrl(url) },
                    onSelectHistoryItem = handleSelectHistoryItem
                )
            }

            composable("scan_qr") {
                ScanQrScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onQrCodeDetected = { qrText ->
                        viewModel.analyzeUrl(qrText)
                    }
                )
            }

            composable("result") {
                activeResult?.let { res ->
                    ResultScreen(
                        result = res,
                        enrichmentState = enrichmentState,
                        onNavigateBack = {
                            viewModel.resetState()
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        },
                        onReAnalyze = {
                            viewModel.analyzeUrl(res.rawUrl)
                        }
                    )
                } ?: run {
                    navController.popBackStack("home", inclusive = false)
                }
            }

            composable("history") {
                HistoryScreen(
                    historyList = historyList,
                    searchQuery = searchQuery,
                    onSearchQueryChanged = { viewModel.onSearchQueryChanged(it) },
                    onDeleteSingle = { id -> viewModel.deleteHistoryItem(id) },
                    onClearAll = { viewModel.clearAllHistory() },
                    onNavigateBack = { navController.popBackStack() },
                    onSelectHistoryItem = handleSelectHistoryItem
                )
            }

            composable("settings") {
                SettingsScreen(
                    securityHelper = viewModel.securityHelper,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}

