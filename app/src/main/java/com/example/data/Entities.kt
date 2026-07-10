package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable

@Entity(tableName = "members")
data class Member(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val code: String,
    val phone: String,
    val instrument: String
) : Serializable

@Entity(tableName = "commitments")
data class Commitment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val date: String,
    val time: String,
    val location: String,
    val isCompleted: Boolean = false
) : Serializable

@Entity(tableName = "attendance")
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val commitmentId: Int,
    val memberId: Int,
    val status: String, // "Aceptado", "Pendiente", "Rechazado"
    val paymentAmount: Double,
    val isPaid: Boolean = false
) : Serializable
