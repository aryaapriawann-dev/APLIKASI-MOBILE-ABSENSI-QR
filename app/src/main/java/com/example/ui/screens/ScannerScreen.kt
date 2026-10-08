package com.example.ui.screens

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.QrPayload
import com.example.data.repository.AttendanceRepository
import com.example.data.repository.AttendanceResult
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.PrimaryBlue
import com.example.ui.theme.RoseError
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerScreen(
    isCheckIn: Boolean,
    repository: AttendanceRepository,
    onBack: () -> Unit
) {
    var isFlashOn by remember { mutableStateOf(false) }
    var scanResult by remember { mutableStateOf<AttendanceResult?>(null) }
    var showResultSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val transition = rememberInfiniteTransition(label = "Laser")
    val laserY by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserPosition"
    )

    fun handleScanPayload(payload: QrPayload) {
        val res = repository.processScan(payload, isCheckIn)
        scanResult = res
        showResultSheet = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isCheckIn) "Scan Presensi Masuk" else "Scan Presensi Pulang",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("btn_scanner_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { isFlashOn = !isFlashOn }, modifier = Modifier.testTag("btn_flash")) {
                        Icon(
                            imageVector = if (isFlashOn) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash",
                            tint = if (isFlashOn) Color.Yellow else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.85f)
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            // Viewfinder and Camera Simulator View
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // Dimmed camera overlay canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val boxSize = 260.dp.toPx()
                    val left = (size.width - boxSize) / 2
                    val top = (size.height - boxSize) / 2.3f

                    // Semi-transparent darkened background
                    drawRect(
                        color = Color(0xAA000000),
                        size = size
                    )

                    // Clear cutout in the middle for scanner
                    drawRect(
                        color = Color(0x11FFFFFF),
                        topLeft = Offset(left, top),
                        size = Size(boxSize, boxSize)
                    )

                    // 4 Corner Brackets
                    val strokeWidth = 5.dp.toPx()
                    val cornerLen = 30.dp.toPx()
                    val cornerColor = Color(0xFF38BDF8)

                    // Top-Left
                    val pathTL = Path().apply {
                        moveTo(left, top + cornerLen)
                        lineTo(left, top)
                        lineTo(left + cornerLen, top)
                    }
                    drawPath(pathTL, cornerColor, style = Stroke(strokeWidth))

                    // Top-Right
                    val pathTR = Path().apply {
                        moveTo(left + boxSize - cornerLen, top)
                        lineTo(left + boxSize, top)
                        lineTo(left + boxSize, top + cornerLen)
                    }
                    drawPath(pathTR, cornerColor, style = Stroke(strokeWidth))

                    // Bottom-Left
                    val pathBL = Path().apply {
                        moveTo(left, top + boxSize - cornerLen)
                        lineTo(left, top + boxSize)
                        lineTo(left + cornerLen, top + boxSize)
                    }
                    drawPath(pathBL, cornerColor, style = Stroke(strokeWidth))

                    // Bottom-Right
                    val pathBR = Path().apply {
                        moveTo(left + boxSize - cornerLen, top + boxSize)
                        lineTo(left + boxSize, top + boxSize)
                        lineTo(left + boxSize, top + boxSize - cornerLen)
                    }
                    drawPath(pathBR, cornerColor, style = Stroke(strokeWidth))

                    // Laser scan line
                    val laserCurrentY = top + (boxSize * laserY)
                    drawLine(
                        color = Color(0xFF38BDF8),
                        start = Offset(left + 8.dp.toPx(), laserCurrentY),
                        end = Offset(left + boxSize - 8.dp.toPx(), laserCurrentY),
                        strokeWidth = 3.dp.toPx()
                    )
                }

                // Subtitle Instruction
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 140.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Arahkan kamera ke QR Code di layar kantor",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Pastikan Anda berada di area kantor (${repository.defaultOffice.name})",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Floating Simulator Actions
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                color = Color(0xFF1E293B)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Pengujian Emulator (Simulasi Payload QR)",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                // Valid QR
                                val payload = QrPayload(
                                    officeId = repository.defaultOffice.id,
                                    officeName = repository.defaultOffice.name,
                                    latitude = repository.defaultOffice.latitude,
                                    longitude = repository.defaultOffice.longitude,
                                    validUntilTimestamp = System.currentTimeMillis() + 60000,
                                    token = UUID.randomUUID().toString()
                                )
                                handleScanPayload(payload)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sim_valid_qr"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("QR Valid", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                // Expired QR
                                val payload = QrPayload(
                                    officeId = repository.defaultOffice.id,
                                    officeName = repository.defaultOffice.name,
                                    latitude = repository.defaultOffice.latitude,
                                    longitude = repository.defaultOffice.longitude,
                                    validUntilTimestamp = System.currentTimeMillis() - 10000, // Expired
                                    token = UUID.randomUUID().toString()
                                )
                                handleScanPayload(payload)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sim_expired_qr"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("QR Kadaluarsa", fontSize = 11.sp, color = Color.White)
                        }

                        OutlinedButton(
                            onClick = {
                                // Invalid Office QR
                                val payload = QrPayload(
                                    officeId = "OFC-BDG-99",
                                    officeName = "Cabang Bandung",
                                    latitude = -6.9175,
                                    longitude = 107.6191,
                                    validUntilTimestamp = System.currentTimeMillis() + 60000,
                                    token = UUID.randomUUID().toString()
                                )
                                handleScanPayload(payload)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("btn_sim_wrong_office_qr"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Kantor Lain", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
            }

            // Bottom Sheet Result Modal
            if (showResultSheet && scanResult != null) {
                ModalBottomSheet(
                    onDismissRequest = {
                        showResultSheet = false
                        if (scanResult is AttendanceResult.Success) {
                            onBack()
                        }
                    },
                    sheetState = sheetState,
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        when (val res = scanResult!!) {
                            is AttendanceResult.Success -> {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSuccess.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = "Sukses",
                                        tint = EmeraldSuccess,
                                        modifier = Modifier.size(46.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Presensi Berhasil!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = res.message,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                )

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Tipe Aksi:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(res.type, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Waktu Catat:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                if (res.type == "Check-in") res.record.formattedCheckIn else res.record.formattedCheckOut,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Status Kehadiran:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text(
                                                text = if (res.record.status == AttendanceStatus.ON_TIME) "Tepat Waktu" else "Terlambat",
                                                fontWeight = FontWeight.Bold,
                                                color = if (res.record.status == AttendanceStatus.ON_TIME) EmeraldSuccess else RoseError,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text("Jarak ke Titik QR:", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Text("${res.record.checkInDistanceMeters.toInt()} meter (Valid)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(20.dp))

                                Button(
                                    onClick = {
                                        showResultSheet = false
                                        onBack()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("btn_close_success"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                ) {
                                    Text("Selesai & Kembali ke Beranda", fontWeight = FontWeight.Bold)
                                }
                            }

                            is AttendanceResult.Error -> {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(RoseError.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Error,
                                        contentDescription = "Gagal",
                                        tint = RoseError,
                                        modifier = Modifier.size(46.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "Presensi Ditolak",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )

                                Text(
                                    text = res.reason,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                )

                                if (res.currentDistance != null && res.maxRadius != null) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(14.dp),
                                        colors = CardDefaults.cardColors(containerColor = RoseError.copy(alpha = 0.08f))
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(14.dp)
                                        ) {
                                            Text(
                                                text = "Detail Validasi Geofencing:",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = RoseError
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text("Jarak Terdeteksi: ${res.currentDistance.toInt()} meter", fontSize = 12.sp)
                                            Text("Maksimal Diizinkan: ${res.maxRadius.toInt()} meter", fontSize = 12.sp)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            repository.toggleSimulatedLocation(true)
                                            showResultSheet = false
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_fix_location"),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Set Posisi di Kantor", fontSize = 12.sp)
                                    }

                                    Button(
                                        onClick = { showResultSheet = false },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("btn_retry_scan"),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = RoseError)
                                    ) {
                                        Text("Coba Lagi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
