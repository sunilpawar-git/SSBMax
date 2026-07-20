package com.ssbmax.shared.data.repository

/**
 * "Personal Background" and "Family Environment" section builders for [PIQContextBuilder],
 * split out purely to stay under this repo's 300-line-per-file limit — see that file's class doc.
 */

internal fun deriveResidenceType(population: String): String = when {
    population.isBlank() -> "Unknown background"
    population.contains("metro", ignoreCase = true) -> "Metropolitan city"
    population.contains("lakh", ignoreCase = true) -> "Large city"
    population.contains("50000", ignoreCase = true) ||
        population.contains("50,000", ignoreCase = true) -> "Town"
    population.toIntOrNull()?.let { it > 100000 } == true -> "City"
    population.toIntOrNull()?.let { it > 50000 } == true -> "Town"
    population.toIntOrNull()?.let { it > 10000 } == true -> "Small town"
    else -> "Rural/Village background"
}

internal fun buildPersonalBackground(
    fullName: String,
    age: String,
    gender: String,
    state: String,
    district: String,
    maritalStatus: String,
    religion: String,
    motherTongue: String,
    permanentAddress: String,
    presentAddress: String,
    maxResidencePopulation: String,
    isDistrictHQ: Boolean,
    height: String,
    weight: String
): String {
    val residenceType = deriveResidenceType(maxResidencePopulation)
    val mobilityContext = if (permanentAddress != presentAddress && presentAddress.isNotBlank()) {
        "Has relocated from permanent address"
    } else {
        "Living at permanent address"
    }

    return """
PERSONAL BACKGROUND:
- Name: ${fullName.ifBlank { "Not provided" }}
- Age: ${age.ifBlank { "Not provided" }}
- Gender: ${gender.ifBlank { "Not provided" }}
- From: ${district.ifBlank { "Unknown district" }}, ${state.ifBlank { "Unknown state" }}
- Background: $residenceType${if (isDistrictHQ) " (District HQ)" else ""}
- Marital Status: ${maritalStatus.ifBlank { "Not provided" }}
- Religion: ${religion.ifBlank { "Not provided" }}
- Mother Tongue: ${motherTongue.ifBlank { "Not provided" }}
- Mobility: $mobilityContext
- Physical: Height ${height.ifBlank { "N/A" }}m, Weight ${weight.ifBlank { "N/A" }}kg
    """.trimIndent()
}

internal fun deriveFamilyContext(
    fatherOccupation: String,
    motherOccupation: String,
    parentsAlive: String,
    ageAtFatherDeath: String,
    ageAtMotherDeath: String
): String {
    val contexts = mutableListOf<String>()

    val defenseKeywords = listOf("army", "navy", "air force", "military", "forces", "jco", "nco", "officer")
    if (defenseKeywords.any { fatherOccupation.contains(it, ignoreCase = true) }) {
        contexts.add("Defense family background")
    }

    if (parentsAlive.contains("only", ignoreCase = true) ||
        ageAtFatherDeath.isNotBlank() || ageAtMotherDeath.isNotBlank()
    ) {
        contexts.add("Single parent/guardian upbringing")
    }

    if (motherOccupation.isNotBlank() &&
        !motherOccupation.contains("housewife", ignoreCase = true) &&
        !motherOccupation.contains("homemaker", ignoreCase = true)
    ) {
        contexts.add("Working mother")
    }

    val govtKeywords = listOf("govt", "government", "psu", "public sector", "ias", "ips")
    if (govtKeywords.any { fatherOccupation.contains(it, ignoreCase = true) }) {
        contexts.add("Government service family")
    }

    if (fatherOccupation.contains("business", ignoreCase = true) ||
        fatherOccupation.contains("entrepreneur", ignoreCase = true)
    ) {
        contexts.add("Business family")
    }

    return contexts.joinToString(", ").ifBlank { "Standard family environment" }
}

internal fun buildFamilyEnvironment(
    fatherName: String,
    fatherOccupation: String,
    fatherEducation: String,
    fatherIncome: String,
    motherName: String,
    motherOccupation: String,
    motherEducation: String,
    parentsAlive: String,
    ageAtFatherDeath: String,
    ageAtMotherDeath: String,
    guardianName: String,
    guardianOccupation: String,
    siblings: List<Map<String, Any>>
): String {
    val familyContext = deriveFamilyContext(
        fatherOccupation, motherOccupation, parentsAlive,
        ageAtFatherDeath, ageAtMotherDeath
    )

    val siblingSummary = if (siblings.isNotEmpty()) {
        siblings.mapNotNull { sibling ->
            val name = sibling["name"] as? String ?: ""
            val age = sibling["age"] as? String ?: ""
            val occupation = sibling["occupation"] as? String ?: ""
            if (name.isNotBlank()) {
                "$name (${age.ifBlank { "age unknown" }}, ${occupation.ifBlank { "occupation unknown" }})"
            } else null
        }.joinToString("; ").ifBlank { "Details not provided" }
    } else {
        "Only child / No siblings listed"
    }

    return """
FAMILY ENVIRONMENT:
- Father: ${fatherName.ifBlank { "Not provided" }}
  - Occupation: ${fatherOccupation.ifBlank { "Not provided" }}
  - Education: ${fatherEducation.ifBlank { "Not provided" }}
  - Income: ${fatherIncome.ifBlank { "Not provided" }}
- Mother: ${motherName.ifBlank { "Not provided" }}
  - Occupation: ${motherOccupation.ifBlank { "Not provided" }}
  - Education: ${motherEducation.ifBlank { "Not provided" }}
- Parents Status: ${parentsAlive.ifBlank { "Both alive (assumed)" }}${
        if (ageAtFatherDeath.isNotBlank()) "\n  - Lost father at age $ageAtFatherDeath" else ""
    }${
        if (ageAtMotherDeath.isNotBlank()) "\n  - Lost mother at age $ageAtMotherDeath" else ""
    }${
        if (guardianName.isNotBlank()) "\n- Guardian: $guardianName ($guardianOccupation)" else ""
    }
- Siblings: $siblingSummary
- Family Context: $familyContext
    """.trimIndent()
}
