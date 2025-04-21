package com.example.queuemanagement

import android.app.AlertDialog
import android.text.InputType
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.example.queuemanagement.database.Appointment
import com.example.queuemanagement.database.AppointmentDatabase
import com.example.queuemanagement.database.CustomerDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch

import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter

class AppointmentAdapter(
    private var timeSlots: List<Pair<String, String>>, // Pair<startTime, endTime>
    private var appointments: List<Appointment>,
    private val getSelectedDate: () -> String,
    private val onAppointmentChanged: () -> Unit
) : RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    class AppointmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val timeSlotText: TextView = view.findViewById(R.id.timeSlotText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_time_slot, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val timeSlot = timeSlots[position]
        val appointment = appointments.find { it.startTime == timeSlot.first && it.endTime == timeSlot.second }


        if (appointment != null) {
            // Filled slot: Show appointment details and set filled background
            holder.timeSlotText.text = "${appointment.startTime} - ${appointment.endTime}: ${appointment.name}"
            holder.itemView.setBackgroundResource(R.drawable.bg_filled_slot)

            // Handle click for deleting appointment
            holder.itemView.setOnClickListener {
                // Show delete confirmation dialog
                AlertDialog.Builder(holder.itemView.context)
                    .setMessage("האם למחוק את התור?")
                    .setPositiveButton("Yes") { _, _ ->
                        CoroutineScope(Dispatchers.IO).launch {
                            AppointmentDatabase.getDatabase(holder.itemView.context)
                                .appointmentDao().delete(appointment)
                            withContext(Dispatchers.Main) {
                                onAppointmentChanged() // Refresh the data
                            }
                        }
                    }
                    .setNegativeButton("No", null)
                    .show()
            }
        } else {
            // Empty slot: Show available status and set empty background
            holder.timeSlotText.text = "${timeSlot.first} - ${timeSlot.second} - פנוי"
            holder.itemView.setBackgroundResource(R.drawable.bg_empty_slot)

            // Handle click for adding an appointment
            holder.itemView.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    val customerNames = CustomerDatabase.getDatabase(holder.itemView.context)
                        .customerDao()
                        .getAllCustomerNames() // Make sure this DAO function exists

                    withContext(Dispatchers.Main) {
                        val input = AutoCompleteTextView(holder.itemView.context).apply {
                            inputType = InputType.TYPE_CLASS_TEXT
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT
                            )
                            textDirection = View.TEXT_DIRECTION_RTL
                            gravity = Gravity.END
                            threshold = 1 // Start suggesting after 1 letter
                            setAdapter(
                                ArrayAdapter(
                                    holder.itemView.context,
                                    android.R.layout.simple_dropdown_item_1line,
                                    customerNames
                                )
                            )
                        }

                        AlertDialog.Builder(holder.itemView.context)
                            .setTitle("שם הלקוח")
                            .setView(input)
                            .setPositiveButton("Save") { _, _ ->
                                val customerName = input.text.toString().trim()
                                if (customerName.isNotEmpty()) {
                                    val newAppointment = Appointment(
                                        name = customerName,
                                        appointmentDate = getSelectedDate(),
                                        startTime = timeSlot.first,
                                        endTime = timeSlot.second
                                    )
                                    CoroutineScope(Dispatchers.IO).launch {
                                        AppointmentDatabase.getDatabase(holder.itemView.context)
                                            .appointmentDao().insert(newAppointment)

                                        val all = AppointmentDatabase.getDatabase(holder.itemView.context)
                                            .appointmentDao().getAppointmentsByDate(newAppointment.appointmentDate)

                                        withContext(Dispatchers.Main) {
                                            onAppointmentChanged()
                                        }
                                    }
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            }
        }
    }


    override fun getItemCount(): Int = timeSlots.size

    fun updateAppointments(newAppointments: List<Appointment>) {
        this.appointments = newAppointments
        notifyDataSetChanged()
    }

    fun updateTimeSlots(newTimeSlots: List<Pair<String, String>>) {
        this.timeSlots = newTimeSlots
        notifyDataSetChanged()
    }

    companion object {
        fun generateTimeSlots(): List<Pair<String, String>> {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val slots = mutableListOf<Pair<String, String>>()
            var current = LocalTime.of(7, 0)
            while (current.isBefore(LocalTime.of(19, 0))) {
                val end = current.plusMinutes(45)
                slots.add(Pair(current.format(formatter), end.format(formatter)))
                current = end
            }
            return slots
        }
    }
}
