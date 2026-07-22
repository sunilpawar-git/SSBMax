package com.ssbmax.shared.ui.piq.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.piq_age_father_death
import ssbmax.shared.generated.resources.piq_age_mother_death
import ssbmax.shared.generated.resources.piq_batch_number
import ssbmax.shared.generated.resources.piq_batch_placeholder
import ssbmax.shared.generated.resources.piq_batch_supporting
import ssbmax.shared.generated.resources.piq_chest_number
import ssbmax.shared.generated.resources.piq_date_of_birth
import ssbmax.shared.generated.resources.piq_district
import ssbmax.shared.generated.resources.piq_dob_placeholder
import ssbmax.shared.generated.resources.piq_family_enhancement
import ssbmax.shared.generated.resources.piq_full_name
import ssbmax.shared.generated.resources.piq_guardian_education
import ssbmax.shared.generated.resources.piq_guardian_income
import ssbmax.shared.generated.resources.piq_guardian_name
import ssbmax.shared.generated.resources.piq_guardian_occupation
import ssbmax.shared.generated.resources.piq_guardian_section
import ssbmax.shared.generated.resources.piq_header_section
import ssbmax.shared.generated.resources.piq_is_district_hq
import ssbmax.shared.generated.resources.piq_marital_status
import ssbmax.shared.generated.resources.piq_max_residence
import ssbmax.shared.generated.resources.piq_max_residence_pop
import ssbmax.shared.generated.resources.piq_mother_tongue
import ssbmax.shared.generated.resources.piq_oir_number
import ssbmax.shared.generated.resources.piq_oir_placeholder
import ssbmax.shared.generated.resources.piq_parents_alive
import ssbmax.shared.generated.resources.piq_permanent_address
import ssbmax.shared.generated.resources.piq_permanent_pop
import ssbmax.shared.generated.resources.piq_personal_details
import ssbmax.shared.generated.resources.piq_present_address
import ssbmax.shared.generated.resources.piq_present_pop
import ssbmax.shared.generated.resources.piq_religion
import ssbmax.shared.generated.resources.piq_residence_section
import ssbmax.shared.generated.resources.piq_sc_st_obc
import ssbmax.shared.generated.resources.piq_selection_board
import ssbmax.shared.generated.resources.piq_state
import ssbmax.shared.generated.resources.piq_upsc_roll

/**
 * Page 1, part 1 of [com.ssbmax.shared.ui.piq.PIQTestScreen]: header/name,
 * residence, personal-details table, and family-enhancement/guardian
 * sections. Split out of the Android original's single 845-line
 * `PIQTestScreen.kt` `Page1Content()` to respect the 300-line-per-file limit
 * -- see [PIQPage1FamilyFields] and [PIQPage1EducationFields] for the rest
 * of Page 1.
 */
