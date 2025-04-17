package com.example.queuemanagement

import androidx.room.*

@Dao
interface AppointmentDao {
    @Insert
    suspend fun insert(appointment: Appointment)

    @Delete
    suspend fun delete(appointment: Appointment)

    @Query("SELECT * FROM appointments WHERE appointmentDate = :date")
    suspend fun getAppointmentsByDate(date: String): List<Appointment>
}
