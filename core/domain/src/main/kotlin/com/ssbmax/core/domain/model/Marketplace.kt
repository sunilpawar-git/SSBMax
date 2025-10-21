package com.ssbmax.core.domain.model

/**
 * Represents a coaching institute in the marketplace.
 * Used for displaying SSB preparation coaching options.
 */
data class CoachingInstitute(
    val id: String,
    val name: String,
    val description: String,
    val rating: Float,
    val reviewCount: Int,
    val location: String,
    val city: String,
    val state: String,
    val type: InstituteType,
    val priceRange: PriceRange,
    val specializations: List<String>,
    val features: List<String>,
    val imageUrl: String? = null,
    val phoneNumber: String? = null,
    val email: String? = null,
    val website: String? = null,
    val establishedYear: Int? = null,
    val successRate: Float? = null,
    val totalStudents: Int? = null
) {
    /**
     * Gets formatted price display
     */
    fun getPriceDisplay(): String {
        return when (priceRange) {
            PriceRange.BUDGET -> "₹5,000 - ₹15,000"
            PriceRange.MODERATE -> "₹15,000 - ₹30,000"
            PriceRange.PREMIUM -> "₹30,000 - ₹50,000"
            PriceRange.LUXURY -> "₹50,000+"
        }
    }

    /**
     * Gets location display
     */
    fun getLocationDisplay(): String {
        return "$city, $state"
    }
}

/**
 * Type of coaching offered by institute
 */
enum class InstituteType(val displayName: String) {
    ONLINE("Online Classes"),
    PHYSICAL("Physical Classes"),
    BOTH("Online & Physical");

    companion object {
        fun fromDisplayName(name: String): InstituteType? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}

/**
 * Price range for coaching
 */
enum class PriceRange(val displayName: String) {
    BUDGET("Budget (₹5k-15k)"),
    MODERATE("Moderate (₹15k-30k)"),
    PREMIUM("Premium (₹30k-50k)"),
    LUXURY("Luxury (₹50k+)");

    companion object {
        fun fromDisplayName(name: String): PriceRange? {
            return values().find { it.displayName.equals(name, ignoreCase = true) }
        }
    }
}

