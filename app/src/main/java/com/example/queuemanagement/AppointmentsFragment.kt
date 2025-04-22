package com.example.queuemanagement


import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.telephony.SubscriptionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.queuemanagement.database.AppointmentDatabase
import java.util.*

import android.util.Log
import android.widget.Toast
import com.example.queuemanagement.database.Appointment
import com.example.queuemanagement.database.CustomerDatabase
import kotlinx.coroutines.*

class AppointmentFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppointmentAdapter
    private lateinit var datePickerButton: Button
    private lateinit var sendSmsButton: Button
    private lateinit var db: AppointmentDatabase
    private var selectedDate: String = getTodayDate()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_appointments, container, false)

        recyclerView = view.findViewById(R.id.appointmentRecyclerView)
        datePickerButton = view.findViewById(R.id.datePickerButton)
        db = AppointmentDatabase.getDatabase(requireContext())

        sendSmsButton = view.findViewById(R.id.sendSmsButton)

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

        sendSmsButton.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("אישור שליחת SMS")
                .setMessage("האם לשלוח הודעה לכל הלקוחות בתאריך $selectedDate?")
                .setPositiveButton("כן") { _, _ ->
                    sendSmsToAll(selectedDate, "שלום! תזכורת על תור בתאריך $selectedDate.")
                }
                .setNegativeButton("לא", null)
                .show()
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

    private fun sendSmsToAll(date: String, message: String) {
        val context = requireContext()
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppointmentDatabase.getDatabase(context)
            val appointments = db.appointmentDao().getAppointmentsByDate(date)

            val customerdb = CustomerDatabase.getDatabase(context)

            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // API 31+: SmsManager.getDefault() is deprecated but still works
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            var successCount = 0
            var failCount = 0

            for (appointment in appointments) {
                val customer = customerdb.customerDao().getCustomerByName(appointment.name)
//                val phone = normalizePhoneNumber(customer?.phoneNumber)
                val phone = customer?.phoneNumber?.let { normalizePhoneNumber(it) }


                if (!phone.isNullOrEmpty()) {
                    try {
                        smsManager.sendTextMessage(phone, null, message, null, null)
                        successCount++
                        Log.d("SMS", "Sending SMS to ${appointment.name} at $phone")
                    } catch (e: Exception) {
                        Log.e("SMS", "Failed to send SMS to ${appointment.name} at $phone", e)
                        failCount++
                    }
                } else {
                    Log.w("SMS", "No phone number for customer: ${appointment.name}")
                    failCount++
                }
            }

            withContext(Dispatchers.Main) {
                Toast.makeText(
                    context,
                    "נשלחו $successCount הודעות SMS. נכשלו $failCount.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}
