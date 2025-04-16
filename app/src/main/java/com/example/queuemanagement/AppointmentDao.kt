package com.example.queuemanagement

import androidx.room.*

@Dao
interface AppointmentDao {
    @Insert
    suspend fun insert(appointment: Appointment)

    @Delete
    suspend fun delete(appointment: Appointment)

    @Query("SELECT * FROM appointment_table")
    suspend fun getAll(): List<Appointment>
}
