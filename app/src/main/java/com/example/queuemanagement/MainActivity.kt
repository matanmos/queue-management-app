package com.example.queuemanagement

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val datePickerButton = findViewById<Button>(R.id.datePickerButton)
        val saveCustomerButton = findViewById<Button>(R.id.saveCustomerButton)
        val saveAppointmentButton = findViewById<Button>(R.id.saveAppointmentButton)
        val customerNameEditText = findViewById<EditText>(R.id.customerName)
        val customerPhoneEditText = findViewById<EditText>(R.id.customerPhone)
        val startTimeButton = findViewById<Button>(R.id.startTimeButton)
        val endTimeButton = findViewById<Button>(R.id.endTimeButton)

        var selectedStartTime: String?  = null
        var selectedEndTime: String?  = null
        var selectedDate: String? = null


        // Start time picker
        startTimeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { dialog: TimePicker?, selectedHour: Int, selectedMinute: Int ->
                selectedStartTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                startTimeButton.text = "שעת התחלה: $selectedStartTime"
            }, hour, minute, true).show()
        }

        endTimeButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val minute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(this, { dialog: TimePicker?, selectedHour: Int, selectedMinute: Int ->
                selectedEndTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                endTimeButton.text = "שעת סיום: $selectedEndTime"
            }, hour, minute, true).show()
        }
        // Date picker logic
        datePickerButton.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                selectedDate = "$selectedDay/${selectedMonth + 1}/$selectedYear"
                datePickerButton.text = "$selectedDate"
                Toast.makeText(this, "נבחר תאריך: $selectedDate", Toast.LENGTH_SHORT).show()
            }, year, month, day)

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

        // Save appointment
        saveAppointmentButton.setOnClickListener {
            val name = customerNameEditText.text.toString().trim()
            val appointmentText = datePickerButton.text.toString().trim()

            if (selectedDate == null || selectedStartTime == null || selectedEndTime == null || name.isEmpty()) {
                Toast.makeText(this, "אנא מלא את כל הפרטים!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val appointment = Appointment(
                name = name,
                appointmentDate = appointmentText,
                startTime = selectedStartTime.toString(),
                endTime = selectedEndTime.toString()
            )
            CoroutineScope(Dispatchers.IO).launch {
                AppointmentDatabase.getDatabase(applicationContext).appointmentDao().insert(appointment)
            }
            Toast.makeText(this, "תור חדש נשמר בהצלחה!!", Toast.LENGTH_SHORT).show()
        }
    }
}