@Composable
fun PIQPage1PersonalFields(
    answers: Map<String, String>,
    selectionBoardOptions: List<String>,
    onFieldChange: (String, String) -> Unit
) {
    PIQSectionHeader(stringResource(Res.string.piq_header_section))
    PIQTextField(
        label = stringResource(Res.string.piq_oir_number),
        value = answers["oirNumber"] ?: "",
        onValueChange = { onFieldChange("oirNumber", it) },
        placeholder = stringResource(Res.string.piq_oir_placeholder),
        enabled = false
    )
    PIQDropdownField(
        label = stringResource(Res.string.piq_selection_board),
        value = answers["selectionBoard"] ?: "",
        options = selectionBoardOptions,
        onValueChange = { onFieldChange("selectionBoard", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_batch_number),
        value = answers["batchNumber"] ?: "",
        onValueChange = { onFieldChange("batchNumber", it) },
        placeholder = stringResource(Res.string.piq_batch_placeholder),
        enabled = false,
        supportingText = stringResource(Res.string.piq_batch_supporting)
    )
    PIQTextField(
        label = stringResource(Res.string.piq_chest_number),
        value = answers["chestNumber"] ?: "",
        onValueChange = { onFieldChange("chestNumber", it) },
        placeholder = stringResource(Res.string.piq_batch_placeholder),
        enabled = false,
        supportingText = stringResource(Res.string.piq_batch_supporting)
    )
    PIQTextField(
        label = stringResource(Res.string.piq_upsc_roll),
        value = answers["upscRollNumber"] ?: "",
        onValueChange = { onFieldChange("upscRollNumber", it) },
        placeholder = stringResource(Res.string.piq_batch_placeholder),
        enabled = false,
        supportingText = stringResource(Res.string.piq_batch_supporting)
    )
    PIQTextField(
        label = stringResource(Res.string.piq_full_name),
        value = answers["fullName"] ?: "",
        onValueChange = { onFieldChange("fullName", it) }
    )

    PIQSectionHeader(stringResource(Res.string.piq_residence_section))
    PIQTextField(
        label = stringResource(Res.string.piq_max_residence),
        value = answers["maximumResidence"] ?: "",
        onValueChange = { onFieldChange("maximumResidence", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_max_residence_pop),
        value = answers["maximumResidencePopulation"] ?: "",
        onValueChange = { onFieldChange("maximumResidencePopulation", it) },
        keyboardType = KeyboardType.Number
    )
    PIQTextField(
        label = stringResource(Res.string.piq_present_address),
        value = answers["presentAddress"] ?: "",
        onValueChange = { onFieldChange("presentAddress", it) },
        multiline = true,
        minLines = 3
    )
    PIQTextField(
        label = stringResource(Res.string.piq_present_pop),
        value = answers["presentResidencePopulation"] ?: "",
        onValueChange = { onFieldChange("presentResidencePopulation", it) },
        keyboardType = KeyboardType.Number
    )
    PIQTextField(
        label = stringResource(Res.string.piq_permanent_address),
        value = answers["permanentAddress"] ?: "",
        onValueChange = { onFieldChange("permanentAddress", it) },
        multiline = true,
        minLines = 3
    )
    PIQTextField(
        label = stringResource(Res.string.piq_permanent_pop),
        value = answers["permanentResidencePopulation"] ?: "",
        onValueChange = { onFieldChange("permanentResidencePopulation", it) },
        keyboardType = KeyboardType.Number
    )
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = answers["isDistrictHQ"]?.toBoolean() ?: false,
            onCheckedChange = { onFieldChange("isDistrictHQ", it.toString()) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(Res.string.piq_is_district_hq))
    }

    PIQSectionHeader(stringResource(Res.string.piq_personal_details))
    PIQTextField(
        label = stringResource(Res.string.piq_state),
        value = answers["state"] ?: "",
        onValueChange = { onFieldChange("state", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_district),
        value = answers["district"] ?: "",
        onValueChange = { onFieldChange("district", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_religion),
        value = answers["religion"] ?: "",
        onValueChange = { onFieldChange("religion", it) }
    )
    PIQDropdownField(
        label = stringResource(Res.string.piq_sc_st_obc),
        value = answers["scStObcStatus"] ?: "",
        options = listOf("", "SC", "ST", "OBC"),
        onValueChange = { onFieldChange("scStObcStatus", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_mother_tongue),
        value = answers["motherTongue"] ?: "",
        onValueChange = { onFieldChange("motherTongue", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_date_of_birth),
        value = answers["dateOfBirth"] ?: "",
        onValueChange = { onFieldChange("dateOfBirth", it) },
        placeholder = stringResource(Res.string.piq_dob_placeholder)
    )
    PIQDropdownField(
        label = stringResource(Res.string.piq_marital_status),
        value = answers["maritalStatus"] ?: "",
        options = listOf("", "Single", "Married", "Widower"),
        onValueChange = { onFieldChange("maritalStatus", it) }
    )

    PIQSectionHeader(stringResource(Res.string.piq_family_enhancement))
    PIQDropdownField(
        label = stringResource(Res.string.piq_parents_alive),
        value = answers["parentsAlive"] ?: "",
        options = listOf("", "Both", "Father Only", "Mother Only", "None"),
        onValueChange = { onFieldChange("parentsAlive", it) }
    )
    if (answers["parentsAlive"] == "Mother Only" || answers["parentsAlive"] == "None") {
        PIQTextField(
            label = stringResource(Res.string.piq_age_father_death),
            value = answers["ageAtFatherDeath"] ?: "",
            onValueChange = { onFieldChange("ageAtFatherDeath", it) },
            keyboardType = KeyboardType.Number
        )
    }
    if (answers["parentsAlive"] == "Father Only" || answers["parentsAlive"] == "None") {
        PIQTextField(
            label = stringResource(Res.string.piq_age_mother_death),
            value = answers["ageAtMotherDeath"] ?: "",
            onValueChange = { onFieldChange("ageAtMotherDeath", it) },
            keyboardType = KeyboardType.Number
        )
    }
    if (answers["parentsAlive"] == "None") {
        PIQSectionHeader(stringResource(Res.string.piq_guardian_section))
        PIQTextField(
            label = stringResource(Res.string.piq_guardian_name),
            value = answers["guardianName"] ?: "",
            onValueChange = { onFieldChange("guardianName", it) }
        )
        PIQTextField(
            label = stringResource(Res.string.piq_guardian_occupation),
            value = answers["guardianOccupation"] ?: "",
            onValueChange = { onFieldChange("guardianOccupation", it) }
        )
        PIQTextField(
            label = stringResource(Res.string.piq_guardian_education),
            value = answers["guardianEducation"] ?: "",
            onValueChange = { onFieldChange("guardianEducation", it) }
        )
        PIQTextField(
            label = stringResource(Res.string.piq_guardian_income),
            value = answers["guardianIncome"] ?: "",
            onValueChange = { onFieldChange("guardianIncome", it) }
        )
    }
}
