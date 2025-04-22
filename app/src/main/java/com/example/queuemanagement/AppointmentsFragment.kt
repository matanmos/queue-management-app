package com.example.queuemanagement

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.queuemanagement.database.Appointment
import com.example.queuemanagement.database.AppointmentDatabase
import com.example.queuemanagement.database.CustomerDatabase
import kotlinx.coroutines.*
import org.threeten.bp.Duration
import java.time.format.DateTimeFormatter
import java.util.*

class AppointmentFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: AppointmentAdapter
    private lateinit var datePickerButton: Button
    private lateinit var sendSmsButton: Button
    private lateinit var timeSlotSpinner: Spinner
    private lateinit var db: AppointmentDatabase
    private lateinit var viewModel: AppointmentViewModel

    private var selectedDate: String = getTodayDate()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_appointments, container, false)

        recyclerView = view.findViewById(R.id.appointmentRecyclerView)
        datePickerButton = view.findViewById(R.id.datePickerButton)
        sendSmsButton = view.findViewById(R.id.sendSmsButton)
        timeSlotSpinner = view.findViewById(R.id.timeSlotSpinner)
        db = AppointmentDatabase.getDatabase(requireContext())

        val viewModelFactory = AppointmentViewModelFactory(db)
        viewModel = ViewModelProvider(this, viewModelFactory)[AppointmentViewModel::class.java]

        adapter = AppointmentAdapter(
            emptyList(),
            emptyList(),
            { selectedDate },
            { loadAppointments(selectedDate) }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        datePickerButton.text = selectedDate
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

        // Observe appointments and update RecyclerView
        viewModel.appointmentsForDate.observe(viewLifecycleOwner) { appointments ->
            val selectedDuration = when (timeSlotSpinner.selectedItem.toString()) {
                "30 דקות" -> Duration.ofMinutes(30)
                "45 דקות" -> Duration.ofMinutes(45)
                "60 דקות" -> Duration.ofMinutes(60)
                else -> Duration.ofMinutes(45)
            }

            val timeSlots = AppointmentAdapter.generateTimeSlotsDynamic(appointments, selectedDuration)

            adapter.updateTimeSlots(
                timeSlots.map {
                    Pair(
                        it.first.format(DateTimeFormatter.ofPattern("HH:mm")),
                        it.second.format(DateTimeFormatter.ofPattern("HH:mm"))
                    )
                }
            )
            adapter.updateAppointments(appointments)
        }

        return view
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = formatDate(selectedDay, selectedMonth, selectedYear)
                datePickerButton.text = selectedDate
                loadAppointments(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun loadAppointments(date: String) {
        viewModel.loadAppointmentsForDate(date)
    }

    private fun sendSmsToAll(date: String, message: String) {
        val context = requireContext()
        CoroutineScope(Dispatchers.IO).launch {
            val appointments = db.appointmentDao().getAppointmentsByDate(date)
            val customerDb = CustomerDatabase.getDatabase(context)
            val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }

            var successCount = 0
            var failCount = 0

            for (appointment in appointments) {
                val customer = customerDb.customerDao().getCustomerByName(appointment.name)
                val phone = customer?.phoneNumber?.let { normalizePhoneNumber(it) }

                if (!phone.isNullOrEmpty()) {
                    try {
                        smsManager.sendTextMessage(phone, null, message, null, null)
                        successCount++
                    } catch (e: Exception) {
                        Log.e("SMS", "Failed to send SMS to ${appointment.name} at $phone", e)
                        failCount++
                    }
                } else {
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
