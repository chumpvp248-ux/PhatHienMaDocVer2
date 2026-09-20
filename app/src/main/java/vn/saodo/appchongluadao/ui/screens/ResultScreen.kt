package vn.saodo.appchongluadao.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.saodo.appchongluadao.domain.model.*
import vn.saodo.appchongluadao.ui.theme.*
import vn.saodo.appchongluadao.ui.viewmodel.EnrichmentState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    result: UrlAnalysisResult,
    enrichmentState: EnrichmentState = EnrichmentState.Idle,
    onNavigateBack: () -> Unit,
    onReAnalyze: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    var showReportDialog by remember { mutableStateOf(false) }

    val enrichmentResult = (enrichmentState as? EnrichmentState.Success)?.data
    val isEnrichmentLoading = enrichmentState is EnrichmentState.Loading

    val bannerColor = when (result.riskLevel) {
        RiskLevel.LOW -> RiskLowGreen
        RiskLevel.MEDIUM -> RiskMediumAmber
        RiskLevel.HIGH -> RiskHighRed
    }

    val riskTitle = when (result.riskLevel) {
        RiskLevel.LOW -> "LIÊN KẾT AN TOÀN"
        RiskLevel.MEDIUM -> "CẦN THẬN TRỌNG"
        RiskLevel.HIGH -> "NGUY CƠ LỪA ĐẢO CAO"
    }

    val riskIcon = when (result.riskLevel) {
        RiskLevel.LOW -> Icons.Default.CheckCircle
        RiskLevel.MEDIUM -> Icons.Default.Warning
        RiskLevel.HIGH -> Icons.Default.Dangerous
    }

    val dateFormat = SimpleDateFormat("HH:mm:ss dd/MM/yyyy", Locale.getDefault())
    val formattedTime = dateFormat.format(Date(result.timestamp))

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Kết Quả Đánh Giá An Ninh",
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
                    IconButton(onClick = {
                        val sendIntent = android.content.Intent().apply {
                            action = android.content.Intent.ACTION_SEND
                            putExtra(android.content.Intent.EXTRA_TEXT, "Báo cáo an ninh cho link: ${result.rawUrl} - Điểm rủi ro: ${result.score}/10")
                            type = "text/plain"
                        }
                        context.startActivity(android.content.Intent.createChooser(sendIntent, "Chia sẻ kết quả"))
                    }) {
                        Icon(Icons.Default.Share, contentDescription = "Chia sẻ", tint = TextPrimary)
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
                .padding(horizontal = 18.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. BANNER CHÍNH SIÊU ĐẸP (Hero Score Gauge)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.verticalGradient(
                        listOf(bannerColor.copy(alpha = 0.8f), bannerColor.copy(alpha = 0.2f))
                    )
                )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    // Đồng hồ đo điểm số rủi ro hình tròn (Circular Score Gauge)
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(120.dp)
                    ) {
                        Canvas(modifier = Modifier.size(110.dp)) {
                            val strokeWidth = 8.dp.toPx()
                            // Vòng tròn nền mờ
                            drawCircle(
                                color = SurfaceVariantDark.copy(alpha = 0.5f),
                                radius = (size.minDimension - strokeWidth) / 2,
                                style = Stroke(strokeWidth)
                            )
                            // Cung tiến trình theo điểm số
                            val sweepAngle = (result.score / 10.0f) * 360f
                            drawArc(
                                brush = Brush.sweepGradient(
                                    listOf(bannerColor.copy(alpha = 0.6f), bannerColor, bannerColor)
                                ),
                                startAngle = -90f,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                style = Stroke(strokeWidth, cap = StrokeCap.Round)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${result.score}",
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                color = bannerColor
                            )
                            Text(
                                text = "TRÊN 10",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Huy hiệu trạng thái (Status Pill)
                    Surface(
                        color = bannerColor.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(20.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, bannerColor.copy(alpha = 0.5f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                riskIcon,
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = riskTitle,
                                color = bannerColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (result.score >= 7) "Khả năng rất cao là liên kết lừa đảo, giả mạo thương hiệu để chiếm đoạt tài khoản"
                        else if (result.score in 4..6) "Phát hiện một số dấu hiệu bất thường, cần thận trọng không cung cấp OTP/mật khẩu"
                        else "Cấu trúc URL an toàn, chưa ghi nhận dấu hiệu độc hại",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    // Huy hiệu tổng hợp đa nguồn (AI Decision Fusion Badge)
                    if (result.isFused) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Surface(
                            color = AccentTeal.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentTeal.copy(alpha = 0.5f))
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = AccentTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Đã tổng hợp đa nguồn (AI + WHOIS + Đối tác an ninh)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentTeal
                                    )
                                }
                                if (!result.fusionSummary.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = result.fusionSummary,
                                        fontSize = 11.sp,
                                        color = TextSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 1.5. HƯỚNG DẪN HÀNH ĐỘNG THỰC TẾ DÀNH CHO NGƯỜI DÙNG (Actionable Guidance)
            ActionableGuidanceCard(
                riskLevel = result.riskLevel,
                onOpenUrl = {
                    try {
                        uriHandler.openUri(result.rawUrl)
                    } catch (_: Exception) {}
                }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 2. THẺ CHI TIẾT URL VÀ MÁY CHỦ (Host & Security Card)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = androidx.compose.ui.graphics.SolidColor(vn.saodo.appchongluadao.ui.theme.BorderColor)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val isHttps = result.displayUrl.startsWith("https", ignoreCase = true)
                            Surface(
                                color = if (isHttps) RiskLowGreen.copy(alpha = 0.15f) else RiskHighRed.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        if (isHttps) Icons.Default.Lock else Icons.Default.LockOpen,
                                        contentDescription = null,
                                        tint = if (isHttps) RiskLowGreen else RiskHighRed,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isHttps) "HTTPS (Mã hóa)" else "HTTP (Không mã hóa)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isHttps) RiskLowGreen else RiskHighRed
                                    )
                                }
                            }
                        }

                        // Nút sao chép URL
                        IconButton(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("URL", result.rawUrl)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Đã sao chép liên kết!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                Icons.Default.ContentCopy,
                                contentDescription = "Sao chép",
                                tint = PrimaryCyan,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = result.host,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = result.displayUrl,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = TextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = vn.saodo.appchongluadao.ui.theme.BorderColor.copy(alpha = 0.4f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Mô hình: ${result.modelVersion}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 3. CĂN CỨ & BẰNG CHỨNG PHÂN TÍCH (XAI)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Psychology,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Căn Cứ & Bằng Chứng Phân Tích (XAI)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Giải thích minh bạch lý do đánh giá theo từng đặc trưng và tri thức bảo mật",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))

            if (result.reasons.isEmpty()) {
                Text(
                    "Không có cảnh báo đặc biệt nào được ghi nhận.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMuted
                )
            } else {
                result.reasons.forEach { reason ->
                    ReasonItemCard(reason)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // 4. KẾT QUẢ TỪ CHỐNG LỪA ĐẢO & BÊN THỨ BA (Image 2)
            ThirdPartyReputationCard(
                enrichment = enrichmentResult,
                isLoading = isEnrichmentLoading,
                result = result
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 5. THÔNG TIN BỔ SUNG (WHOIS / DNS / IP) (Image 1)
            AdditionalInfoCard(
                whois = enrichmentResult?.whois,
                isLoading = isEnrichmentLoading
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 6. TRA CỨU DANH SÁCH ĐEN & NGUỒN UY TÍN (Threat Intelligence 1-chạm)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.VerifiedUser,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Tra Cứu Trực Tiếp Nguồn Uy Tín",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "Đối soát liên kết 1-chạm với các cơ sở dữ liệu an ninh mạng hàng đầu",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(12.dp))

            result.threatIntelSources.forEach { source ->
                ThreatIntelSourceCard(
                    source = source,
                    onOpenLookup = { url ->
                        try {
                            uriHandler.openUri(url)
                        } catch (_: Exception) {}
                    }
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 7. CÁC NÚT HÀNH ĐỘNG CHÍNH (Bottom Actions)
            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
            ) {
                Icon(Icons.Default.Home, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Quay Lại Trang Chủ", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onReAnalyze,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCyan)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Phân Tích Lại Liên Kết", fontWeight = FontWeight.SemiBold)
            }

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = { showReportDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Báo Cáo Nhận Định Sai", color = TextMuted, fontSize = 13.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }

    if (showReportDialog) {
        AlertDialog(
            onDismissRequest = { showReportDialog = false },
            title = { Text("Báo Cáo Nhận Định Chưa Đúng", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Thông tin gửi gồm URL (${result.host}) và điểm số đánh giá (${result.score}/10) nhằm phục vụ tối ưu hóa thuật toán an toàn thông tin. Hệ thống cam kết không gửi bất kỳ dữ liệu cá nhân nào của bạn.",
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showReportDialog = false
                    Toast.makeText(context, "Cảm ơn bạn đã gửi phản hồi đóng góp cho cộng đồng!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Gửi Phản Hồi", color = PrimaryCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showReportDialog = false }) {
                    Text("Hủy", color = TextSecondary)
                }
            }
        )
    }
}

data class GuidanceInfo(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val items: List<String>
)

@Composable
fun ActionableGuidanceCard(
    riskLevel: RiskLevel,
    onOpenUrl: () -> Unit
) {
    val guidance = when (riskLevel) {
        RiskLevel.HIGH -> GuidanceInfo(
            title = "Hành Động Khẩn Cấp Cần Làm:",
            icon = Icons.Default.Dangerous,
            tint = RiskHighRed,
            items = listOf(
                "Tuyệt đối KHÔNG nhập mật khẩu, mã OTP hoặc số thẻ ngân hàng.",
                "Đóng ngay trang web này để phòng ngừa đánh cắp tài khoản.",
                "Nếu đã lỡ nhập thông tin, hãy liên hệ ngân hàng khóa thẻ và đổi mật khẩu ngay lập tức."
            )
        )
        RiskLevel.MEDIUM -> GuidanceInfo(
            title = "Lưu Ý Thận Trọng Khi Thao Tác:",
            icon = Icons.Default.Warning,
            tint = RiskMediumAmber,
            items = listOf(
                "Kiểm tra kỹ chính tả tên miền xem có giả mạo thương hiệu uy tín không.",
                "Tuyệt đối không tải xuống tệp tin hoặc cài đặt ứng dụng APK từ trang web này.",
                "Không thực hiện chuyển tiền nếu chưa xác thực nguồn gốc qua kênh chính thống."
            )
        )
        RiskLevel.LOW -> GuidanceInfo(
            title = "Hướng Dẫn Truy Cập An Toàn:",
            icon = Icons.Default.CheckCircle,
            tint = RiskLowGreen,
            items = listOf(
                "Cấu trúc URL an toàn, chưa ghi nhận dấu hiệu độc hại trong cơ sở dữ liệu.",
                "Bạn có thể yên tâm truy cập liên kết này trên trình duyệt.",
                "Luôn chú ý biểu tượng ổ khóa bảo mật HTTPS trước khi nhập dữ liệu cá nhân."
            )
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = guidance.tint.copy(alpha = 0.08f)),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(guidance.tint.copy(alpha = 0.35f))
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(guidance.icon, contentDescription = null, tint = guidance.tint, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = guidance.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = guidance.tint
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            guidance.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(guidance.tint)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary,
                        lineHeight = 18.sp
                    )
                }
            }

            if (riskLevel == RiskLevel.LOW) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onOpenUrl,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = guidance.tint),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Launch,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Mở Liên Kết Trong Trình Duyệt",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }
        }
    }
}


@Composable
fun AdditionalInfoCard(
    whois: DomainWhoisInfo?,
    isLoading: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(vn.saodo.appchongluadao.ui.theme.BorderColor)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Dns,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Thông tin bổ sung",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCyan
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoRow(icon = Icons.Default.Public, label = "Địa chỉ IP", value = whois?.ipAddress, isLoading = isLoading)
            HorizontalDivider(color = vn.saodo.appchongluadao.ui.theme.BorderColor.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 9.dp))

            InfoRow(icon = Icons.Default.Place, label = "IP có vị trí", value = whois?.ipLocation, isLoading = isLoading)
            HorizontalDivider(color = vn.saodo.appchongluadao.ui.theme.BorderColor.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 9.dp))

            InfoRow(icon = Icons.Default.Business, label = "Nhà cung cấp", value = whois?.isp, isLoading = isLoading)
            HorizontalDivider(color = vn.saodo.appchongluadao.ui.theme.BorderColor.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 9.dp))

            InfoRow(icon = Icons.Default.Apartment, label = "Nhà đăng ký", value = whois?.registrar, isLoading = isLoading)
            HorizontalDivider(color = vn.saodo.appchongluadao.ui.theme.BorderColor.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 9.dp))

            InfoRow(icon = Icons.Default.PersonOutline, label = "Chủ sở hữu", value = whois?.registrant, isLoading = isLoading)
            HorizontalDivider(color = vn.saodo.appchongluadao.ui.theme.BorderColor.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 9.dp))

            InfoRow(icon = Icons.Default.CalendarToday, label = "Ngày đăng ký", value = whois?.registrationDate, isLoading = isLoading)
            HorizontalDivider(color = vn.saodo.appchongluadao.ui.theme.BorderColor.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 9.dp))

            InfoRow(icon = Icons.Default.Event, label = "Ngày hết hạn", value = whois?.expirationDate, isLoading = isLoading)
            HorizontalDivider(color = vn.saodo.appchongluadao.ui.theme.BorderColor.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 9.dp))

            InfoRow(icon = Icons.Default.Autorenew, label = "Cập nhật", value = whois?.updatedDate, isLoading = isLoading)
            HorizontalDivider(color = vn.saodo.appchongluadao.ui.theme.BorderColor.copy(alpha = 0.35f), modifier = Modifier.padding(vertical = 9.dp))

            InfoRow(icon = Icons.AutoMirrored.Filled.List, label = "Nameservers", value = whois?.nameservers, isLoading = isLoading)
        }
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String?,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1.1f)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(17.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
        }

        Box(
            modifier = Modifier.weight(1.3f),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (isLoading || (value == null && isLoading)) {
                // Thanh placeholder xám bo góc (Skeleton)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariantDark.copy(alpha = 0.6f))
                )
            } else if (!value.isNullOrBlank()) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(18.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceVariantDark.copy(alpha = 0.25f))
                )
            }
        }
    }
}

