package com.example.queuemanagement.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.queuemanagement.AppointmentDao

@Database(entities = [Appointment::class], version = 3)
abstract class AppointmentDatabase : RoomDatabase() {
    abstract fun appointmentDao(): AppointmentDao

    companion object {
        @Volatile private var INSTANCE: AppointmentDatabase? = null

        fun getDatabase(context: Context): AppointmentDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppointmentDatabase::class.java,
                    "appointment_database"
                ).build()
                // ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
