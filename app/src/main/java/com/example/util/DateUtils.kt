package com.example.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object DateUtils {
    private val TIME_ZONE_KL = TimeZone.getTimeZone("Asia/Kuala_Lumpur")

    fun getCurrentCalendar(): Calendar {
        return Calendar.getInstance(TIME_ZONE_KL)
    }

    fun getCurrentMonthKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        sdf.timeZone = TIME_ZONE_KL
        return sdf.format(Date())
    }

    fun formatMonthKey(monthKey: String): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM", Locale.US)
            sdfInput.timeZone = TIME_ZONE_KL
            val date = sdfInput.parse(monthKey) ?: return monthKey
            val sdfOutput = SimpleDateFormat("MMMM yyyy", Locale.US)
            sdfOutput.timeZone = TIME_ZONE_KL
            sdfOutput.format(date)
        } catch (e: Exception) {
            monthKey
        }
    }

    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("d MMM yyyy", Locale.US)
        sdf.timeZone = TIME_ZONE_KL
        return sdf.format(Date(timestamp))
    }

    fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.US)
        sdf.timeZone = TIME_ZONE_KL
        return sdf.format(Date(timestamp))
    }

    /**
     * Requirement: Check-in period is 27th until the final calendar day of each month.
     */
    fun isCheckInPeriod(): Boolean {
        val cal = getCurrentCalendar()
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        return dayOfMonth >= 27
    }

    fun getDaysUntilCheckIn(): Int {
        val cal = getCurrentCalendar()
        val dayOfMonth = cal.get(Calendar.DAY_OF_MONTH)
        return if (dayOfMonth < 27) 27 - dayOfMonth else 0
    }

    fun isDecember(monthKey: String): Boolean {
        return monthKey.endsWith("-12")
    }

    fun formatCurrency(amount: Double): String {
        return "RM ${String.format(Locale.US, "%,.2f", amount)}"
    }
}
