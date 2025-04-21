package com.example.queuemanagement

import androidx.room.*
import com.example.queuemanagement.database.Appointment

@Dao
interface AppointmentDao {
    @Insert
    suspend fun insert(appointment: Appointment)

    @Delete
    suspend fun delete(appointment: Appointment)

    @Query("DELETE FROM appointments WHERE startTime = :startTime AND appointmentDate = :date")
    suspend fun deleteByTime(date: String, startTime: String)

    @Query("SELECT * FROM appointments WHERE appointmentDate = :date")
    suspend fun getAppointmentsByDate(date: String): List<Appointment>

    @Query("SELECT * FROM appointments")
    suspend fun getAllAppointments(): List<Appointment>
}
