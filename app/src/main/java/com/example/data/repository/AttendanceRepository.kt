package com.example.data.repository

import com.example.data.model.AttendanceRecord
import com.example.data.model.AttendanceStatus
import com.example.data.model.OfficeLocation
import com.example.data.model.QrPayload
import com.example.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

sealed class AttendanceResult {
    data class Success(
        val record: AttendanceRecord,
        val type: String, // "Check-in" or "Check-out"
        val message: String
    ) : AttendanceResult()

    data class Error(
        val reason: String,
        val currentDistance: Double? = null,
        val maxRadius: Double? = null
    ) : AttendanceResult()
}

class AttendanceRepository {

    // Default Office: Sudirman Head Office
    val defaultOffice = OfficeLocation(
        id = "OFC-JKT-01",
        name = "Kantor Pusat Sudirman",
        address = "Jl. Jend. Sudirman Kav 52-53, Jakarta Selatan",
        latitude = -6.2088,
        longitude = 106.8456,
        radiusMeters = 100.0
    )

    // Current Simulated User Location (Starts at 35m from office - INSIDE radius)
    private val _userLatitude = MutableStateFlow(-6.2086)
    val userLatitude: StateFlow<Double> = _userLatitude.asStateFlow()

    private val _userLongitude = MutableStateFlow(106.8458)
    val userLongitude: StateFlow<Double> = _userLongitude.asStateFlow()

    private val _isUserInsideOffice = MutableStateFlow(true)
    val isUserInsideOffice: StateFlow<Boolean> = _isUserInsideOffice.asStateFlow()

    // Logged in User
    private val _currentUser = MutableStateFlow<User?>(
        User(
            id = "EMP-001",
            name = "Budi Pratama",
            role = "Senior Mobile Engineer",
            department = "Technology & Product",
            employeeNumber = "KRY-2023-8821"
        )
    )
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    // History Records
    private val _history = MutableStateFlow<List<AttendanceRecord>>(
        listOf(
            AttendanceRecord(
                id = "REC-101",
                date = LocalDate.now().minusDays(1),
                checkInTime = LocalTime.of(8, 25, 12),
                checkOutTime = LocalTime.of(17, 30, 45),
                officeName = "Kantor Pusat Sudirman",
                status = AttendanceStatus.ON_TIME,
                checkInDistanceMeters = 24.5,
                checkOutDistanceMeters = 18.2
            ),
            AttendanceRecord(
                id = "REC-102",
                date = LocalDate.now().minusDays(2),
                checkInTime = LocalTime.of(8, 55, 30),
                checkOutTime = LocalTime.of(17, 45, 0),
                officeName = "Kantor Pusat Sudirman",
                status = AttendanceStatus.ON_TIME,
                checkInDistanceMeters = 30.0,
                checkOutDistanceMeters = 42.1
            ),
            AttendanceRecord(
                id = "REC-103",
                date = LocalDate.now().minusDays(3),
                checkInTime = LocalTime.of(9, 15, 20),
                checkOutTime = LocalTime.of(18, 0, 10),
                officeName = "Kantor Pusat Sudirman",
                status = AttendanceStatus.LATE,
                checkInDistanceMeters = 15.8,
                checkOutDistanceMeters = 22.0,
                notes = "Macet di tol dalam kota"
            ),
            AttendanceRecord(
                id = "REC-104",
                date = LocalDate.now().minusDays(4),
                checkInTime = LocalTime.of(8, 20, 5),
                checkOutTime = LocalTime.of(17, 10, 0),
                officeName = "Kantor Pusat Sudirman",
                status = AttendanceStatus.ON_TIME,
                checkInDistanceMeters = 33.2,
                checkOutDistanceMeters = 29.5
            )
        )
    )
    val history: StateFlow<List<AttendanceRecord>> = _history.asStateFlow()

    // Today's Record
    private val _todayRecord = MutableStateFlow<AttendanceRecord?>(null)
    val todayRecord: StateFlow<AttendanceRecord?> = _todayRecord.asStateFlow()

    fun login(email: String, role: String = "Mobile Engineer") {
        val name = when {
            email.contains("siti", ignoreCase = true) -> "Siti Rahma"
            email.contains("ahmad", ignoreCase = true) -> "Ahmad Fauzi"
            else -> "Budi Pratama"
        }
        _currentUser.value = User(
            id = UUID.randomUUID().toString().take(8),
            name = name,
            role = role,
            department = "Engineering",
            employeeNumber = "KRY-${(1000..9999).random()}"
        )
    }

