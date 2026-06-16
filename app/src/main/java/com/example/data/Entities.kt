package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val fullName: String,
    val phone: String,
    val observations: String,
    val registrationDate: Long = System.currentTimeMillis(),
    val isPermanent: Boolean = true
)

@Entity(tableName = "services")
data class Service(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val price: Double,
    val durationMinutes: Int,
    val description: String,
    val isActive: Boolean = true
)

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val serviceId: Int,
    val dateTimestamp: Long, // Start time of appointment
    val observations: String,
    val status: String, // Pending, Confirmed, Completed, Cancelled
    val isPaid: Boolean = false,
    val paymentMethod: String = ""
)

@Entity(tableName = "wallets")
data class Wallet(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val alias: String,
    val cvu: String,
    val titular: String,
    val qrImagePath: String
)
