package com.ssbmax.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility functions for date formatting
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
}
