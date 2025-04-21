package com.example.queuemanagement.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.queuemanagement.AppointmentDao
import com.example.queuemanagement.CustomerDao


@Database(entities = [Customer::class, Appointment::class], version = 1)
abstract class AppDatabase : RoomDatabase() {

    abstract fun customerDao(): CustomerDao
    abstract fun appointmentDao(): AppointmentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "queue_management_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

