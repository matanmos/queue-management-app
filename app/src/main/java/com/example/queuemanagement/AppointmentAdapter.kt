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
import org.threeten.bp.Duration


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
                        val context = holder.itemView.context

                        val layout = LinearLayout(context).apply {
                            orientation = LinearLayout.VERTICAL
                            setPadding(50, 20, 50, 20)
                        }

                        val input = AutoCompleteTextView(context).apply {
                            inputType = InputType.TYPE_CLASS_TEXT
                            textDirection = View.TEXT_DIRECTION_RTL
                            gravity = Gravity.END
                            threshold = 1
                            setAdapter(
                                ArrayAdapter(
                                    context,
                                    android.R.layout.simple_dropdown_item_1line,
                                    customerNames
                                )
                            )
                        }

                        val durationSpinner = Spinner(context).apply {
                            adapter = ArrayAdapter(
                                context,
                                android.R.layout.simple_spinner_dropdown_item,
                                listOf("30 דקות", "45 דקות", "60 דקות")
                            )
                        }

                        layout.addView(TextView(context).apply { text = "שם לקוח:" })
                        layout.addView(input)
                        layout.addView(TextView(context).apply { text = "משך תור:" })
                        layout.addView(durationSpinner)

                        AlertDialog.Builder(context)
                            .setTitle("הוספת תור")
                            .setView(layout)
                            .setPositiveButton("שמור") { _, _ ->
                                val customerName = input.text.toString().trim()
                                val selectedDuration = when (durationSpinner.selectedItem.toString()) {
                                    "30 דקות" -> 30L
                                    "45 דקות" -> 45L
                                    "60 דקות" -> 60L
                                    else -> 45L
                                }
                                if (customerName.isNotEmpty()) {
                                    CoroutineScope(Dispatchers.IO).launch {
                                        val formatter = DateTimeFormatter.ofPattern("HH:mm")
                                        val start = LocalTime.parse(timeSlot.first, formatter)
                                        val end = start.plusMinutes(selectedDuration)
                                        val newAppointment = Appointment(
                                            name = customerName,
                                            appointmentDate = getSelectedDate(),
                                            startTime = start.format(formatter),
                                            endTime = end.format(formatter)
                                        )
                                        AppointmentDatabase.getDatabase(context)
                                            .appointmentDao().insert(newAppointment)
                                        withContext(Dispatchers.Main) {
                                            onAppointmentChanged()
                                        }
                                    }
                                }
                            }
                            .setNegativeButton("ביטול", null)
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

        fun generateTimeSlotsDynamic(
            appointments: List<Appointment>,
            selectedDuration: Duration,
            openingTime: LocalTime = LocalTime.of(7, 0),
            closingTime: LocalTime = LocalTime.of(19, 0)
        ): List<Pair<String, String>> {
            val formatter = DateTimeFormatter.ofPattern("HH:mm")
            val result = mutableListOf<Pair<String, String>>()

            val sortedAppointments = appointments.sortedBy { LocalTime.parse(it.startTime) }
            var currentTime = openingTime

            for (appointment in sortedAppointments) {
                val appointmentStart = LocalTime.parse(appointment.startTime)
                val appointmentEnd = LocalTime.parse(appointment.endTime)

                // Add available slots before this appointment
                while (currentTime.plus(selectedDuration) <= appointmentStart) {
                    val slotEnd = currentTime.plus(selectedDuration)
                    result.add(Pair(currentTime.format(formatter), slotEnd.format(formatter)))
                    currentTime = slotEnd
                }

                // Skip to after the current appointment
                if (currentTime < appointmentEnd) {
                    currentTime = appointmentEnd
                }

                // Add this appointment as a filled slot
                result.add(Pair(appointmentStart.format(formatter), appointmentEnd.format(formatter)))
            }

            // Add remaining slots after the last appointment
            while (currentTime.plus(selectedDuration) <= closingTime) {
                val slotEnd = currentTime.plus(selectedDuration)
                result.add(Pair(currentTime.format(formatter), slotEnd.format(formatter)))
                currentTime = slotEnd
            }

            return result
        }


        fun generateAvailableSlots(
            existingAppointments: List<Appointment>,
            duration: Duration,
            openingTime: LocalTime = LocalTime.of(7, 0),
            closingTime: LocalTime = LocalTime.of(19, 0)
        ): List<Pair<LocalTime, LocalTime>> {
            val slots = mutableListOf<Pair<LocalTime, LocalTime>>()
            var currentTime = openingTime

            while (currentTime.plus(duration) <= closingTime) {
                val endTime = currentTime.plus(duration)
                if (isTimeSlotAvailable(currentTime, endTime, existingAppointments)) {
                    slots.add(currentTime to endTime)
                }
                currentTime = currentTime.plusMinutes(15) // advance in 15min increments
            }

            return slots
        }

        fun isTimeSlotAvailable(start: LocalTime, end: LocalTime, existingAppointments: List<Appointment>): Boolean {
            for (appointment in existingAppointments) {
                val existingStart = LocalTime.parse(appointment.startTime)
                val existingEnd = LocalTime.parse(appointment.endTime)
                if (start < existingEnd && end > existingStart) {
                    return false
                }
            }
            return true
        }

    }
}
