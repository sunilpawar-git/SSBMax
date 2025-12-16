package com.ssbmax.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions for date formatting
 * Centralizes all date formatting to avoid duplication and ensure consistency
 */
object DateFormatter {

    /**
     * Format timestamp to relative date string
     *
     * @param timestamp Timestamp in milliseconds
     * @return Relative date string (e.g., "Today", "Yesterday", "3 days ago", "Dec 15")
     */
    fun formatRelativeDate(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 24 * 60 * 60 * 1000 -> "Today"
            diff < 2 * 24 * 60 * 60 * 1000 -> "Yesterday"
            diff < 7 * 24 * 60 * 60 * 1000 -> {
                val days = (diff / (24 * 60 * 60 * 1000)).toInt()
                "$days days ago"
            }
            else -> {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                dateFormat.format(Date(timestamp))
            }
        }
    }

    /**
     * Format timestamp to full date string
     *
     * @param timestamp Timestamp in milliseconds
     * @return Full date string (e.g., "Dec 15, 2024")
     */
    fun formatFullDate(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Format timestamp to date and time string
     *
     * @param timestamp Timestamp in milliseconds
     * @return Date and time string (e.g., "Dec 15, 2024 at 02:30 PM")
     */
    fun formatDateTime(timestamp: Long): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
        return dateFormat.format(Date(timestamp))
    }

    /**
     * Format Date object to full date string
     *
     * @param date Date object
     * @return Full date string (e.g., "Dec 15, 2024")
     */
    fun formatFullDate(date: Date): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return dateFormat.format(date)
    }
}
