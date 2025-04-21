package com.example.queuemanagement


import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.queuemanagement.database.AppointmentDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

import android.util.Log

class AppointmentFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppointmentAdapter
    private lateinit var datePickerButton: Button
    private lateinit var db: AppointmentDatabase
    private var selectedDate: String = getTodayDate()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_appointments, container, false)

        recyclerView = view.findViewById(R.id.appointmentRecyclerView)
        datePickerButton = view.findViewById(R.id.datePickerButton)
//        db = AppDatabase.getInstance(requireContext())
        db = AppointmentDatabase.getDatabase(requireContext())

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        val timeSlots = AppointmentAdapter.generateTimeSlots() // Assuming this is a static method in your adapter

        datePickerButton.text = selectedDate

        adapter = AppointmentAdapter(
            timeSlots,
            emptyList(),
            { selectedDate }, // Lambda that returns the selected date
            { loadAppointments(selectedDate) } // Reload appointments when changed
        )
        recyclerView.adapter = adapter

        loadAppointments(selectedDate)

        datePickerButton.setOnClickListener {
            showDatePicker()
        }

        return view
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()

        val dialog = DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = formatDate(selectedDay, selectedMonth, selectedYear)
                datePickerButton.text = selectedDate
                loadAppointments(selectedDate)
        },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH))

        dialog.show()
    }

    private fun loadAppointments(date: String) {
        Log.d("AppointmentFragment", "Loading appointments for date: $date") // Log when loading appointments
        GlobalScope.launch(Dispatchers.IO) {
            val appointments = db.appointmentDao().getAppointmentsByDate(date.trim())
            Log.d("AppointmentFragment", "Appointments before reload: $appointments")
            val allAppointments = db.appointmentDao().getAllAppointments()
            Log.d("AppointmentFragment", "All Appointments in DB: $allAppointments")
            // Switch to the main thread to update UI
            withContext(Dispatchers.Main) {
                // Only update the UI if the fragment is still attached to the activity
                if (isAdded) {
                    Log.d("AppointmentFragment", "Appointments loaded: ${appointments.size}") // Log the number of appointments
                    adapter.updateAppointments(appointments)
                    val allAppointments = db.appointmentDao().getAllAppointments()
                    Log.d("AppointmentFragment", "All Appointments in DB: $allAppointments")
                }
            }
        }
    }
}
