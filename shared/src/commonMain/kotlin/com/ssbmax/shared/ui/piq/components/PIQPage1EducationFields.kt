package com.ssbmax.shared.ui.piq.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.piq_10th_standard
import ssbmax.shared.generated.resources.piq_12th_standard
import ssbmax.shared.generated.resources.piq_achievement
import ssbmax.shared.generated.resources.piq_boarder
import ssbmax.shared.generated.resources.piq_board
import ssbmax.shared.generated.resources.piq_cgpa
import ssbmax.shared.generated.resources.piq_college_name
import ssbmax.shared.generated.resources.piq_degree
import ssbmax.shared.generated.resources.piq_degree_diploma
import ssbmax.shared.generated.resources.piq_educational_record
import ssbmax.shared.generated.resources.piq_graduation
import ssbmax.shared.generated.resources.piq_institution_name
import ssbmax.shared.generated.resources.piq_medium
import ssbmax.shared.generated.resources.piq_percentage
import ssbmax.shared.generated.resources.piq_post_graduation
import ssbmax.shared.generated.resources.piq_school_name
import ssbmax.shared.generated.resources.piq_stream
import ssbmax.shared.generated.resources.piq_university
import ssbmax.shared.generated.resources.piq_year_passing

/**
 * Page 1, part 3 (final part) of [com.ssbmax.shared.ui.piq.PIQTestScreen]:
 * 10th/12th/Graduation/Post-Graduation educational record -- moved to the
 * end of Page 1 in the Android original (not Page 2), ported unchanged.
 */
