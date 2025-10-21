package com.ssbmax.core.domain.model

/**
 * Represents a single FAQ item
 */
data class FAQItem(
    val id: String,
    val question: String,
    val answer: String,
    val category: FAQCategory,
    val order: Int
)

/**
 * FAQ categories for organization
 */
enum class FAQCategory(val displayName: String) {
    GENERAL("General"),
    TESTS("Tests & Assessments"),
    SUBSCRIPTION("Subscription & Billing"),
    TECHNICAL("Technical Support"),
    SSB_PROCESS("SSB Process")
}

