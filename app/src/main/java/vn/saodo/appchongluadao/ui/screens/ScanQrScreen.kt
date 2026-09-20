package vn.saodo.appchongluadao.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import vn.saodo.appchongluadao.ui.theme.*
import java.util.concurrent.Executors

@Composable
fun ScanQrScreen(
    onNavigateBack: () -> Unit,
    onQrCodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted ->
            hasCameraPermission = granted
        }
    )

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var isFlashOn by remember { mutableStateOf(false) }
    var cameraControl: CameraControl? by remember { mutableStateOf(null) }
    var hasScanned by remember { mutableStateOf(false) }
    var nonUrlWarning by remember { mutableStateOf<String?>(null) }
    var isProcessingGalleryImage by remember { mutableStateOf(false) }

    // Bộ chọn ảnh từ thư viện thiết bị (Gallery Image Picker)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            isProcessingGalleryImage = true
            try {
                val inputImage = InputImage.fromFilePath(context, uri)
                val scanner = BarcodeScanning.getClient()
                scanner.process(inputImage)
                    .addOnSuccessListener { barcodes ->
                        isProcessingGalleryImage = false
                        var found = false
                        for (barcode in barcodes) {
                            val rawVal = barcode.rawValue
                            if (!rawVal.isNullOrBlank()) {
                                if (barcode.valueType == Barcode.TYPE_URL ||
                                    rawVal.startsWith("http://", ignoreCase = true) ||
                                    rawVal.startsWith("https://", ignoreCase = true)
                                ) {
                                    found = true
                                    hasScanned = true
                                    onQrCodeDetected(rawVal)
                                    break
                                } else if (barcode.valueType == Barcode.TYPE_WIFI) {
                                    nonUrlWarning = "Mã QR chứa cấu hình WiFi, không phải URL trang web"
                                } else {
                                    nonUrlWarning = "Mã QR trong ảnh chứa văn bản hoặc dữ liệu không phải URL"
                                }
                            }
                        }
                        if (!found && nonUrlWarning == null) {
                            Toast.makeText(context, "Không tìm thấy mã QR nào trong ảnh được chọn!", Toast.LENGTH_LONG).show()
                        }
                    }
                    .addOnFailureListener {
                        isProcessingGalleryImage = false
                        Toast.makeText(context, "Không thể giải mã hình ảnh này!", Toast.LENGTH_SHORT).show()
                    }
            } catch (e: Exception) {
                isProcessingGalleryImage = false
                Toast.makeText(context, "Lỗi khi đọc tệp ảnh!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        containerColor = Color.Black
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (hasCameraPermission) {
                // CameraX Preview
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val barcodeScanner = BarcodeScanning.getClient()
                            val cameraExecutor = Executors.newSingleThreadExecutor()

                            val imageAnalysis = ImageAnalysis.Builder()
                                .setTargetResolution(Size(1280, 720))
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()

                            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                @OptIn(ExperimentalGetImage::class)
                                val mediaImage = imageProxy.image
                                if (mediaImage != null && !hasScanned) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )

                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            for (barcode in barcodes) {
                                                val rawVal = barcode.rawValue
                                                if (!rawVal.isNullOrBlank() && !hasScanned) {
                                                    // Kiểm tra loại mã QR: Chỉ chấp nhận URL HTTP/HTTPS
                                                    if (barcode.valueType == Barcode.TYPE_URL ||
                                                        rawVal.startsWith("http://", ignoreCase = true) ||
                                                        rawVal.startsWith("https://", ignoreCase = true)
                                                    ) {
                                                        hasScanned = true
                                                        onQrCodeDetected(rawVal)
                                                        break
                                                    } else if (barcode.valueType == Barcode.TYPE_WIFI) {
                                                        nonUrlWarning = "Mã QR chứa cấu hình WiFi, không phải URL trang web"
                                                    } else {
                                                        nonUrlWarning = "Mã QR chứa văn bản hoặc dữ liệu không phải URL"
                                                    }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                            try {
                                cameraProvider.unbindAll()
                                val camera = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalysis
                                )
                                cameraControl = camera.cameraControl
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Khung ngắm QR Code Overlay
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .border(3.dp, PrimaryCyan, RoundedCornerShape(20.dp))
                    )
                }

                // Cảnh báo nếu quét phải QR không phải URL
                nonUrlWarning?.let { warning ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 100.dp, start = 24.dp, end = 24.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(SurfaceDark.copy(alpha = 0.9f))
                            .border(1.dp, RiskMediumAmber, RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            warning,
                            color = RiskMediumAmber,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Nút quay lại, Thư viện ảnh và Đèn Flash
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(SurfaceDark.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại", tint = TextPrimary)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Nút chọn ảnh từ thư viện
                        IconButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SurfaceDark.copy(alpha = 0.8f))
                        ) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = "Chọn ảnh từ thư viện",
                                tint = PrimaryCyan
                            )
                        }

                        // Nút đèn Flash
                        IconButton(
                            onClick = {
                                isFlashOn = !isFlashOn
                                cameraControl?.enableTorch(isFlashOn)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(SurfaceDark.copy(alpha = 0.8f))
                        ) {
                            Icon(
                                if (isFlashOn) Icons.Default.FlashOff else Icons.Default.FlashOn,
                                contentDescription = "Flash",
                                tint = if (isFlashOn) PrimaryCyan else TextPrimary
                            )
                        }
                    }
                }

                // Hướng dẫn người dùng
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 86.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        "Căn mã QR vào giữa khung hình để phân tích",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Nút nổi chọn ảnh từ thư viện ở cạnh dưới
                Button(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 36.dp)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SurfaceDark.copy(alpha = 0.9f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryCyan.copy(alpha = 0.6f))
                ) {
                    Icon(
                        Icons.Default.PhotoLibrary,
                        contentDescription = null,
                        tint = PrimaryCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Chọn ảnh QR từ thư viện",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                // Đang xử lý ảnh từ thư viện
                if (isProcessingGalleryImage) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SurfaceDark,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = PrimaryCyan)
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    "Đang nhận diện mã QR trong ảnh...",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

            } else {
                // Khi chưa có quyền Camera
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = SurfaceDark,
                        modifier = Modifier.size(72.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                tint = PrimaryCyan,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Quét Mã QR Linh Hoạt",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Bạn có thể cấp quyền Camera để quét trực tiếp hoặc chọn ảnh mã QR có sẵn từ thư viện ảnh của bạn.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
                    ) {
                        Text("Cấp Quyền Camera", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCyan)
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Chọn Ảnh Từ Thư Viện", fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = onNavigateBack) {
                        Text("Quay lại Trang chủ", color = TextSecondary)
                    }
                }
            }
        }
    }
}
