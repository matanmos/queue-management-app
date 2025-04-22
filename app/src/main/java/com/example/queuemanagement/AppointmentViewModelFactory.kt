package com.example.queuemanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.queuemanagement.database.AppointmentDatabase

class AppointmentViewModelFactory(private val database: AppointmentDatabase) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AppointmentViewModel::class.java)) {
            return AppointmentViewModel(database) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
