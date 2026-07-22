package com.ssbmax.shared.ui.piq.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.piq_age
import ssbmax.shared.generated.resources.piq_education
import ssbmax.shared.generated.resources.piq_elder_sibling
import ssbmax.shared.generated.resources.piq_father_education
import ssbmax.shared.generated.resources.piq_father_income
import ssbmax.shared.generated.resources.piq_father_name
import ssbmax.shared.generated.resources.piq_father_occupation
import ssbmax.shared.generated.resources.piq_father_section
import ssbmax.shared.generated.resources.piq_income
import ssbmax.shared.generated.resources.piq_mother_education
import ssbmax.shared.generated.resources.piq_mother_name
import ssbmax.shared.generated.resources.piq_mother_occupation
import ssbmax.shared.generated.resources.piq_mother_section
import ssbmax.shared.generated.resources.piq_occupation
import ssbmax.shared.generated.resources.piq_sibling_name
import ssbmax.shared.generated.resources.piq_sibling_name_younger
import ssbmax.shared.generated.resources.piq_siblings_section
import ssbmax.shared.generated.resources.piq_younger_sibling

/**
 * Page 1, part 2 of [com.ssbmax.shared.ui.piq.PIQTestScreen]: father/mother
 * and siblings sections. Siblings are exposed as 2 fixed elder + 2 fixed
 * younger slots in the flat `answers` map, matching the Android original --
 * no dynamic "+Add sibling" UI exists there either.
 */
@Composable
fun PIQPage1FamilyFields(
    answers: Map<String, String>,
    onFieldChange: (String, String) -> Unit
) {
    PIQSectionHeader(stringResource(Res.string.piq_father_section))
    PIQTextField(
        label = stringResource(Res.string.piq_father_name),
        value = answers["fatherName"] ?: "",
        onValueChange = { onFieldChange("fatherName", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_father_occupation),
        value = answers["fatherOccupation"] ?: "",
        onValueChange = { onFieldChange("fatherOccupation", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_father_education),
        value = answers["fatherEducation"] ?: "",
        onValueChange = { onFieldChange("fatherEducation", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_father_income),
        value = answers["fatherIncome"] ?: "",
        onValueChange = { onFieldChange("fatherIncome", it) }
    )

    PIQSectionHeader(stringResource(Res.string.piq_mother_section))
    PIQTextField(
        label = stringResource(Res.string.piq_mother_name),
        value = answers["motherName"] ?: "",
        onValueChange = { onFieldChange("motherName", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_mother_occupation),
        value = answers["motherOccupation"] ?: "",
        onValueChange = { onFieldChange("motherOccupation", it) }
    )
    PIQTextField(
        label = stringResource(Res.string.piq_mother_education),
        value = answers["motherEducation"] ?: "",
        onValueChange = { onFieldChange("motherEducation", it) }
    )

    PIQSectionHeader(stringResource(Res.string.piq_siblings_section))
    Text(
        stringResource(Res.string.piq_elder_sibling),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp)
    )
    repeat(2) { index -> SiblingFields(answers, "elderSibling${index + 1}_", stringResource(Res.string.piq_sibling_name, index + 1), onFieldChange) }

    Text(
        stringResource(Res.string.piq_younger_sibling),
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(top = 8.dp)
    )
    repeat(2) { index -> SiblingFields(answers, "youngerSibling${index + 1}_", stringResource(Res.string.piq_sibling_name_younger, index + 1), onFieldChange) }
}

@Composable
private fun SiblingFields(
    answers: Map<String, String>,
    prefix: String,
    nameLabel: String,
    onFieldChange: (String, String) -> Unit
) {
    PIQTextField(
        label = nameLabel,
        value = answers["${prefix}name"] ?: "",
        onValueChange = { onFieldChange("${prefix}name", it) }
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PIQTextField(
            label = stringResource(Res.string.piq_age),
            value = answers["${prefix}age"] ?: "",
            onValueChange = { onFieldChange("${prefix}age", it) },
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )
        PIQTextField(
            label = stringResource(Res.string.piq_education),
            value = answers["${prefix}education"] ?: "",
            onValueChange = { onFieldChange("${prefix}education", it) },
            modifier = Modifier.weight(1f)
        )
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PIQTextField(
            label = stringResource(Res.string.piq_occupation),
            value = answers["${prefix}occupation"] ?: "",
            onValueChange = { onFieldChange("${prefix}occupation", it) },
            keyboardType = KeyboardType.Text,
            modifier = Modifier.weight(1f)
        )
        PIQTextField(
            label = stringResource(Res.string.piq_income),
            value = answers["${prefix}income"] ?: "",
            onValueChange = { onFieldChange("${prefix}income", it) },
            keyboardType = KeyboardType.Number,
            modifier = Modifier.weight(1f)
        )
    }
}
