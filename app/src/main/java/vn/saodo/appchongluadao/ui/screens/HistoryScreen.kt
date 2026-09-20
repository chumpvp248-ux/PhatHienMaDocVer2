package vn.saodo.appchongluadao.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.saodo.appchongluadao.data.local.ScanHistoryEntity
import vn.saodo.appchongluadao.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

enum class HistoryFilter {
    ALL, HIGH_RISK, MEDIUM_RISK, LOW_RISK
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    historyList: List<ScanHistoryEntity>,
    searchQuery: String,
    onSearchQueryChanged: (String) -> Unit,
    onDeleteSingle: (String) -> Unit,
    onClearAll: () -> Unit,
    onNavigateBack: () -> Unit,
    onSelectHistoryItem: (ScanHistoryEntity) -> Unit
) {
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf(HistoryFilter.ALL) }
    val dateFormat = remember { SimpleDateFormat("HH:mm • dd/MM/yyyy", Locale.getDefault()) }

    val filteredList = remember(historyList, searchQuery, selectedFilter) {
        historyList
            .filter {
                if (searchQuery.isBlank()) true
                else it.host.contains(searchQuery, ignoreCase = true) || it.rawUrl.contains(searchQuery, ignoreCase = true)
            }
            .filter {
                when (selectedFilter) {
                    HistoryFilter.ALL -> true
                    HistoryFilter.HIGH_RISK -> it.score >= 7
                    HistoryFilter.MEDIUM_RISK -> it.score in 4..6
                    HistoryFilter.LOW_RISK -> it.score in 1..3
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Lịch Sử Kiểm Tra (${historyList.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    if (historyList.isNotEmpty()) {
                        IconButton(onClick = { showClearConfirm = true }) {
                            Icon(
                                Icons.Default.DeleteSweep,
                                contentDescription = "Xóa toàn bộ",
                                tint = RiskHighRed
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BgDark)
            )
        },
        containerColor = BgDark
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Ô tìm kiếm hiện đại
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Tìm theo tên miền hoặc URL...", color = TextMuted) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Xóa tìm kiếm", tint = TextMuted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    focusedBorderColor = PrimaryCyan,
                    unfocusedBorderColor = BorderColor,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Filter chips dạng danh mục
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.ALL,
                        onClick = { selectedFilter = HistoryFilter.ALL },
                        label = { Text("Tất cả (${historyList.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryCyan.copy(alpha = 0.2f),
                            selectedLabelColor = PrimaryCyan
                        )
                    )
                }
                item {
                    val count = historyList.count { it.score >= 7 }
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.HIGH_RISK,
                        onClick = { selectedFilter = HistoryFilter.HIGH_RISK },
                        label = { Text("Nguy hiểm ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RiskHighRed.copy(alpha = 0.2f),
                            selectedLabelColor = RiskHighRed
                        )
                    )
                }
                item {
                    val count = historyList.count { it.score in 4..6 }
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.MEDIUM_RISK,
                        onClick = { selectedFilter = HistoryFilter.MEDIUM_RISK },
                        label = { Text("Thận trọng ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RiskMediumAmber.copy(alpha = 0.2f),
                            selectedLabelColor = RiskMediumAmber
                        )
                    )
                }
                item {
                    val count = historyList.count { it.score in 1..3 }
                    FilterChip(
                        selected = selectedFilter == HistoryFilter.LOW_RISK,
                        onClick = { selectedFilter = HistoryFilter.LOW_RISK },
                        label = { Text("An toàn ($count)") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = RiskLowGreen.copy(alpha = 0.2f),
                            selectedLabelColor = RiskLowGreen
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (filteredList.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(bottom = 60.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SurfaceDark,
                            modifier = Modifier.size(76.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Outlined.History,
                                    contentDescription = null,
                                    tint = TextMuted,
                                    modifier = Modifier.size(38.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            if (searchQuery.isNotBlank() || selectedFilter != HistoryFilter.ALL)
                                "Không tìm thấy kết quả nào"
                            else
                                "Chưa có bản ghi lịch sử nào",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            "Kết quả phân tích URL và mã QR sẽ tự động lưu trữ tại đây",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 20.dp)
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        val badgeColor = when {
                            item.score >= 7 -> RiskHighRed
                            item.score >= 4 -> RiskMediumAmber
                            else -> RiskLowGreen
                        }

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectHistoryItem(item) },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                            border = CardDefaults.outlinedCardBorder().copy(
                                brush = Brush.verticalGradient(listOf(CardBorderSubtle, Color.Transparent))
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Badge điểm số tròn
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(badgeColor.copy(alpha = 0.15f))
                                        .border(1.dp, badgeColor.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${item.score}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = badgeColor
                                    )
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.host,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.displayUrl,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dateFormat.format(Date(item.timestamp)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary,
                                        fontSize = 11.sp
                                    )
                                }

                                IconButton(
                                    onClick = { onDeleteSingle(item.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Xóa bản ghi",
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(4.dp))

                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowForwardIos,
                                    contentDescription = null,
                                    tint = TextMuted.copy(alpha = 0.5f),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Xóa Toàn Bộ Lịch Sử?", fontWeight = FontWeight.Bold) },
            text = { Text("Các bản ghi kiểm tra trên thiết bị này sẽ bị xóa hoàn toàn khỏi cơ sở dữ liệu.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onClearAll()
                        showClearConfirm = false
                    }
                ) {
                    Text("Xóa Tất Cả", color = RiskHighRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) {
                    Text("Hủy", color = TextSecondary)
                }
            }
        )
    }
}