@Composable
fun PIQPage1EducationFields(
    answers: Map<String, String>,
    onFieldChange: (String, String) -> Unit
) {
    PIQSectionHeader(stringResource(Res.string.piq_educational_record))

    Text(stringResource(Res.string.piq_10th_standard), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    EducationBlock(answers, "education10th_", onFieldChange)

    Text(stringResource(Res.string.piq_12th_standard), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    PIQTextField(
        label = stringResource(Res.string.piq_school_name),
        value = answers["education12th_institution"] ?: "",
        onValueChange = { onFieldChange("education12th_institution", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_board),
        value = answers["education12th_board"] ?: "",
        onValueChange = { onFieldChange("education12th_board", it) }
    )
    PIQDropdownField(
        label = stringResource(Res.string.piq_stream),
        value = answers["education12th_stream"] ?: "",
        options = listOf("", "Science", "Commerce", "Arts"),
        onValueChange = { onFieldChange("education12th_stream", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_year_passing),
        value = answers["education12th_year"] ?: "",
        onValueChange = { onFieldChange("education12th_year", it) },
        keyboardType = KeyboardType.Number
    )
    PIQTextField(
        label = stringResource(Res.string.piq_percentage),
        value = answers["education12th_percentage"] ?: "",
        onValueChange = { onFieldChange("education12th_percentage", it) },
        keyboardType = KeyboardType.Decimal
    )
    PIQTextField(
        label = stringResource(Res.string.piq_medium),
        value = answers["education12th_medium"] ?: "",
        onValueChange = { onFieldChange("education12th_medium", it) }
    )
    PIQDropdownField(
        label = stringResource(Res.string.piq_boarder),
        value = answers["education12th_boarder"] ?: "",
        options = listOf("", "Boarder", "Day Scholar"),
        onValueChange = { onFieldChange("education12th_boarder", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_achievement),
        value = answers["education12th_achievement"] ?: "",
        onValueChange = { onFieldChange("education12th_achievement", it) },
        multiline = true,
        minLines = 2
    )

    Text(stringResource(Res.string.piq_graduation), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    PIQTextField(
        label = stringResource(Res.string.piq_college_name),
        value = answers["educationGrad_institution"] ?: "",
        onValueChange = { onFieldChange("educationGrad_institution", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_university),
        value = answers["educationGrad_university"] ?: "",
        onValueChange = { onFieldChange("educationGrad_university", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_degree),
        value = answers["educationGrad_degree"] ?: "",
        onValueChange = { onFieldChange("educationGrad_degree", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_year_passing),
        value = answers["educationGrad_year"] ?: "",
        onValueChange = { onFieldChange("educationGrad_year", it) },
        keyboardType = KeyboardType.Number
    )
    PIQTextField(
        label = stringResource(Res.string.piq_cgpa),
        value = answers["educationGrad_cgpa"] ?: "",
        onValueChange = { onFieldChange("educationGrad_cgpa", it) },
        keyboardType = KeyboardType.Decimal
    )
    PIQTextField(
        label = stringResource(Res.string.piq_medium),
        value = answers["educationGrad_medium"] ?: "",
        onValueChange = { onFieldChange("educationGrad_medium", it) }
    )
    PIQDropdownField(
        label = stringResource(Res.string.piq_boarder),
        value = answers["educationGrad_boarder"] ?: "",
        options = listOf("", "Boarder", "Day Scholar"),
        onValueChange = { onFieldChange("educationGrad_boarder", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_achievement),
        value = answers["educationGrad_achievement"] ?: "",
        onValueChange = { onFieldChange("educationGrad_achievement", it) },
        multiline = true,
        minLines = 2
    )

    Text(stringResource(Res.string.piq_post_graduation), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
    PIQTextField(
        label = stringResource(Res.string.piq_institution_name),
        value = answers["educationPG_institution"] ?: "",
        onValueChange = { onFieldChange("educationPG_institution", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_university),
        value = answers["educationPG_university"] ?: "",
        onValueChange = { onFieldChange("educationPG_university", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_degree_diploma),
        value = answers["educationPG_degree"] ?: "",
        onValueChange = { onFieldChange("educationPG_degree", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_year_passing),
        value = answers["educationPG_year"] ?: "",
        onValueChange = { onFieldChange("educationPG_year", it) },
        keyboardType = KeyboardType.Number
    )
    PIQTextField(
        label = stringResource(Res.string.piq_cgpa),
        value = answers["educationPG_cgpa"] ?: "",
        onValueChange = { onFieldChange("educationPG_cgpa", it) },
        keyboardType = KeyboardType.Decimal
    )
    PIQTextField(
        label = stringResource(Res.string.piq_medium),
        value = answers["educationPG_medium"] ?: "",
        onValueChange = { onFieldChange("educationPG_medium", it) }
    )
    PIQDropdownField(
        label = stringResource(Res.string.piq_boarder),
        value = answers["educationPG_boarder"] ?: "",
        options = listOf("", "Boarder", "Day Scholar"),
        onValueChange = { onFieldChange("educationPG_boarder", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_achievement),
        value = answers["educationPG_achievement"] ?: "",
        onValueChange = { onFieldChange("educationPG_achievement", it) },
        multiline = true,
        minLines = 2
    )
}

@Composable
private fun EducationBlock(
    answers: Map<String, String>,
    prefix: String,
    onFieldChange: (String, String) -> Unit
) {
    PIQTextField(
        label = stringResource(Res.string.piq_school_name),
        value = answers["${prefix}institution"] ?: "",
        onValueChange = { onFieldChange("${prefix}institution", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_board),
        value = answers["${prefix}board"] ?: "",
        onValueChange = { onFieldChange("${prefix}board", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_year_passing),
        value = answers["${prefix}year"] ?: "",
        onValueChange = { onFieldChange("${prefix}year", it) },
        keyboardType = KeyboardType.Number
    )
    PIQTextField(
        label = stringResource(Res.string.piq_percentage),
        value = answers["${prefix}percentage"] ?: "",
        onValueChange = { onFieldChange("${prefix}percentage", it) },
        keyboardType = KeyboardType.Decimal
    )
    PIQTextField(
        label = stringResource(Res.string.piq_medium),
        value = answers["${prefix}medium"] ?: "",
        onValueChange = { onFieldChange("${prefix}medium", it) }
    )
    PIQDropdownField(
        label = stringResource(Res.string.piq_boarder),
        value = answers["${prefix}boarder"] ?: "",
        options = listOf("", "Boarder", "Day Scholar"),
        onValueChange = { onFieldChange("${prefix}boarder", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_achievement),
        value = answers["${prefix}achievement"] ?: "",
        onValueChange = { onFieldChange("${prefix}achievement", it) },
        multiline = true,
        minLines = 2
    )
}