@Composable
fun ThirdPartyReputationCard(
    enrichment: FullEnrichmentResult?,
    isLoading: Boolean,
    result: UrlAnalysisResult
) {
    val bannerText = when {
        enrichment != null -> enrichment.chongLuaDaoRiskTitle
        result.riskLevel == RiskLevel.HIGH -> "Có thể nguy hiểm"
        result.riskLevel == RiskLevel.MEDIUM -> "Cần thận trọng"
        else -> "An toàn"
    }

    val bannerColor = when {
        enrichment != null -> when (enrichment.chongLuaDaoRiskLevel) {
            RiskLevel.HIGH -> Color(0xFFEF4444)
            RiskLevel.MEDIUM -> RiskMediumAmber
            RiskLevel.LOW -> RiskLowGreen
        }
        result.riskLevel == RiskLevel.HIGH -> Color(0xFFEF4444)
        result.riskLevel == RiskLevel.MEDIUM -> RiskMediumAmber
        else -> RiskLowGreen
    }

    val thirdParties = enrichment?.thirdParties ?: listOf(
        ThirdPartyReputation("Scam Adviser", "Không tìm thấy", ThreatStatusType.NOT_FOUND),
        ThirdPartyReputation("Criminalip", "Không tìm thấy", ThreatStatusType.NOT_FOUND),
        ThirdPartyReputation("Hudson Rock", "Không tìm thấy", ThreatStatusType.NOT_FOUND),
        ThirdPartyReputation("Have I Been Pwned", "Không tìm thấy", ThreatStatusType.NOT_FOUND),
        ThirdPartyReputation("Phish Tank", "Không tìm thấy", ThreatStatusType.NOT_FOUND),
        ThirdPartyReputation("CyRadar", "Không tìm thấy", ThreatStatusType.NOT_FOUND),
        ThirdPartyReputation("ScamVN", "Không tìm thấy", ThreatStatusType.NOT_FOUND),
        ThirdPartyReputation("IP Quality Score", "Không tìm thấy", ThreatStatusType.NOT_FOUND),
        ThirdPartyReputation("APIVoid", "Không tìm thấy", ThreatStatusType.NOT_FOUND),
        ThirdPartyReputation("PhishDestroy", "Không tìm thấy", ThreatStatusType.NOT_FOUND)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(vn.saodo.appchongluadao.ui.theme.BorderColor)
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header @ Kết quả từ Chống Lừa Đảo
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "@",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryCyan
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Kết quả từ Chống Lừa Đảo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCyan
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Banner lớn (VD: Có thể nguy hiểm)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(bannerColor)
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = bannerText,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Tiêu đề con: Kết quả từ bên thứ ba
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Public,
                    contentDescription = null,
                    tint = PrimaryCyan,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    "Kết quả từ bên thứ ba",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryCyan
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 10 nguồn bên thứ ba
            thirdParties.forEach { source ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = source.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val badgeColor = when (source.statusType) {
                            ThreatStatusType.MALICIOUS -> RiskHighRed
                            ThreatStatusType.SUSPICIOUS -> RiskMediumAmber
                            ThreatStatusType.SAFE -> RiskLowGreen
                            ThreatStatusType.NOT_FOUND -> Color(0xFFD97706) // Golden amber
                        }

                        Surface(
                            shape = CircleShape,
                            color = badgeColor,
                            modifier = Modifier.size(18.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (source.statusType == ThreatStatusType.MALICIOUS) "✕" else "!",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = source.statusText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = badgeColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Ngày cập nhật: lúc HH:mm...
            Text(
                text = "Ngày cập nhật: ${enrichment?.updatedAtFormatted ?: ("lúc " + SimpleDateFormat("HH:mm a 'ngày' dd 'tháng' MM, yyyy", Locale("vi", "VN")).format(Date(result.timestamp)))}",
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun ReasonItemCard(reason: ScanReason) {
    val indicatorColor = when (reason.severity) {
        "critical", "high" -> RiskHighRed
        "medium" -> RiskMediumAmber
        else -> AccentTeal
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(vn.saodo.appchongluadao.ui.theme.BorderColor)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Thanh màu chỉ thị mức độ nghiêm trọng
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(indicatorColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        reason.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    val sourceLabel = when (reason.source) {
                        "whois" -> "WHOIS"
                        "threat_intel" -> "Đối tác"
                        "heuristic" -> "Quy tắc"
                        else -> "Mô hình AI"
                    }
                    Surface(
                        color = if (reason.source == "whois" || reason.source == "threat_intel") PrimaryCyan.copy(alpha = 0.15f) else SurfaceVariantDark,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            sourceLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (reason.source == "whois" || reason.source == "threat_intel") PrimaryCyan else TextMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    reason.message,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun ThreatIntelSourceCard(
    source: ThreatIntelSource,
    onOpenLookup: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceDark),
        border = CardDefaults.outlinedCardBorder().copy(
            brush = androidx.compose.ui.graphics.SolidColor(
                if (source.isMalicious) RiskHighRed.copy(alpha = 0.5f) else vn.saodo.appchongluadao.ui.theme.BorderColor
            )
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    source.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Surface(
                    color = if (source.isMalicious) RiskHighRed.copy(alpha = 0.2f)
                    else PrimaryCyan.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = source.statusText,
                        color = if (source.isMalicious) RiskHighRed else PrimaryCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                source.description,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )

            if (source.lookupUrl != null) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { onOpenLookup(source.lookupUrl) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCyan),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Launch,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Tra cứu trực tiếp trên ${source.name}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