    fun logout() {
        _currentUser.value = null
        _todayRecord.value = null
    }

    // Toggle location simulation for emulator testing
    fun toggleSimulatedLocation(insideOffice: Boolean) {
        _isUserInsideOffice.value = insideOffice
        if (insideOffice) {
            // ~35m from office
            _userLatitude.value = defaultOffice.latitude + 0.0002
            _userLongitude.value = defaultOffice.longitude + 0.0002
        } else {
            // ~850m from office (Outside geofence)
            _userLatitude.value = defaultOffice.latitude + 0.0075
            _userLongitude.value = defaultOffice.longitude + 0.0060
        }
    }

    fun calculateDistanceToOffice(): Double {
        return calculateHaversine(
            _userLatitude.value,
            _userLongitude.value,
            defaultOffice.latitude,
            defaultOffice.longitude
        )
    }

    // Process scanned QR payload
    fun processScan(payload: QrPayload, isCheckIn: Boolean): AttendanceResult {
        // 1. Validate Office
        if (payload.officeId != defaultOffice.id) {
            return AttendanceResult.Error(
                reason = "QR Code bukan milik kantor '${defaultOffice.name}' (Office ID tidak cocok)."
            )
        }

        // 2. Validate QR Expiration (allow within validity window or 24hr for demo)
        val now = System.currentTimeMillis()
        if (payload.validUntilTimestamp < now) {
            return AttendanceResult.Error(
                reason = "QR Code sudah kadaluarsa. Silakan refresh QR Code di layar kantor."
            )
        }

        // 3. Validate Geofence (Haversine)
        val distance = calculateHaversine(
            _userLatitude.value,
            _userLongitude.value,
            payload.latitude,
            payload.longitude
        )

        if (distance > defaultOffice.radiusMeters) {
            return AttendanceResult.Error(
                reason = "Posisi Anda berada di luar radius kantor! Anda harus berada dalam radius ${defaultOffice.radiusMeters.toInt()}m dari titik absensi.",
                currentDistance = distance,
                maxRadius = defaultOffice.radiusMeters
            )
        }

        // 4. Process Attendance
        val currentTime = LocalTime.now()
        val today = LocalDate.now()
        val currentRecord = _todayRecord.value

        if (isCheckIn) {
            if (currentRecord?.checkInTime != null) {
                return AttendanceResult.Error(reason = "Anda sudah melakukan Check-in hari ini pada ${currentRecord.formattedCheckIn}!")
            }

            // Standard shift start is 09:00:00
            val isLate = currentTime.isAfter(LocalTime.of(9, 0, 0))
            val status = if (isLate) AttendanceStatus.LATE else AttendanceStatus.ON_TIME

            val newRecord = AttendanceRecord(
                id = "REC-${System.currentTimeMillis().toString().takeLast(6)}",
                date = today,
                checkInTime = currentTime,
                checkOutTime = null,
                officeName = defaultOffice.name,
                status = status,
                checkInDistanceMeters = distance,
                notes = if (isLate) "Terlambat (melebihi jam 09:00)" else "Tepat Waktu"
            )

            _todayRecord.value = newRecord
            _history.value = listOf(newRecord) + _history.value

            return AttendanceResult.Success(
                record = newRecord,
                type = "Check-in",
                message = "Check-in berhasil! ${if (isLate) "Status: Terlambat." else "Status: Tepat Waktu."}"
            )
        } else {
            // Check-Out
            if (currentRecord == null || currentRecord.checkInTime == null) {
                return AttendanceResult.Error(reason = "Anda belum melakukan Check-in hari ini! Lakukan check-in terlebih dahulu.")
            }
            if (currentRecord.checkOutTime != null) {
                return AttendanceResult.Error(reason = "Anda sudah melakukan Check-out hari ini pada ${currentRecord.formattedCheckOut}!")
            }

            val updatedRecord = currentRecord.copy(
                checkOutTime = currentTime,
                checkOutDistanceMeters = distance
            )

            _todayRecord.value = updatedRecord
            _history.value = _history.value.map { if (it.id == updatedRecord.id) updatedRecord else it }

            return AttendanceResult.Success(
                record = updatedRecord,
                type = "Check-out",
                message = "Check-out berhasil dicatat pada ${currentTime.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"))}. Selamat beristirahat!"
            )
        }
    }

    private fun calculateHaversine(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val r = 6371000.0 // Earth radius in meters
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }
}
