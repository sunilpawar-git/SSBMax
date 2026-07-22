package com.ssbmax.shared.ui.piq.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.piq_achievement
import ssbmax.shared.generated.resources.piq_age
import ssbmax.shared.generated.resources.piq_age_father_death
import ssbmax.shared.generated.resources.piq_age_mother_death
import ssbmax.shared.generated.resources.piq_batch_number
import ssbmax.shared.generated.resources.piq_boarder
import ssbmax.shared.generated.resources.piq_board
import ssbmax.shared.generated.resources.piq_cgpa
import ssbmax.shared.generated.resources.piq_chest_number
import ssbmax.shared.generated.resources.piq_date_of_birth
import ssbmax.shared.generated.resources.piq_district
import ssbmax.shared.generated.resources.piq_education
import ssbmax.shared.generated.resources.piq_father_education
import ssbmax.shared.generated.resources.piq_father_income
import ssbmax.shared.generated.resources.piq_father_name
import ssbmax.shared.generated.resources.piq_father_occupation
import ssbmax.shared.generated.resources.piq_full_name
import ssbmax.shared.generated.resources.piq_guardian_education
import ssbmax.shared.generated.resources.piq_guardian_income
import ssbmax.shared.generated.resources.piq_guardian_name
import ssbmax.shared.generated.resources.piq_guardian_occupation
import ssbmax.shared.generated.resources.piq_income
import ssbmax.shared.generated.resources.piq_is_district_hq
import ssbmax.shared.generated.resources.piq_marital_status
import ssbmax.shared.generated.resources.piq_max_residence
import ssbmax.shared.generated.resources.piq_max_residence_pop
import ssbmax.shared.generated.resources.piq_medium
import ssbmax.shared.generated.resources.piq_mother_education
import ssbmax.shared.generated.resources.piq_mother_name
import ssbmax.shared.generated.resources.piq_mother_occupation
import ssbmax.shared.generated.resources.piq_mother_tongue
import ssbmax.shared.generated.resources.piq_occupation
import ssbmax.shared.generated.resources.piq_oir_number
import ssbmax.shared.generated.resources.piq_parents_alive
import ssbmax.shared.generated.resources.piq_percentage
import ssbmax.shared.generated.resources.piq_permanent_address
import ssbmax.shared.generated.resources.piq_permanent_pop
import ssbmax.shared.generated.resources.piq_present_address
import ssbmax.shared.generated.resources.piq_present_pop
import ssbmax.shared.generated.resources.piq_religion
import ssbmax.shared.generated.resources.piq_review_field_10th
import ssbmax.shared.generated.resources.piq_review_field_12th
import ssbmax.shared.generated.resources.piq_review_field_educational_record
import ssbmax.shared.generated.resources.piq_review_field_graduation
import ssbmax.shared.generated.resources.piq_review_field_pg
import ssbmax.shared.generated.resources.piq_review_field_siblings
import ssbmax.shared.generated.resources.piq_review_yes
import ssbmax.shared.generated.resources.piq_sc_st_obc
import ssbmax.shared.generated.resources.piq_school_name
import ssbmax.shared.generated.resources.piq_selection_board
import ssbmax.shared.generated.resources.piq_sibling_name
import ssbmax.shared.generated.resources.piq_sibling_name_younger
import ssbmax.shared.generated.resources.piq_state
import ssbmax.shared.generated.resources.piq_stream
import ssbmax.shared.generated.resources.piq_upsc_roll
import ssbmax.shared.generated.resources.piq_year_passing

/**
 * Page 1 ("Personal & Family") summary section of
 * [com.ssbmax.shared.ui.piq.PIQReviewScreen]. Reuses the same string
 * resources as the corresponding Page 1 input fields
 * ([PIQPage1PersonalFields]/[PIQPage1FamilyFields]/[PIQPage1EducationFields])
 * as labels here (the Android original hardcodes matching English literals
 * like `"OIR Number"` directly as `ReviewField()` args -- this port
 * externalizes them by reusing the existing per-field string resources
 * rather than duplicating ~50 near-identical keys).
 */
