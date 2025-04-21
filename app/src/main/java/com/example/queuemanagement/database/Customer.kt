package com.example.queuemanagement.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0, // Primary key will auto-generate
    val name: String,
    val phoneNumber: String,
)
