package com.ssbmax.ui.tests.piq

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ssbmax.R
import com.ssbmax.core.domain.model.PIQPage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PIQReviewScreen(
    answers: Map<String, String>,
    onEdit: (PIQPage) -> Unit,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    var showSubmitDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.piq_review_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, stringResource(R.string.cd_back))
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = 8.dp,
                tonalElevation = 3.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = { showSubmitDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.piq_review_action_submit))
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Completion warning - removed since all fields are optional
            // No validation needed for lenient form

            // Page 1 Summary - Exact sequence matching SSB PIQ form
            ReviewSection(
                title = stringResource(R.string.piq_review_section_personal_family),
                onEdit = { onEdit(PIQPage.PAGE_1) }
            ) {
                // Header Information (Top of form)
                ReviewField("OIR Number", answers["oirNumber"])
                ReviewField("Selection Board", answers["selectionBoard"])
                ReviewField("Batch No", answers["batchNumber"])
                ReviewField("Chest Number", answers["chestNumber"])
                ReviewField("UPSC Roll No", answers["upscRollNumber"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Personal Information (Table format in actual form)
                ReviewField("Full Name", answers["fullName"])
                ReviewField("Date of Birth", answers["dateOfBirth"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Personal Details Table (as per SSB PIQ form)
                ReviewField("State", answers["state"])
                ReviewField("District", answers["district"])
                ReviewField("Religion", answers["religion"])
                ReviewField("SC/ST/OBC Status", answers["scStObcStatus"])
                ReviewField("Mother Tongue", answers["motherTongue"])
                ReviewField("Marital Status", answers["maritalStatus"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Residence Information
                ReviewField("Permanent Address", answers["permanentAddress"])
                ReviewField("Present Address", answers["presentAddress"])
                ReviewField("Maximum Residence", answers["maximumResidence"])
                ReviewField("Maximum Residence Population", answers["maximumResidencePopulation"])
                ReviewField("Present Residence Population", answers["presentResidencePopulation"])
                ReviewField("Permanent Residence Population", answers["permanentResidencePopulation"])
                if (answers["isDistrictHQ"]?.toBoolean() == true) {
                    ReviewField("Is District HQ", stringResource(R.string.piq_review_yes))
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Father's Information
                ReviewField("Father's Name", answers["fatherName"])
                ReviewField("Father's Occupation", answers["fatherOccupation"])
                ReviewField("Father's Education", answers["fatherEducation"])
                ReviewField("Father's Income", answers["fatherIncome"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Mother's Information
                ReviewField("Mother's Name", answers["motherName"])
                ReviewField("Mother's Occupation", answers["motherOccupation"])
                ReviewField("Mother's Education", answers["motherEducation"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Siblings Information
                Text(stringResource(R.string.piq_review_field_siblings), style = MaterialTheme.typography.titleSmall)
                repeat(2) { index ->
                    val prefix = "elderSibling${index + 1}_"
                    val name = answers["${prefix}name"]
                    if (!name.isNullOrBlank()) {
                        ReviewField("Elder Sibling ${index + 1} - Name", name)
                        ReviewField("  Age", answers["${prefix}age"])
                        ReviewField("  Education", answers["${prefix}education"])
                        ReviewField("  Occupation", answers["${prefix}occupation"])
                        ReviewField("  Income", answers["${prefix}income"])
                    }
                }
                repeat(2) { index ->
                    val prefix = "youngerSibling${index + 1}_"
                    val name = answers["${prefix}name"]
                    if (!name.isNullOrBlank()) {
                        ReviewField("Younger Sibling ${index + 1} - Name", name)
                        ReviewField("  Age", answers["${prefix}age"])
                        ReviewField("  Education", answers["${prefix}education"])
                        ReviewField("  Occupation", answers["${prefix}occupation"])
                        ReviewField("  Income", answers["${prefix}income"])
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Family Enhancement
                ReviewField("Parents Alive", answers["parentsAlive"])
                ReviewField("Age at Father's Death", answers["ageAtFatherDeath"])
                ReviewField("Age at Mother's Death", answers["ageAtMotherDeath"])
                if (answers["parentsAlive"] == "None") {
                    ReviewField("Guardian Name", answers["guardianName"])
                    ReviewField("Guardian Occupation", answers["guardianOccupation"])
                    ReviewField("Guardian Education", answers["guardianEducation"])
                    ReviewField("Guardian Income", answers["guardianIncome"])
                }
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Educational Record - moved from Page 2 to Page 1
                Text(stringResource(R.string.piq_review_field_educational_record), style = MaterialTheme.typography.titleSmall)

                Text(stringResource(R.string.piq_review_field_10th), style = MaterialTheme.typography.titleSmall)
                ReviewField("School Name", answers["education10th_institution"])
                ReviewField("Board", answers["education10th_board"])
                ReviewField("Year", answers["education10th_year"])
                ReviewField("Percentage", answers["education10th_percentage"])
                ReviewField("Medium of Instruction", answers["education10th_medium"])
                ReviewField("Boarder/Day Scholar", answers["education10th_boarder"])
                ReviewField("Outstanding Achievement", answers["education10th_achievement"])

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(stringResource(R.string.piq_review_field_12th), style = MaterialTheme.typography.titleSmall)
                ReviewField("School Name", answers["education12th_institution"])
                ReviewField("Board", answers["education12th_board"])
                ReviewField("Stream", answers["education12th_stream"])
                ReviewField("Year", answers["education12th_year"])
                ReviewField("Percentage", answers["education12th_percentage"])
                ReviewField("Medium of Instruction", answers["education12th_medium"])
                ReviewField("Boarder/Day Scholar", answers["education12th_boarder"])
                ReviewField("Outstanding Achievement", answers["education12th_achievement"])

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(stringResource(R.string.piq_review_field_graduation), style = MaterialTheme.typography.titleSmall)
                ReviewField("College Name", answers["educationGrad_institution"])
                ReviewField("University", answers["educationGrad_university"])
                ReviewField("Degree", answers["educationGrad_degree"])
                ReviewField("Year", answers["educationGrad_year"])
                ReviewField("CGPA/Percentage", answers["educationGrad_cgpa"])
                ReviewField("Medium of Instruction", answers["educationGrad_medium"])
                ReviewField("Boarder/Day Scholar", answers["educationGrad_boarder"])
                ReviewField("Outstanding Achievement", answers["educationGrad_achievement"])

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                Text(stringResource(R.string.piq_review_field_pg), style = MaterialTheme.typography.titleSmall)
                ReviewField("Institution Name", answers["educationPG_institution"])
                ReviewField("University", answers["educationPG_university"])
                ReviewField("Degree/Diploma", answers["educationPG_degree"])
                ReviewField("Year", answers["educationPG_year"])
                ReviewField("CGPA/Percentage", answers["educationPG_cgpa"])
                ReviewField("Medium of Instruction", answers["educationPG_medium"])
                ReviewField("Boarder/Day Scholar", answers["educationPG_boarder"])
                ReviewField("Outstanding Achievement", answers["educationPG_achievement"])
            }

            // Page 2 Summary - Exact sequence matching SSB PIQ form
            ReviewSection(
                title = stringResource(R.string.piq_review_section_career),
                onEdit = { onEdit(PIQPage.PAGE_2) }
            ) {
                // Physical Details - moved from Page 1 to Page 2
                ReviewField("Age (Years & Months)", answers["age"])
                ReviewField("Height (metres)", answers["height"])
                ReviewField("Weight (kilograms)", answers["weight"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Occupation - moved from Page 1 to Page 2
                ReviewField("Present Occupation", answers["presentOccupation"])
                ReviewField("Personal Monthly Income", answers["personalMonthlyIncome"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Interests
                ReviewField("Hobbies & Interests", answers["hobbies"])
                ReviewField("Sports", answers["sports"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // NCC Training
                ReviewField("Has NCC Training", if (answers["ncc_hasTraining"]?.toBoolean() == true) "Yes" else "No")
                ReviewField("Total Training", answers["ncc_totalTraining"])
                ReviewField("Wing", answers["ncc_wing"])
                ReviewField("Division", answers["ncc_division"])
                ReviewField("Certificate Obtained", answers["ncc_certificate"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Positions of Responsibility
                ReviewField("Positions Held", answers["positionsOfResponsibility"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                
                // Service Selection
                ReviewField("Nature of Commission", answers["natureOfCommission"])
                ReviewField("Choice of Service", answers["choiceOfService"])
                ReviewField("Number of Chances Availed", answers["chancesAvailed"])
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Submit confirmation dialog
        if (showSubmitDialog) {
            AlertDialog(
                onDismissRequest = { showSubmitDialog = false },
                title = { Text(stringResource(R.string.piq_review_dialog_title)) },
                text = {
                    Text(stringResource(R.string.piq_review_dialog_message))
                },
                confirmButton = {
                    Button(onClick = {
                        showSubmitDialog = false
                        onSubmit()
                    }) {
                        Text(stringResource(R.string.action_submit))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSubmitDialog = false }) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}

@Composable
private fun ReviewSection(
    title: String,
    onEdit: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, stringResource(R.string.action_edit))
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun ReviewField(label: String, value: String?) {
    if (!value.isNullOrBlank()) {
        Column(modifier = Modifier.padding(vertical = 4.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

