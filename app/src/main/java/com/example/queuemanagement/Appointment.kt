package com.example.queuemanagement

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "appointment_table")
data class Appointment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val appointmentDate: String
)
