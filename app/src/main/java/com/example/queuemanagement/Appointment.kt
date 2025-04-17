package com.example.queuemanagement

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointments")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val appointmentDate: String,
    val startTime: String,
    val endTime: String
)
