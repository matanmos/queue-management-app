package com.example.queuemanagement

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.queuemanagement.database.CustomerDatabase
import com.example.queuemanagement.database.Customer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CustomerFragment : Fragment() {

    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var saveButton: Button
    private lateinit var db: CustomerDatabase

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_customers, container, false)

        nameEditText = view.findViewById(R.id.customerName)
        phoneEditText = view.findViewById(R.id.customerPhone)
        saveButton = view.findViewById(R.id.saveCustomerButton)
        db = CustomerDatabase.getDatabase(requireContext())


        saveButton.setOnClickListener {
            saveCustomer()
        }

        return view
    }

    private fun saveCustomer() {
        val name = nameEditText.text.toString().trim()
        val phone = phoneEditText.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(requireContext(), "נא למלא את כל השדות", Toast.LENGTH_SHORT).show()
            return
        }

        if (phone.length != 10) {
            Toast.makeText(requireContext(), "מספר הטלפון חייב להיות באורך של 10 ספרות", Toast.LENGTH_SHORT).show()
            return
        }


        GlobalScope.launch(Dispatchers.IO) {
            if (db.customerDao().isCustomerNameExists(name)) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "שם הלקוח כבר קיים במערכת", Toast.LENGTH_SHORT).show()
                }
                return@launch
            }
            val customer = Customer(name = name, phoneNumber = phone)

            db.customerDao().insert(customer)
            withContext(Dispatchers.Main) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "לקוח נשמר בהצלחה", Toast.LENGTH_SHORT).show()
                    nameEditText.text.clear()
                    phoneEditText.text.clear()
                }
            }
        }
    }
}