@Composable
fun PIQReviewPersonalSection(answers: Map<String, String>) {
    ReviewField(stringResource(Res.string.piq_oir_number), answers["oirNumber"])
    ReviewField(stringResource(Res.string.piq_selection_board), answers["selectionBoard"])
    ReviewField(stringResource(Res.string.piq_batch_number), answers["batchNumber"])
    ReviewField(stringResource(Res.string.piq_chest_number), answers["chestNumber"])
    ReviewField(stringResource(Res.string.piq_upsc_roll), answers["upscRollNumber"])
    Divider()

    ReviewField(stringResource(Res.string.piq_full_name), answers["fullName"])
    ReviewField(stringResource(Res.string.piq_date_of_birth), answers["dateOfBirth"])
    Divider()

    ReviewField(stringResource(Res.string.piq_state), answers["state"])
    ReviewField(stringResource(Res.string.piq_district), answers["district"])
    ReviewField(stringResource(Res.string.piq_religion), answers["religion"])
    ReviewField(stringResource(Res.string.piq_sc_st_obc), answers["scStObcStatus"])
    ReviewField(stringResource(Res.string.piq_mother_tongue), answers["motherTongue"])
    ReviewField(stringResource(Res.string.piq_marital_status), answers["maritalStatus"])
    Divider()

    ReviewField(stringResource(Res.string.piq_permanent_address), answers["permanentAddress"])
    ReviewField(stringResource(Res.string.piq_present_address), answers["presentAddress"])
    ReviewField(stringResource(Res.string.piq_max_residence), answers["maximumResidence"])
    ReviewField(stringResource(Res.string.piq_max_residence_pop), answers["maximumResidencePopulation"])
    ReviewField(stringResource(Res.string.piq_present_pop), answers["presentResidencePopulation"])
    ReviewField(stringResource(Res.string.piq_permanent_pop), answers["permanentResidencePopulation"])
    if (answers["isDistrictHQ"]?.toBoolean() == true) {
        ReviewField(stringResource(Res.string.piq_is_district_hq), stringResource(Res.string.piq_review_yes))
    }
    Divider()

    ReviewField(stringResource(Res.string.piq_father_name), answers["fatherName"])
    ReviewField(stringResource(Res.string.piq_father_occupation), answers["fatherOccupation"])
    ReviewField(stringResource(Res.string.piq_father_education), answers["fatherEducation"])
    ReviewField(stringResource(Res.string.piq_father_income), answers["fatherIncome"])
    Divider()

    ReviewField(stringResource(Res.string.piq_mother_name), answers["motherName"])
    ReviewField(stringResource(Res.string.piq_mother_occupation), answers["motherOccupation"])
    ReviewField(stringResource(Res.string.piq_mother_education), answers["motherEducation"])
    Divider()

    Text(stringResource(Res.string.piq_review_field_siblings), style = MaterialTheme.typography.titleSmall)
    repeat(2) { i ->
        ReviewSibling(answers, "elderSibling${i + 1}_", stringResource(Res.string.piq_sibling_name, i + 1))
    }
    repeat(2) { i ->
        ReviewSibling(answers, "youngerSibling${i + 1}_", stringResource(Res.string.piq_sibling_name_younger, i + 1))
    }
    Divider()

    ReviewField(stringResource(Res.string.piq_parents_alive), answers["parentsAlive"])
    ReviewField(stringResource(Res.string.piq_age_father_death), answers["ageAtFatherDeath"])
    ReviewField(stringResource(Res.string.piq_age_mother_death), answers["ageAtMotherDeath"])
    if (answers["parentsAlive"] == "None") {
        ReviewField(stringResource(Res.string.piq_guardian_name), answers["guardianName"])
        ReviewField(stringResource(Res.string.piq_guardian_occupation), answers["guardianOccupation"])
        ReviewField(stringResource(Res.string.piq_guardian_education), answers["guardianEducation"])
        ReviewField(stringResource(Res.string.piq_guardian_income), answers["guardianIncome"])
    }
    Divider()

    Text(stringResource(Res.string.piq_review_field_educational_record), style = MaterialTheme.typography.titleSmall)
    Text(stringResource(Res.string.piq_review_field_10th), style = MaterialTheme.typography.titleSmall)
    ReviewEducation(answers, "education10th_", includeStream = false)
    Divider()
    Text(stringResource(Res.string.piq_review_field_12th), style = MaterialTheme.typography.titleSmall)
    ReviewEducation(answers, "education12th_", includeStream = true)
    Divider()
    Text(stringResource(Res.string.piq_review_field_graduation), style = MaterialTheme.typography.titleSmall)
    ReviewEducation(answers, "educationGrad_", includeStream = false)
    Divider()
    Text(stringResource(Res.string.piq_review_field_pg), style = MaterialTheme.typography.titleSmall)
    ReviewEducation(answers, "educationPG_", includeStream = false)
}

@Composable
private fun ReviewSibling(answers: Map<String, String>, prefix: String, nameLabel: String) {
    val name = answers["${prefix}name"]
    if (!name.isNullOrBlank()) {
        ReviewField(nameLabel, name)
        ReviewField(stringResource(Res.string.piq_age), answers["${prefix}age"])
        ReviewField(stringResource(Res.string.piq_education), answers["${prefix}education"])
        ReviewField(stringResource(Res.string.piq_occupation), answers["${prefix}occupation"])
        ReviewField(stringResource(Res.string.piq_income), answers["${prefix}income"])
    }
}

@Composable
private fun ReviewEducation(answers: Map<String, String>, prefix: String, includeStream: Boolean) {
    ReviewField(stringResource(Res.string.piq_school_name), answers["${prefix}institution"])
    ReviewField(stringResource(Res.string.piq_board), answers["${prefix}board"])
    if (includeStream) {
        ReviewField(stringResource(Res.string.piq_stream), answers["${prefix}stream"])
    }
    ReviewField(stringResource(Res.string.piq_year_passing), answers["${prefix}year"])
    ReviewField(stringResource(Res.string.piq_percentage), answers["${prefix}percentage"])
    ReviewField(stringResource(Res.string.piq_cgpa), answers["${prefix}cgpa"])
    ReviewField(stringResource(Res.string.piq_medium), answers["${prefix}medium"])
    ReviewField(stringResource(Res.string.piq_boarder), answers["${prefix}boarder"])
    ReviewField(stringResource(Res.string.piq_achievement), answers["${prefix}achievement"])
}

@Composable
private fun Divider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}
