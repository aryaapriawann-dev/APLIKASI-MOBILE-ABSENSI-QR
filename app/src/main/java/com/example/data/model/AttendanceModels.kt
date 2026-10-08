package com.example.data.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

data class User(
    val id: String,
    val name: String,
    val role: String,
    val department: String,
    val employeeNumber: String,
    val avatarUrl: String = ""
)

data class OfficeLocation(
    val id: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double
)

enum class AttendanceStatus {
    ON_TIME,   // Tepat Waktu
    LATE,      // Terlambat
    ABSENT     // Tidak Hadir
}

data class AttendanceRecord(
    val id: String,
    val date: LocalDate,
    val checkInTime: LocalTime?,
    val checkOutTime: LocalTime?,
    val officeName: String,
    val status: AttendanceStatus,
    val checkInDistanceMeters: Double,
    val checkOutDistanceMeters: Double? = null,
    val notes: String = ""
) {
    val formattedDate: String
        get() = date.format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"))

    val formattedCheckIn: String
        get() = checkInTime?.format(DateTimeFormatter.ofPattern("HH:mm:ss")) ?: "--:--:--"

    val formattedCheckOut: String
        get() = checkOutTime?.format(DateTimeFormatter.ofPattern("HH:mm:ss")) ?: "--:--:--"
}

data class QrPayload(
    val officeId: String,
    val officeName: String,
    val latitude: Double,
    val longitude: Double,
    val validUntilTimestamp: Long,
    val token: String
)
