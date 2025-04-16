package com.example.queuemanagement

import android.app.DatePickerDialog
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

        var selectedDate: String = ""

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

            if (name.isEmpty() || selectedDate.isEmpty()) {
                Toast.makeText(this, "יש להזין שם ולבחור תאריך!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val appointment = Appointment(name = name, appointmentDate = selectedDate)
            CoroutineScope(Dispatchers.IO).launch {
                AppointmentDatabase.getDatabase(applicationContext).appointmentDao().insert(appointment)
            }
            Toast.makeText(this, "תור חדש נשמר בהצלחה!!", Toast.LENGTH_SHORT).show()
        }
    }
}
