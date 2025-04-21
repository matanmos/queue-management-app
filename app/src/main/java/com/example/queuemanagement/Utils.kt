package com.example.queuemanagement

import java.util.Calendar
import android.util.Log

fun getTodayDate(): String {
    val calendar = Calendar.getInstance()
    val day = calendar.get(Calendar.DAY_OF_MONTH)
    val month = calendar.get(Calendar.MONTH)
    val year = calendar.get(Calendar.YEAR)
    return formatDate(day, month, year)
}

fun formatDate(day: Int, month: Int, year: Int): String {
    Log.d("Utils", "formatDate: Day:$day; Month:$month; year:$year")
    var tmp = String.format("%02d/%02d/%04d", day, month + 1, year)
    Log.d("Utils", "formatDate: $tmp")
    return tmp
}