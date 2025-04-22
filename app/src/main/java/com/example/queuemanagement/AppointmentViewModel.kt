package com.example.queuemanagement

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.queuemanagement.database.Appointment
import com.example.queuemanagement.database.AppointmentDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class AppointmentViewModel(private val database: AppointmentDatabase) : ViewModel() {

    private val _availableSlots = MutableLiveData<List<LocalTime>>()
    val availableSlots: LiveData<List<LocalTime>> = _availableSlots

    private val _appointmentsForDate = MutableLiveData<List<Appointment>>()
    val appointmentsForDate: LiveData<List<Appointment>> = _appointmentsForDate

    fun loadAppointmentsForDate(date: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val appointments = database.appointmentDao().getAppointmentsByDate(date)
            _appointmentsForDate.postValue(appointments)
        }
    }

    fun loadAvailableSlots(date: String, selectedDurationMinutes: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val existingAppointments = database.appointmentDao().getAppointmentsByDate(date)
            val formatter = DateTimeFormatter.ofPattern("HH:mm")

            val takenSlots = existingAppointments.map {
                val start = LocalTime.parse(it.startTime, formatter)
                val end = LocalTime.parse(it.endTime, formatter)
                start to end
            }

            val fullDaySlots = mutableListOf<LocalTime>()
            var current = LocalTime.of(7, 0)
            val endOfDay = LocalTime.of(19, 0)

            while (current.plusMinutes(selectedDurationMinutes.toLong()) <= endOfDay) {
                val next = current.plusMinutes(selectedDurationMinutes.toLong())
                val isOverlapping = takenSlots.any { (start, end) ->
                    current < end && next > start
                }
                if (!isOverlapping) {
                    fullDaySlots.add(current)
                }
                current = current.plusMinutes(15)
            }

            _availableSlots.postValue(fullDaySlots)
        }
    }
}
