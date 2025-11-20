package com.ssbmax.ui.tests.piq

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R
import com.ssbmax.core.domain.model.PIQPage
import com.ssbmax.ui.tests.piq.SELECTION_BOARD_OPTIONS
import com.ssbmax.ui.tests.piq.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PIQTestScreen(
    testId: String,
    onNavigateBack: () -> Unit,
    onNavigateToResult: (String) -> Unit = {},
    viewModel: PIQTestViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Navigate to result screen on submission complete (outside if-else to always execute)
    if (uiState.submissionComplete) {
        LaunchedEffect(Unit) {
            uiState.submissionId?.let { submissionId ->
                onNavigateToResult(submissionId)
            } ?: onNavigateBack()
        }
    }

    // Show review screen or form
    if (uiState.showReviewScreen) {
        PIQReviewScreen(
            answers = uiState.answers,
            onEdit = { page -> viewModel.editPage(page) },
            onSubmit = { viewModel.submitTest() },
            onBack = { viewModel.navigateToPage(PIQPage.PAGE_2) }
        )
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("PIQ - ${uiState.currentPage.displayName}") },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, stringResource(R.string.piq_back))
                        }
                    },
                    actions = {
                        // Show save indicator
                        if (uiState.isSaving) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(end = 8.dp),
                                strokeWidth = 2.dp
                            )
                        } else if (uiState.lastSavedAt != null) {
                            Text(
                                stringResource(R.string.piq_saved),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 16.dp)
                            )
                        }
                    }
                )
            },
            bottomBar = {
                PIQNavigationBar(
                    currentPage = uiState.currentPage,
                    onPreviousPage = {
                        if (uiState.currentPage == PIQPage.PAGE_2) {
                            viewModel.navigateToPage(PIQPage.PAGE_1)
                        }
                    },
                    onNextPage = {
                        if (uiState.currentPage == PIQPage.PAGE_1) {
                            viewModel.navigateToPage(PIQPage.PAGE_2)
                        } else {
                            viewModel.goToReview()
                        }
                    },
                    canGoBack = uiState.currentPage == PIQPage.PAGE_2,
                    nextButtonText = if (uiState.currentPage == PIQPage.PAGE_2) stringResource(R.string.piq_review) else stringResource(R.string.piq_next)
                )
            }
        ) { padding ->
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                when (uiState.currentPage) {
                    PIQPage.PAGE_1 -> Page1Content(
                        answers = uiState.answers,
                        onFieldChange = { fieldId, value ->
                            viewModel.updateField(fieldId, value)
                        },
                        modifier = Modifier.padding(padding)
                    )
                    PIQPage.PAGE_2 -> Page2Content(
                        answers = uiState.answers,
                        onFieldChange = { fieldId, value ->
                            viewModel.updateField(fieldId, value)
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            // Show error if any
            uiState.error?.let { error ->
                AlertDialog(
                    onDismissRequest = { viewModel.clearError() },
                    title = { Text(stringResource(R.string.piq_error)) },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text(stringResource(R.string.piq_ok))
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun Page1Content(
    answers: Map<String, String>,
    onFieldChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Privacy Warning Banner
        PIQPrivacyWarningBanner()

        Text(
            stringResource(R.string.piq_personal_family_title),
            style = MaterialTheme.typography.headlineSmall
        )

        // Header Section (Exact SSB PIQ sequence)
        PIQSectionHeader(stringResource(R.string.piq_header_section))
        PIQTextField(
            label = stringResource(R.string.piq_oir_number),
            value = answers["oirNumber"] ?: "",
            onValueChange = { onFieldChange("oirNumber", it) },
            placeholder = stringResource(R.string.piq_oir_placeholder),
            enabled = false // Auto-filled, read-only
        )
        PIQDropdownField(
            label = stringResource(R.string.piq_selection_board),
            value = answers["selectionBoard"] ?: "",
            options = SELECTION_BOARD_OPTIONS,
            onValueChange = { onFieldChange("selectionBoard", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_batch_number),
            value = answers["batchNumber"] ?: "",
            onValueChange = { onFieldChange("batchNumber", it) },
            placeholder = stringResource(R.string.piq_batch_placeholder),
            enabled = false,
            supportingText = stringResource(R.string.piq_batch_supporting)
        )
        PIQTextField(
            label = stringResource(R.string.piq_chest_number),
            value = answers["chestNumber"] ?: "",
            onValueChange = { onFieldChange("chestNumber", it) },
            placeholder = stringResource(R.string.piq_batch_placeholder),
            enabled = false,
            supportingText = stringResource(R.string.piq_batch_supporting)
        )
        PIQTextField(
            label = stringResource(R.string.piq_upsc_roll),
            value = answers["upscRollNumber"] ?: "",
            onValueChange = { onFieldChange("upscRollNumber", it) },
            placeholder = stringResource(R.string.piq_batch_placeholder),
            enabled = false,
            supportingText = stringResource(R.string.piq_batch_supporting)
        )

        // Name (in CAPITALS) - comes right after header as per SSB PIQ
        PIQTextField(
            label = stringResource(R.string.piq_full_name),
            value = answers["fullName"] ?: "",
            onValueChange = { onFieldChange("fullName", it) }
        )

        // Residence Information - comes after Name as per SSB PIQ
        PIQSectionHeader(stringResource(R.string.piq_residence_section))
        PIQTextField(
            label = stringResource(R.string.piq_max_residence),
            value = answers["maximumResidence"] ?: "",
            onValueChange = { onFieldChange("maximumResidence", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_max_residence_pop),
            value = answers["maximumResidencePopulation"] ?: "",
            onValueChange = { onFieldChange("maximumResidencePopulation", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = stringResource(R.string.piq_present_address),
            value = answers["presentAddress"] ?: "",
            onValueChange = { onFieldChange("presentAddress", it) },
            multiline = true,
            minLines = 3
        )
        PIQTextField(
            label = stringResource(R.string.piq_present_pop),
            value = answers["presentResidencePopulation"] ?: "",
            onValueChange = { onFieldChange("presentResidencePopulation", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = stringResource(R.string.piq_permanent_address),
            value = answers["permanentAddress"] ?: "",
            onValueChange = { onFieldChange("permanentAddress", it) },
            multiline = true,
            minLines = 3
        )
        PIQTextField(
            label = stringResource(R.string.piq_permanent_pop),
            value = answers["permanentResidencePopulation"] ?: "",
            onValueChange = { onFieldChange("permanentResidencePopulation", it) },
            keyboardType = KeyboardType.Number
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = answers["isDistrictHQ"]?.toBoolean() ?: false,
                onCheckedChange = { onFieldChange("isDistrictHQ", it.toString()) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.piq_is_district_hq))
        }
        
        // Personal Details Table - comes after Residence as per SSB PIQ
        PIQSectionHeader(stringResource(R.string.piq_personal_details))
        PIQTextField(
            label = stringResource(R.string.piq_state),
            value = answers["state"] ?: "",
            onValueChange = { onFieldChange("state", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_district),
            value = answers["district"] ?: "",
            onValueChange = { onFieldChange("district", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_religion),
            value = answers["religion"] ?: "",
            onValueChange = { onFieldChange("religion", it) }
        )
        PIQDropdownField(
            label = stringResource(R.string.piq_sc_st_obc),
            value = answers["scStObcStatus"] ?: "",
            options = listOf("", "SC", "ST", "OBC"),
            onValueChange = { onFieldChange("scStObcStatus", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_mother_tongue),
            value = answers["motherTongue"] ?: "",
            onValueChange = { onFieldChange("motherTongue", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_date_of_birth),
            value = answers["dateOfBirth"] ?: "",
            onValueChange = { onFieldChange("dateOfBirth", it) },
            placeholder = stringResource(R.string.piq_dob_placeholder)
        )
        PIQDropdownField(
            label = stringResource(R.string.piq_marital_status),
            value = answers["maritalStatus"] ?: "",
            options = listOf("", "Single", "Married", "Widower"),
            onValueChange = { onFieldChange("maritalStatus", it) }
        )
        
        // Parents Alive - comes after Personal Details Table as per SSB PIQ
        PIQSectionHeader(stringResource(R.string.piq_family_enhancement))
        PIQDropdownField(
            label = stringResource(R.string.piq_parents_alive),
            value = answers["parentsAlive"] ?: "",
            options = listOf("", "Both", "Father Only", "Mother Only", "None"),
            onValueChange = { onFieldChange("parentsAlive", it) }
        )

        // Conditional fields based on parentsAlive
        if (answers["parentsAlive"] == "Mother Only" || answers["parentsAlive"] == "None") {
            PIQTextField(
                label = stringResource(R.string.piq_age_father_death),
                value = answers["ageAtFatherDeath"] ?: "",
                onValueChange = { onFieldChange("ageAtFatherDeath", it) },
                keyboardType = KeyboardType.Number
            )
        }
        if (answers["parentsAlive"] == "Father Only" || answers["parentsAlive"] == "None") {
            PIQTextField(
                label = stringResource(R.string.piq_age_mother_death),
                value = answers["ageAtMotherDeath"] ?: "",
                onValueChange = { onFieldChange("ageAtMotherDeath", it) },
                keyboardType = KeyboardType.Number
            )
        }
        if (answers["parentsAlive"] == "None") {
            PIQSectionHeader(stringResource(R.string.piq_guardian_section))
            PIQTextField(
                label = stringResource(R.string.piq_guardian_name),
                value = answers["guardianName"] ?: "",
                onValueChange = { onFieldChange("guardianName", it) }
            )
            PIQTextField(
                label = stringResource(R.string.piq_guardian_occupation),
                value = answers["guardianOccupation"] ?: "",
                onValueChange = { onFieldChange("guardianOccupation", it) }
            )
            PIQTextField(
                label = stringResource(R.string.piq_guardian_education),
                value = answers["guardianEducation"] ?: "",
                onValueChange = { onFieldChange("guardianEducation", it) }
            )
            PIQTextField(
                label = stringResource(R.string.piq_guardian_income),
                value = answers["guardianIncome"] ?: "",
                onValueChange = { onFieldChange("guardianIncome", it) }
            )
        }
        
        // Parents/Guardian/Siblings table (Education, Occupation, Income)
        // Father's Information
        PIQSectionHeader(stringResource(R.string.piq_father_section))
        PIQTextField(
            label = stringResource(R.string.piq_father_name),
            value = answers["fatherName"] ?: "",
            onValueChange = { onFieldChange("fatherName", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_father_occupation),
            value = answers["fatherOccupation"] ?: "",
            onValueChange = { onFieldChange("fatherOccupation", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_father_education),
            value = answers["fatherEducation"] ?: "",
            onValueChange = { onFieldChange("fatherEducation", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_father_income),
            value = answers["fatherIncome"] ?: "",
            onValueChange = { onFieldChange("fatherIncome", it) }
        )

        // Mother's Information
        PIQSectionHeader(stringResource(R.string.piq_mother_section))
        PIQTextField(
            label = stringResource(R.string.piq_mother_name),
            value = answers["motherName"] ?: "",
            onValueChange = { onFieldChange("motherName", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_mother_occupation),
            value = answers["motherOccupation"] ?: "",
            onValueChange = { onFieldChange("motherOccupation", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_mother_education),
            value = answers["motherEducation"] ?: "",
            onValueChange = { onFieldChange("motherEducation", it) }
        )
        
        // Siblings Information - comes after Mother's Information as per SSB PIQ
        PIQSectionHeader(stringResource(R.string.piq_siblings_section))
        Text(
            stringResource(R.string.piq_elder_sibling),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        // Note: Siblings are managed as a dynamic list in ViewModel state
        // For now, we'll add fields for up to 2 elder siblings
        repeat(2) { index ->
            val prefix = "elderSibling${index + 1}_"
            PIQTextField(
                label = stringResource(R.string.piq_sibling_name, index + 1),
                value = answers["${prefix}name"] ?: "",
                onValueChange = { onFieldChange("${prefix}name", it) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PIQTextField(
                    label = stringResource(R.string.piq_age),
                    value = answers["${prefix}age"] ?: "",
                    onValueChange = { onFieldChange("${prefix}age", it) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                PIQTextField(
                    label = stringResource(R.string.piq_education),
                    value = answers["${prefix}education"] ?: "",
                    onValueChange = { onFieldChange("${prefix}education", it) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PIQTextField(
                    label = stringResource(R.string.piq_occupation),
                    value = answers["${prefix}occupation"] ?: "",
                    onValueChange = { onFieldChange("${prefix}occupation", it) },
                    modifier = Modifier.weight(1f)
                )
                PIQTextField(
                    label = stringResource(R.string.piq_income),
                    value = answers["${prefix}income"] ?: "",
                    onValueChange = { onFieldChange("${prefix}income", it) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Text(
            stringResource(R.string.piq_younger_sibling),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        // Add fields for up to 2 younger siblings
        repeat(2) { index ->
            val prefix = "youngerSibling${index + 1}_"
            PIQTextField(
                label = stringResource(R.string.piq_sibling_name_younger, index + 1),
                value = answers["${prefix}name"] ?: "",
                onValueChange = { onFieldChange("${prefix}name", it) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PIQTextField(
                    label = stringResource(R.string.piq_age),
                    value = answers["${prefix}age"] ?: "",
                    onValueChange = { onFieldChange("${prefix}age", it) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                PIQTextField(
                    label = stringResource(R.string.piq_education),
                    value = answers["${prefix}education"] ?: "",
                    onValueChange = { onFieldChange("${prefix}education", it) },
                    modifier = Modifier.weight(1f)
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PIQTextField(
                    label = stringResource(R.string.piq_occupation),
                    value = answers["${prefix}occupation"] ?: "",
                    onValueChange = { onFieldChange("${prefix}occupation", it) },
                    modifier = Modifier.weight(1f)
                )
                PIQTextField(
                    label = stringResource(R.string.piq_income),
                    value = answers["${prefix}income"] ?: "",
                    onValueChange = { onFieldChange("${prefix}income", it) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Educational Record - moved from Page 2 to end of Page 1
        PIQSectionHeader(stringResource(R.string.piq_educational_record))

        // 10th Standard Education
        Text(stringResource(R.string.piq_10th_standard), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        PIQTextField(
            label = stringResource(R.string.piq_school_name),
            value = answers["education10th_institution"] ?: "",
            onValueChange = { onFieldChange("education10th_institution", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_board),
            value = answers["education10th_board"] ?: "",
            onValueChange = { onFieldChange("education10th_board", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_year_passing),
            value = answers["education10th_year"] ?: "",
            onValueChange = { onFieldChange("education10th_year", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = stringResource(R.string.piq_percentage),
            value = answers["education10th_percentage"] ?: "",
            onValueChange = { onFieldChange("education10th_percentage", it) },
            keyboardType = KeyboardType.Decimal
        )
        PIQTextField(
            label = stringResource(R.string.piq_medium),
            value = answers["education10th_medium"] ?: "",
            onValueChange = { onFieldChange("education10th_medium", it) }
        )
        PIQDropdownField(
            label = stringResource(R.string.piq_boarder),
            value = answers["education10th_boarder"] ?: "",
            options = listOf("", "Boarder", "Day Scholar"),
            onValueChange = { onFieldChange("education10th_boarder", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_achievement),
            value = answers["education10th_achievement"] ?: "",
            onValueChange = { onFieldChange("education10th_achievement", it) },
            multiline = true,
            minLines = 2
        )
        
        // 12th Standard Education
        Text(stringResource(R.string.piq_12th_standard), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        PIQTextField(
            label = stringResource(R.string.piq_school_name),
            value = answers["education12th_institution"] ?: "",
            onValueChange = { onFieldChange("education12th_institution", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_board),
            value = answers["education12th_board"] ?: "",
            onValueChange = { onFieldChange("education12th_board", it) }
        )
        PIQDropdownField(
            label = stringResource(R.string.piq_stream),
            value = answers["education12th_stream"] ?: "",
            options = listOf("", "Science", "Commerce", "Arts"),
            onValueChange = { onFieldChange("education12th_stream", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_year_passing),
            value = answers["education12th_year"] ?: "",
            onValueChange = { onFieldChange("education12th_year", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = stringResource(R.string.piq_percentage),
            value = answers["education12th_percentage"] ?: "",
            onValueChange = { onFieldChange("education12th_percentage", it) },
            keyboardType = KeyboardType.Decimal
        )
        PIQTextField(
            label = stringResource(R.string.piq_medium),
            value = answers["education12th_medium"] ?: "",
            onValueChange = { onFieldChange("education12th_medium", it) }
        )
        PIQDropdownField(
            label = stringResource(R.string.piq_boarder),
            value = answers["education12th_boarder"] ?: "",
            options = listOf("", "Boarder", "Day Scholar"),
            onValueChange = { onFieldChange("education12th_boarder", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_achievement),
            value = answers["education12th_achievement"] ?: "",
            onValueChange = { onFieldChange("education12th_achievement", it) },
            multiline = true,
            minLines = 2
        )
        
        // Graduation Education
        Text(stringResource(R.string.piq_graduation), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        PIQTextField(
            label = stringResource(R.string.piq_college_name),
            value = answers["educationGrad_institution"] ?: "",
            onValueChange = { onFieldChange("educationGrad_institution", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_university),
            value = answers["educationGrad_university"] ?: "",
            onValueChange = { onFieldChange("educationGrad_university", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_degree),
            value = answers["educationGrad_degree"] ?: "",
            onValueChange = { onFieldChange("educationGrad_degree", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_year_passing),
            value = answers["educationGrad_year"] ?: "",
            onValueChange = { onFieldChange("educationGrad_year", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = stringResource(R.string.piq_cgpa),
            value = answers["educationGrad_cgpa"] ?: "",
            onValueChange = { onFieldChange("educationGrad_cgpa", it) },
            keyboardType = KeyboardType.Decimal
        )
        PIQTextField(
            label = stringResource(R.string.piq_medium),
            value = answers["educationGrad_medium"] ?: "",
            onValueChange = { onFieldChange("educationGrad_medium", it) }
        )
        PIQDropdownField(
            label = stringResource(R.string.piq_boarder),
            value = answers["educationGrad_boarder"] ?: "",
            options = listOf("", "Boarder", "Day Scholar"),
            onValueChange = { onFieldChange("educationGrad_boarder", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_achievement),
            value = answers["educationGrad_achievement"] ?: "",
            onValueChange = { onFieldChange("educationGrad_achievement", it) },
            multiline = true,
            minLines = 2
        )
        
        // Post-Graduation Education
        Text(stringResource(R.string.piq_post_graduation), style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        PIQTextField(
            label = stringResource(R.string.piq_institution_name),
            value = answers["educationPG_institution"] ?: "",
            onValueChange = { onFieldChange("educationPG_institution", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_university),
            value = answers["educationPG_university"] ?: "",
            onValueChange = { onFieldChange("educationPG_university", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_degree_diploma),
            value = answers["educationPG_degree"] ?: "",
            onValueChange = { onFieldChange("educationPG_degree", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_year_passing),
            value = answers["educationPG_year"] ?: "",
            onValueChange = { onFieldChange("educationPG_year", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = stringResource(R.string.piq_cgpa),
            value = answers["educationPG_cgpa"] ?: "",
            onValueChange = { onFieldChange("educationPG_cgpa", it) },
            keyboardType = KeyboardType.Decimal
        )
        PIQTextField(
            label = stringResource(R.string.piq_medium),
            value = answers["educationPG_medium"] ?: "",
            onValueChange = { onFieldChange("educationPG_medium", it) }
        )
        PIQDropdownField(
            label = stringResource(R.string.piq_boarder),
            value = answers["educationPG_boarder"] ?: "",
            options = listOf("", "Boarder", "Day Scholar"),
            onValueChange = { onFieldChange("educationPG_boarder", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_achievement),
            value = answers["educationPG_achievement"] ?: "",
            onValueChange = { onFieldChange("educationPG_achievement", it) },
            multiline = true,
            minLines = 2
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun Page2Content(
    answers: Map<String, String>,
    onFieldChange: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            stringResource(R.string.piq_career_additional_title),
            style = MaterialTheme.typography.headlineSmall
        )

        // Physical Details - moved from Page 1 to start of Page 2
        PIQSectionHeader(stringResource(R.string.piq_physical_details))
        PIQTextField(
            label = stringResource(R.string.piq_age_years_months),
            value = answers["age"] ?: "",
            onValueChange = { onFieldChange("age", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = stringResource(R.string.piq_height),
            value = answers["height"] ?: "",
            onValueChange = { onFieldChange("height", it) },
            keyboardType = KeyboardType.Decimal
        )
        PIQTextField(
            label = stringResource(R.string.piq_weight),
            value = answers["weight"] ?: "",
            onValueChange = { onFieldChange("weight", it) },
            keyboardType = KeyboardType.Decimal
        )

        // Occupation - moved from Page 1 to Page 2
        PIQSectionHeader(stringResource(R.string.piq_occupation_section))
        PIQTextField(
            label = stringResource(R.string.piq_present_occupation),
            value = answers["presentOccupation"] ?: "",
            onValueChange = { onFieldChange("presentOccupation", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_monthly_income),
            value = answers["personalMonthlyIncome"] ?: "",
            onValueChange = { onFieldChange("personalMonthlyIncome", it) },
            keyboardType = KeyboardType.Number
        )

        // NCC Training - comes after Occupation as per SSB PIQ
        PIQSectionHeader(stringResource(R.string.piq_ncc_section))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = answers["ncc_hasTraining"]?.toBoolean() ?: false,
                onCheckedChange = { onFieldChange("ncc_hasTraining", it.toString()) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.piq_has_ncc))
        }
        if (answers["ncc_hasTraining"]?.toBoolean() == true) {
            PIQTextField(
                label = stringResource(R.string.piq_ncc_total),
                value = answers["ncc_totalTraining"] ?: "",
                onValueChange = { onFieldChange("ncc_totalTraining", it) }
            )
            PIQDropdownField(
                label = stringResource(R.string.piq_ncc_wing),
                value = answers["ncc_wing"] ?: "",
                options = listOf("", "Army", "Navy", "Air Force"),
                onValueChange = { onFieldChange("ncc_wing", it) }
            )
            PIQTextField(
                label = stringResource(R.string.piq_ncc_division),
                value = answers["ncc_division"] ?: "",
                onValueChange = { onFieldChange("ncc_division", it) }
            )
            PIQTextField(
                label = stringResource(R.string.piq_ncc_certificate),
                value = answers["ncc_certificate"] ?: "",
                onValueChange = { onFieldChange("ncc_certificate", it) }
            )
        }
        
        // Participation in games & sports - comes after NCC Training as per SSB PIQ
        PIQSectionHeader(stringResource(R.string.piq_sports_section))
        PIQTextField(
            label = stringResource(R.string.piq_sports),
            value = answers["sports"] ?: "",
            onValueChange = { onFieldChange("sports", it) },
            multiline = true,
            minLines = 2,
            placeholder = stringResource(R.string.piq_sports_placeholder)
        )

        // Hobbies/Interest - comes after Sports as per SSB PIQ
        PIQSectionHeader(stringResource(R.string.piq_interests_section))
        PIQTextField(
            label = stringResource(R.string.piq_hobbies),
            value = answers["hobbies"] ?: "",
            onValueChange = { onFieldChange("hobbies", it) },
            multiline = true,
            minLines = 3,
            placeholder = stringResource(R.string.piq_hobbies_placeholder)
        )

        // Participation in extra-curricular activities - comes after Hobbies as per SSB PIQ
        // Note: Currently using simple text field. Dynamic list can be added later.

        // Position of responsibility/offices held - comes after Activities as per SSB PIQ
        PIQSectionHeader(stringResource(R.string.piq_responsibility_section))
        PIQTextField(
            label = stringResource(R.string.piq_positions),
            value = answers["positionsOfResponsibility"] ?: "",
            onValueChange = { onFieldChange("positionsOfResponsibility", it) },
            multiline = true,
            minLines = 3,
            placeholder = stringResource(R.string.piq_positions_placeholder)
        )

        // Service Selection - comes after Positions as per SSB PIQ
        PIQSectionHeader(stringResource(R.string.piq_service_section))
        PIQTextField(
            label = stringResource(R.string.piq_commission_nature),
            value = answers["natureOfCommission"] ?: "",
            onValueChange = { onFieldChange("natureOfCommission", it) }
        )
        PIQDropdownField(
            label = stringResource(R.string.piq_choice_service),
            value = answers["choiceOfService"] ?: "",
            options = listOf("", "Army", "Navy", "Air Force", "Coast Guard", "Any"),
            onValueChange = { onFieldChange("choiceOfService", it) }
        )
        PIQTextField(
            label = stringResource(R.string.piq_chances_availed),
            value = answers["chancesAvailed"] ?: "",
            onValueChange = { onFieldChange("chancesAvailed", it) },
            keyboardType = KeyboardType.Number
        )
        
        // Details of all previous interviews - comes after Service Selection as per SSB PIQ
        // Note: Currently using simple text field. Dynamic list can be added later.

        Spacer(modifier = Modifier.height(32.dp))
    }
}
