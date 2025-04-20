package com.example.queuemanagement

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.queuemanagement.database.Appointment
import com.example.queuemanagement.database.AppointmentDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.withContext


class MainActivity : AppCompatActivity() {

    private lateinit var appointmentAdapter: AppointmentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AndroidThreeTen.init(this) // Initialize the library

        val datePickerButton = findViewById<Button>(R.id.datePickerButton)
        val recyclerView = findViewById<RecyclerView>(R.id.appointmentRecyclerView)
        val saveCustomerButton = findViewById<Button>(R.id.saveCustomerButton)
        val customerNameEditText = findViewById<EditText>(R.id.customerName)
        val customerPhoneEditText = findViewById<EditText>(R.id.customerPhone)

        var selectedStartTime: String? = null
        var selectedEndTime: String? = null
//        var selectedDate: String? = null

        val timeSlots = AppointmentAdapter.generateTimeSlots()


        // Fetch appointments for today
        val calendar = Calendar.getInstance()
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH) + 1 // Months are 0-based
        val year = calendar.get(Calendar.YEAR)
//        selectedDate = "$day/$month/$year"
//        datePickerButton.text = "$selectedDate"
//        fetchAppointments(selectedDate!!)
//
//        // Set up RecyclerView
//        appointmentAdapter = AppointmentAdapter(timeSlots, emptyList(), selectedDate!!) {
//            // Refresh data when appointment is added or deleted
//            fetchAppointments(selectedDate!!)
//        }
        var selectedDate = getTodayDate()

        appointmentAdapter = AppointmentAdapter(timeSlots, emptyList(), { selectedDate!! }) {
            fetchAppointments(selectedDate!!)
        }
        datePickerButton.text = selectedDate
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = appointmentAdapter
        fetchAppointments(selectedDate!!)

        // Date picker logic
        datePickerButton.setOnClickListener {
            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = formatDate(selectedDay, selectedMonth, selectedYear)
                datePickerButton.text = selectedDate

                fetchAppointments(selectedDate!!)
            },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH))

            datePickerDialog.show()
        }

        // Save customer
        saveCustomerButton.setOnClickListener {
            val name = customerNameEditText.text.toString().trim()
            val phone = customerPhoneEditText.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "יש למלא שם ומספר טלפון!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!phone.matches(Regex("^\\d{10}$"))) {
                Toast.makeText(this, "מספר הפלאפון חייב להיות בן 10 ספרות!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val customer = Customer(name = name, phoneNumber = phone)
            CoroutineScope(Dispatchers.IO).launch {
                CustomerDatabase.getDatabase(applicationContext).customerDao().insert(customer)
            }
            Toast.makeText(this, "לקוח חדש נשמר בהצלחה!!", Toast.LENGTH_SHORT).show()
        }
    }

    // Method to fetch appointments for the selected date
    private fun fetchAppointments(date: String) {
        if (date.isNotEmpty()) {
            CoroutineScope(Dispatchers.Main).launch {
                val appointments = withContext(Dispatchers.IO) {
                    AppointmentDatabase.getDatabase(this@MainActivity)
                        .appointmentDao()
                        .getAppointmentsByDate(date)
                }
                appointmentAdapter.updateAppointments(appointments)
            }
        }
    }
}
