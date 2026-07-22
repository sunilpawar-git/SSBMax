package com.ssbmax.shared.ui.piq.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import ssbmax.shared.generated.resources.piq_monthly_income
import ssbmax.shared.generated.resources.piq_ncc_certificate
import ssbmax.shared.generated.resources.piq_ncc_division
import ssbmax.shared.generated.resources.piq_ncc_total
import ssbmax.shared.generated.resources.piq_ncc_wing
import ssbmax.shared.generated.resources.piq_positions
import ssbmax.shared.generated.resources.piq_present_occupation
import ssbmax.shared.generated.resources.piq_review_no
import ssbmax.shared.generated.resources.piq_review_yes
import ssbmax.shared.generated.resources.piq_sports
import ssbmax.shared.generated.resources.piq_weight

/**
 * Page 2 ("Career & Additional") summary section of
 * [com.ssbmax.shared.ui.piq.PIQReviewScreen]. See [PIQReviewPersonalSection]
 * for the string-resource-reuse rationale.
 */
@Composable
fun PIQReviewCareerSection(answers: Map<String, String>) {
    ReviewField(stringResource(Res.string.piq_age_years_months), answers["age"])
    ReviewField(stringResource(Res.string.piq_height), answers["height"])
    ReviewField(stringResource(Res.string.piq_weight), answers["weight"])
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    ReviewField(stringResource(Res.string.piq_present_occupation), answers["presentOccupation"])
    ReviewField(stringResource(Res.string.piq_monthly_income), answers["personalMonthlyIncome"])
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    ReviewField(stringResource(Res.string.piq_hobbies), answers["hobbies"])
    ReviewField(stringResource(Res.string.piq_sports), answers["sports"])
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    val nccLabel = if (answers["ncc_hasTraining"]?.toBoolean() == true) {
        stringResource(Res.string.piq_review_yes)
    } else {
        stringResource(Res.string.piq_review_no)
    }
    ReviewField(stringResource(Res.string.piq_has_ncc), nccLabel)
    ReviewField(stringResource(Res.string.piq_ncc_total), answers["ncc_totalTraining"])
    ReviewField(stringResource(Res.string.piq_ncc_wing), answers["ncc_wing"])
    ReviewField(stringResource(Res.string.piq_ncc_division), answers["ncc_division"])
    ReviewField(stringResource(Res.string.piq_ncc_certificate), answers["ncc_certificate"])
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    ReviewField(stringResource(Res.string.piq_positions), answers["positionsOfResponsibility"])
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

    ReviewField(stringResource(Res.string.piq_commission_nature), answers["natureOfCommission"])
    ReviewField(stringResource(Res.string.piq_choice_service), answers["choiceOfService"])
    ReviewField(stringResource(Res.string.piq_chances_availed), answers["chancesAvailed"])
}
