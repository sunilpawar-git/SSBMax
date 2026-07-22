package com.ssbmax.shared.ui.piq.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.piq_age_years_months
import ssbmax.shared.generated.resources.piq_chances_availed
import ssbmax.shared.generated.resources.piq_choice_service
import ssbmax.shared.generated.resources.piq_commission_nature
import ssbmax.shared.generated.resources.piq_has_ncc
import ssbmax.shared.generated.resources.piq_height
import ssbmax.shared.generated.resources.piq_hobbies
import ssbmax.shared.generated.resources.piq_hobbies_placeholder
import ssbmax.shared.generated.resources.piq_interests_section
import ssbmax.shared.generated.resources.piq_monthly_income
import ssbmax.shared.generated.resources.piq_ncc_certificate
import ssbmax.shared.generated.resources.piq_ncc_division
import ssbmax.shared.generated.resources.piq_ncc_section
import ssbmax.shared.generated.resources.piq_ncc_total
import ssbmax.shared.generated.resources.piq_ncc_wing
import ssbmax.shared.generated.resources.piq_occupation_section
import ssbmax.shared.generated.resources.piq_physical_details
import ssbmax.shared.generated.resources.piq_positions
import ssbmax.shared.generated.resources.piq_positions_placeholder
import ssbmax.shared.generated.resources.piq_present_occupation
import ssbmax.shared.generated.resources.piq_responsibility_section
import ssbmax.shared.generated.resources.piq_service_section
import ssbmax.shared.generated.resources.piq_sports
import ssbmax.shared.generated.resources.piq_sports_placeholder
import ssbmax.shared.generated.resources.piq_sports_section
import ssbmax.shared.generated.resources.piq_weight

/**
 * KMP port of the Android original's `Page2Content()` from
 * `app/.../ui/tests/piq/PIQTestScreen.kt`: physical details, occupation, NCC
 * training, sports, hobbies, positions of responsibility, and service
 * selection. Note: physical details/occupation are on Page 2 in the Android
 * original despite being logically "personal" -- ported unchanged, matching
 * the exact SSB PIQ paper-form field ordering the comments there call out.
 */
@Composable
fun PIQPage2Fields(
    answers: Map<String, String>,
    onFieldChange: (String, String) -> Unit
) {
    PIQSectionHeader(stringResource(Res.string.piq_physical_details))
    PIQTextField(
        label = stringResource(Res.string.piq_age_years_months),
        value = answers["age"] ?: "",
        onValueChange = { onFieldChange("age", it) },
        keyboardType = KeyboardType.Number
    )
    PIQTextField(
        label = stringResource(Res.string.piq_height),
        value = answers["height"] ?: "",
        onValueChange = { onFieldChange("height", it) },
        keyboardType = KeyboardType.Decimal
    )
    PIQTextField(
        label = stringResource(Res.string.piq_weight),
        value = answers["weight"] ?: "",
        onValueChange = { onFieldChange("weight", it) },
        keyboardType = KeyboardType.Decimal
    )

    PIQSectionHeader(stringResource(Res.string.piq_occupation_section))
    PIQTextField(
        label = stringResource(Res.string.piq_present_occupation),
        value = answers["presentOccupation"] ?: "",
        onValueChange = { onFieldChange("presentOccupation", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_monthly_income),
        value = answers["personalMonthlyIncome"] ?: "",
        onValueChange = { onFieldChange("personalMonthlyIncome", it) },
        keyboardType = KeyboardType.Number
    )

    PIQSectionHeader(stringResource(Res.string.piq_ncc_section))
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = answers["ncc_hasTraining"]?.toBoolean() ?: false,
            onCheckedChange = { onFieldChange("ncc_hasTraining", it.toString()) }
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(stringResource(Res.string.piq_has_ncc))
    }
    if (answers["ncc_hasTraining"]?.toBoolean() == true) {
        PIQTextField(
            label = stringResource(Res.string.piq_ncc_total),
            value = answers["ncc_totalTraining"] ?: "",
            onValueChange = { onFieldChange("ncc_totalTraining", it) }
        )
        PIQDropdownField(
            label = stringResource(Res.string.piq_ncc_wing),
            value = answers["ncc_wing"] ?: "",
            options = listOf("", "Army", "Navy", "Air Force"),
            onValueChange = { onFieldChange("ncc_wing", it) }
        )
        PIQTextField(
            label = stringResource(Res.string.piq_ncc_division),
            value = answers["ncc_division"] ?: "",
            onValueChange = { onFieldChange("ncc_division", it) }
        )
        PIQTextField(
            label = stringResource(Res.string.piq_ncc_certificate),
            value = answers["ncc_certificate"] ?: "",
            onValueChange = { onFieldChange("ncc_certificate", it) }
        )
    }

    PIQSectionHeader(stringResource(Res.string.piq_sports_section))
    PIQTextField(
        label = stringResource(Res.string.piq_sports),
        value = answers["sports"] ?: "",
        onValueChange = { onFieldChange("sports", it) },
        multiline = true,
        minLines = 2,
        placeholder = stringResource(Res.string.piq_sports_placeholder)
    )

    PIQSectionHeader(stringResource(Res.string.piq_interests_section))
    PIQTextField(
        label = stringResource(Res.string.piq_hobbies),
        value = answers["hobbies"] ?: "",
        onValueChange = { onFieldChange("hobbies", it) },
        multiline = true,
        minLines = 3,
        placeholder = stringResource(Res.string.piq_hobbies_placeholder)
    )

    // Extra-curricular activities: no dedicated UI yet, same gap as the
    // Android original (only a plain text field exists there for hobbies/sports;
    // the structured `extraCurricularActivities` list has no "+Add" affordance).

    PIQSectionHeader(stringResource(Res.string.piq_responsibility_section))
    PIQTextField(
        label = stringResource(Res.string.piq_positions),
        value = answers["positionsOfResponsibility"] ?: "",
        onValueChange = { onFieldChange("positionsOfResponsibility", it) },
        multiline = true,
        minLines = 3,
        placeholder = stringResource(Res.string.piq_positions_placeholder)
    )

    PIQSectionHeader(stringResource(Res.string.piq_service_section))
    PIQTextField(
        label = stringResource(Res.string.piq_commission_nature),
        value = answers["natureOfCommission"] ?: "",
        onValueChange = { onFieldChange("natureOfCommission", it) }
    )
    PIQDropdownField(
        label = stringResource(Res.string.piq_choice_service),
        value = answers["choiceOfService"] ?: "",
        options = listOf("", "Army", "Navy", "Air Force", "Coast Guard", "Any"),
        onValueChange = { onFieldChange("choiceOfService", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_chances_availed),
        value = answers["chancesAvailed"] ?: "",
        onValueChange = { onFieldChange("chancesAvailed", it) },
        keyboardType = KeyboardType.Number
    )

    // Previous interviews: no dedicated UI yet, same gap as the Android original
    // (structured `previousInterviews` list has no "+Add" affordance there either).
}
