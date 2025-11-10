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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.PIQPage
import com.ssbmax.ui.tests.piq.SELECTION_BOARD_OPTIONS

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
                            Icon(Icons.Default.ArrowBack, "Back")
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
                                "✓ Saved",
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
                    nextButtonText = if (uiState.currentPage == PIQPage.PAGE_2) "Review" else "Next"
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
                    title = { Text("Error") },
                    text = { Text(error) },
                    confirmButton = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("OK")
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
        PrivacyWarningBanner()
        
        Text(
            "Personal & Family Details",
            style = MaterialTheme.typography.headlineSmall
        )
        
        // Header Section (Exact SSB PIQ sequence)
        SectionHeader("Header Information")
        PIQTextField(
            label = "OIR Number",
            value = answers["oirNumber"] ?: "",
            onValueChange = { onFieldChange("oirNumber", it) },
            placeholder = "To be filled from OIR Test Result",
            enabled = false // Auto-filled, read-only
        )
        PIQDropdownField(
            label = "Selection Board",
            value = answers["selectionBoard"] ?: "",
            options = SELECTION_BOARD_OPTIONS,
            onValueChange = { onFieldChange("selectionBoard", it) }
        )
        PIQTextField(
            label = "Batch No",
            value = answers["batchNumber"] ?: "",
            onValueChange = { onFieldChange("batchNumber", it) },
            placeholder = "To be filled at SSB",
            enabled = false,
            supportingText = "To be filled at SSB"
        )
        PIQTextField(
            label = "Chest Number",
            value = answers["chestNumber"] ?: "",
            onValueChange = { onFieldChange("chestNumber", it) },
            placeholder = "To be filled at SSB",
            enabled = false,
            supportingText = "To be filled at SSB"
        )
        PIQTextField(
            label = "UPSC Roll No",
            value = answers["upscRollNumber"] ?: "",
            onValueChange = { onFieldChange("upscRollNumber", it) },
            placeholder = "To be filled at SSB",
            enabled = false,
            supportingText = "To be filled at SSB"
        )

        // Name (in CAPITALS) - comes right after header as per SSB PIQ
        PIQTextField(
            label = "Full Name (in CAPITALS)",
            value = answers["fullName"] ?: "",
            onValueChange = { onFieldChange("fullName", it) }
        )
        
        // Residence Information - comes after Name as per SSB PIQ
        SectionHeader("Residence Information")
        PIQTextField(
            label = "Maximum Residence",
            value = answers["maximumResidence"] ?: "",
            onValueChange = { onFieldChange("maximumResidence", it) }
        )
        PIQTextField(
            label = "Maximum Residence Population",
            value = answers["maximumResidencePopulation"] ?: "",
            onValueChange = { onFieldChange("maximumResidencePopulation", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = "Present Address",
            value = answers["presentAddress"] ?: "",
            onValueChange = { onFieldChange("presentAddress", it) },
            multiline = true,
            minLines = 3
        )
        PIQTextField(
            label = "Present Residence Population",
            value = answers["presentResidencePopulation"] ?: "",
            onValueChange = { onFieldChange("presentResidencePopulation", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = "Permanent Address",
            value = answers["permanentAddress"] ?: "",
            onValueChange = { onFieldChange("permanentAddress", it) },
            multiline = true,
            minLines = 3
        )
        PIQTextField(
            label = "Permanent Residence Population",
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
            Text("Is District HQ?")
        }
        
        // Personal Details Table - comes after Residence as per SSB PIQ
        SectionHeader("Personal Details")
        PIQTextField(
            label = "State",
            value = answers["state"] ?: "",
            onValueChange = { onFieldChange("state", it) }
        )
        PIQTextField(
            label = "District",
            value = answers["district"] ?: "",
            onValueChange = { onFieldChange("district", it) }
        )
        PIQTextField(
            label = "Religion",
            value = answers["religion"] ?: "",
            onValueChange = { onFieldChange("religion", it) }
        )
        PIQDropdownField(
            label = "SC/ST/OBC Status",
            value = answers["scStObcStatus"] ?: "",
            options = listOf("", "SC", "ST", "OBC"),
            onValueChange = { onFieldChange("scStObcStatus", it) }
        )
        PIQTextField(
            label = "Mother Tongue",
            value = answers["motherTongue"] ?: "",
            onValueChange = { onFieldChange("motherTongue", it) }
        )
        PIQTextField(
            label = "Date of Birth",
            value = answers["dateOfBirth"] ?: "",
            onValueChange = { onFieldChange("dateOfBirth", it) },
            placeholder = "DD/MM/YYYY"
        )
        PIQDropdownField(
            label = "Marital Status",
            value = answers["maritalStatus"] ?: "",
            options = listOf("", "Single", "Married", "Widower"),
            onValueChange = { onFieldChange("maritalStatus", it) }
        )
        
        // Parents Alive - comes after Personal Details Table as per SSB PIQ
        SectionHeader("Family Enhancement")
        PIQDropdownField(
            label = "Parents Alive",
            value = answers["parentsAlive"] ?: "",
            options = listOf("", "Both", "Father Only", "Mother Only", "None"),
            onValueChange = { onFieldChange("parentsAlive", it) }
        )
        
        // Conditional fields based on parentsAlive
        if (answers["parentsAlive"] == "Mother Only" || answers["parentsAlive"] == "None") {
            PIQTextField(
                label = "Age at Father's Death",
                value = answers["ageAtFatherDeath"] ?: "",
                onValueChange = { onFieldChange("ageAtFatherDeath", it) },
                keyboardType = KeyboardType.Number
            )
        }
        if (answers["parentsAlive"] == "Father Only" || answers["parentsAlive"] == "None") {
            PIQTextField(
                label = "Age at Mother's Death",
                value = answers["ageAtMotherDeath"] ?: "",
                onValueChange = { onFieldChange("ageAtMotherDeath", it) },
                keyboardType = KeyboardType.Number
            )
        }
        if (answers["parentsAlive"] == "None") {
            SectionHeader("Guardian Information")
            PIQTextField(
                label = "Guardian Name",
                value = answers["guardianName"] ?: "",
                onValueChange = { onFieldChange("guardianName", it) }
            )
            PIQTextField(
                label = "Guardian Occupation",
                value = answers["guardianOccupation"] ?: "",
                onValueChange = { onFieldChange("guardianOccupation", it) }
            )
            PIQTextField(
                label = "Guardian Education",
                value = answers["guardianEducation"] ?: "",
                onValueChange = { onFieldChange("guardianEducation", it) }
            )
            PIQTextField(
                label = "Guardian Income",
                value = answers["guardianIncome"] ?: "",
                onValueChange = { onFieldChange("guardianIncome", it) }
            )
        }
        
        // Parents/Guardian/Siblings table (Education, Occupation, Income)
        // Father's Information
        SectionHeader("Father's Information")
        PIQTextField(
            label = "Father's Name",
            value = answers["fatherName"] ?: "",
            onValueChange = { onFieldChange("fatherName", it) }
        )
        PIQTextField(
            label = "Father's Occupation",
            value = answers["fatherOccupation"] ?: "",
            onValueChange = { onFieldChange("fatherOccupation", it) }
        )
        PIQTextField(
            label = "Father's Education",
            value = answers["fatherEducation"] ?: "",
            onValueChange = { onFieldChange("fatherEducation", it) }
        )
        PIQTextField(
            label = "Father's Annual Income",
            value = answers["fatherIncome"] ?: "",
            onValueChange = { onFieldChange("fatherIncome", it) }
        )

        // Mother's Information
        SectionHeader("Mother's Information")
        PIQTextField(
            label = "Mother's Name",
            value = answers["motherName"] ?: "",
            onValueChange = { onFieldChange("motherName", it) }
        )
        PIQTextField(
            label = "Mother's Occupation",
            value = answers["motherOccupation"] ?: "",
            onValueChange = { onFieldChange("motherOccupation", it) }
        )
        PIQTextField(
            label = "Mother's Education",
            value = answers["motherEducation"] ?: "",
            onValueChange = { onFieldChange("motherEducation", it) }
        )
        
        // Siblings Information - comes after Mother's Information as per SSB PIQ
        SectionHeader("Siblings Information")
        Text(
            "Elder Brother/Sister",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        // Note: Siblings are managed as a dynamic list in ViewModel state
        // For now, we'll add fields for up to 2 elder siblings
        repeat(2) { index ->
            val prefix = "elderSibling${index + 1}_"
            PIQTextField(
                label = "Elder Sibling ${index + 1} - Name",
                value = answers["${prefix}name"] ?: "",
                onValueChange = { onFieldChange("${prefix}name", it) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PIQTextField(
                    label = "Age",
                    value = answers["${prefix}age"] ?: "",
                    onValueChange = { onFieldChange("${prefix}age", it) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                PIQTextField(
                    label = "Education",
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
                    label = "Occupation",
                    value = answers["${prefix}occupation"] ?: "",
                    onValueChange = { onFieldChange("${prefix}occupation", it) },
                    modifier = Modifier.weight(1f)
                )
                PIQTextField(
                    label = "Income",
                    value = answers["${prefix}income"] ?: "",
                    onValueChange = { onFieldChange("${prefix}income", it) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        Text(
            "Younger Brother/Sister",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(top = 8.dp)
        )
        // Add fields for up to 2 younger siblings
        repeat(2) { index ->
            val prefix = "youngerSibling${index + 1}_"
            PIQTextField(
                label = "Younger Sibling ${index + 1} - Name",
                value = answers["${prefix}name"] ?: "",
                onValueChange = { onFieldChange("${prefix}name", it) }
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PIQTextField(
                    label = "Age",
                    value = answers["${prefix}age"] ?: "",
                    onValueChange = { onFieldChange("${prefix}age", it) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
                PIQTextField(
                    label = "Education",
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
                    label = "Occupation",
                    value = answers["${prefix}occupation"] ?: "",
                    onValueChange = { onFieldChange("${prefix}occupation", it) },
                    modifier = Modifier.weight(1f)
                )
                PIQTextField(
                    label = "Income",
                    value = answers["${prefix}income"] ?: "",
                    onValueChange = { onFieldChange("${prefix}income", it) },
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Educational Record - moved from Page 2 to end of Page 1
        SectionHeader("Educational Record")
        
        // 10th Standard Education
        Text("10th Standard", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        PIQTextField(
            label = "School Name",
            value = answers["education10th_institution"] ?: "",
            onValueChange = { onFieldChange("education10th_institution", it) }
        )
        PIQTextField(
            label = "Board",
            value = answers["education10th_board"] ?: "",
            onValueChange = { onFieldChange("education10th_board", it) }
        )
        PIQTextField(
            label = "Year of Passing",
            value = answers["education10th_year"] ?: "",
            onValueChange = { onFieldChange("education10th_year", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = "Percentage",
            value = answers["education10th_percentage"] ?: "",
            onValueChange = { onFieldChange("education10th_percentage", it) },
            keyboardType = KeyboardType.Decimal
        )
        PIQTextField(
            label = "Medium of Instruction",
            value = answers["education10th_medium"] ?: "",
            onValueChange = { onFieldChange("education10th_medium", it) }
        )
        PIQDropdownField(
            label = "Boarder/Day Scholar",
            value = answers["education10th_boarder"] ?: "",
            options = listOf("", "Boarder", "Day Scholar"),
            onValueChange = { onFieldChange("education10th_boarder", it) }
        )
        PIQTextField(
            label = "Outstanding Achievement",
            value = answers["education10th_achievement"] ?: "",
            onValueChange = { onFieldChange("education10th_achievement", it) },
            multiline = true,
            minLines = 2
        )
        
        // 12th Standard Education
        Text("12th Standard", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        PIQTextField(
            label = "School Name",
            value = answers["education12th_institution"] ?: "",
            onValueChange = { onFieldChange("education12th_institution", it) }
        )
        PIQTextField(
            label = "Board",
            value = answers["education12th_board"] ?: "",
            onValueChange = { onFieldChange("education12th_board", it) }
        )
        PIQDropdownField(
            label = "Stream",
            value = answers["education12th_stream"] ?: "",
            options = listOf("", "Science", "Commerce", "Arts"),
            onValueChange = { onFieldChange("education12th_stream", it) }
        )
        PIQTextField(
            label = "Year of Passing",
            value = answers["education12th_year"] ?: "",
            onValueChange = { onFieldChange("education12th_year", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = "Percentage",
            value = answers["education12th_percentage"] ?: "",
            onValueChange = { onFieldChange("education12th_percentage", it) },
            keyboardType = KeyboardType.Decimal
        )
        PIQTextField(
            label = "Medium of Instruction",
            value = answers["education12th_medium"] ?: "",
            onValueChange = { onFieldChange("education12th_medium", it) }
        )
        PIQDropdownField(
            label = "Boarder/Day Scholar",
            value = answers["education12th_boarder"] ?: "",
            options = listOf("", "Boarder", "Day Scholar"),
            onValueChange = { onFieldChange("education12th_boarder", it) }
        )
        PIQTextField(
            label = "Outstanding Achievement",
            value = answers["education12th_achievement"] ?: "",
            onValueChange = { onFieldChange("education12th_achievement", it) },
            multiline = true,
            minLines = 2
        )
        
        // Graduation Education
        Text("Graduation", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        PIQTextField(
            label = "College Name",
            value = answers["educationGrad_institution"] ?: "",
            onValueChange = { onFieldChange("educationGrad_institution", it) }
        )
        PIQTextField(
            label = "University",
            value = answers["educationGrad_university"] ?: "",
            onValueChange = { onFieldChange("educationGrad_university", it) }
        )
        PIQTextField(
            label = "Degree",
            value = answers["educationGrad_degree"] ?: "",
            onValueChange = { onFieldChange("educationGrad_degree", it) }
        )
        PIQTextField(
            label = "Year of Passing",
            value = answers["educationGrad_year"] ?: "",
            onValueChange = { onFieldChange("educationGrad_year", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = "CGPA/Percentage",
            value = answers["educationGrad_cgpa"] ?: "",
            onValueChange = { onFieldChange("educationGrad_cgpa", it) },
            keyboardType = KeyboardType.Decimal
        )
        PIQTextField(
            label = "Medium of Instruction",
            value = answers["educationGrad_medium"] ?: "",
            onValueChange = { onFieldChange("educationGrad_medium", it) }
        )
        PIQDropdownField(
            label = "Boarder/Day Scholar",
            value = answers["educationGrad_boarder"] ?: "",
            options = listOf("", "Boarder", "Day Scholar"),
            onValueChange = { onFieldChange("educationGrad_boarder", it) }
        )
        PIQTextField(
            label = "Outstanding Achievement",
            value = answers["educationGrad_achievement"] ?: "",
            onValueChange = { onFieldChange("educationGrad_achievement", it) },
            multiline = true,
            minLines = 2
        )
        
        // Post-Graduation Education
        Text("Post-Graduation/Professional", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp))
        PIQTextField(
            label = "Institution Name",
            value = answers["educationPG_institution"] ?: "",
            onValueChange = { onFieldChange("educationPG_institution", it) }
        )
        PIQTextField(
            label = "University",
            value = answers["educationPG_university"] ?: "",
            onValueChange = { onFieldChange("educationPG_university", it) }
        )
        PIQTextField(
            label = "Degree/Diploma",
            value = answers["educationPG_degree"] ?: "",
            onValueChange = { onFieldChange("educationPG_degree", it) }
        )
        PIQTextField(
            label = "Year of Passing",
            value = answers["educationPG_year"] ?: "",
            onValueChange = { onFieldChange("educationPG_year", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = "CGPA/Percentage",
            value = answers["educationPG_cgpa"] ?: "",
            onValueChange = { onFieldChange("educationPG_cgpa", it) },
            keyboardType = KeyboardType.Decimal
        )
        PIQTextField(
            label = "Medium of Instruction",
            value = answers["educationPG_medium"] ?: "",
            onValueChange = { onFieldChange("educationPG_medium", it) }
        )
        PIQDropdownField(
            label = "Boarder/Day Scholar",
            value = answers["educationPG_boarder"] ?: "",
            options = listOf("", "Boarder", "Day Scholar"),
            onValueChange = { onFieldChange("educationPG_boarder", it) }
        )
        PIQTextField(
            label = "Outstanding Achievement",
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
            "Career & Additional Details",
            style = MaterialTheme.typography.headlineSmall
        )

        // Physical Details - moved from Page 1 to start of Page 2
        SectionHeader("Physical Details")
        PIQTextField(
            label = "Age (Years & Months)",
            value = answers["age"] ?: "",
            onValueChange = { onFieldChange("age", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = "Height (metres)",
            value = answers["height"] ?: "",
            onValueChange = { onFieldChange("height", it) },
            keyboardType = KeyboardType.Decimal
        )
        PIQTextField(
            label = "Weight (kilograms)",
            value = answers["weight"] ?: "",
            onValueChange = { onFieldChange("weight", it) },
            keyboardType = KeyboardType.Decimal
        )
        
        // Occupation - moved from Page 1 to Page 2
        SectionHeader("Occupation")
        PIQTextField(
            label = "Present Occupation",
            value = answers["presentOccupation"] ?: "",
            onValueChange = { onFieldChange("presentOccupation", it) }
        )
        PIQTextField(
            label = "Personal Monthly Income",
            value = answers["personalMonthlyIncome"] ?: "",
            onValueChange = { onFieldChange("personalMonthlyIncome", it) },
            keyboardType = KeyboardType.Number
        )

        // NCC Training - comes after Occupation as per SSB PIQ
        SectionHeader("NCC Training")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = answers["ncc_hasTraining"]?.toBoolean() ?: false,
                onCheckedChange = { onFieldChange("ncc_hasTraining", it.toString()) }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Have NCC Training?")
        }
        if (answers["ncc_hasTraining"]?.toBoolean() == true) {
            PIQTextField(
                label = "Total Training",
                value = answers["ncc_totalTraining"] ?: "",
                onValueChange = { onFieldChange("ncc_totalTraining", it) }
            )
            PIQDropdownField(
                label = "Wing",
                value = answers["ncc_wing"] ?: "",
                options = listOf("", "Army", "Navy", "Air Force"),
                onValueChange = { onFieldChange("ncc_wing", it) }
            )
            PIQTextField(
                label = "Division",
                value = answers["ncc_division"] ?: "",
                onValueChange = { onFieldChange("ncc_division", it) }
            )
            PIQTextField(
                label = "Certificate Obtained",
                value = answers["ncc_certificate"] ?: "",
                onValueChange = { onFieldChange("ncc_certificate", it) }
            )
        }
        
        // Participation in games & sports - comes after NCC Training as per SSB PIQ
        SectionHeader("Sports Participation")
        PIQTextField(
            label = "Sports Played",
            value = answers["sports"] ?: "",
            onValueChange = { onFieldChange("sports", it) },
            multiline = true,
            minLines = 2,
            placeholder = "Sports you play or have played"
        )
        
        // Hobbies/Interest - comes after Sports as per SSB PIQ
        SectionHeader("Interests")
        PIQTextField(
            label = "Hobbies & Interests",
            value = answers["hobbies"] ?: "",
            onValueChange = { onFieldChange("hobbies", it) },
            multiline = true,
            minLines = 3,
            placeholder = "List your hobbies and interests"
        )
        
        // Participation in extra-curricular activities - comes after Hobbies as per SSB PIQ
        // Note: Currently using simple text field. Dynamic list can be added later.
        
        // Position of responsibility/offices held - comes after Activities as per SSB PIQ
        SectionHeader("Positions of Responsibility")
        PIQTextField(
            label = "Positions Held",
            value = answers["positionsOfResponsibility"] ?: "",
            onValueChange = { onFieldChange("positionsOfResponsibility", it) },
            multiline = true,
            minLines = 3,
            placeholder = "List positions of responsibility held"
        )
        
        // Service Selection - comes after Positions as per SSB PIQ
        SectionHeader("Service Selection")
        PIQTextField(
            label = "Nature of Commission",
            value = answers["natureOfCommission"] ?: "",
            onValueChange = { onFieldChange("natureOfCommission", it) }
        )
        PIQDropdownField(
            label = "Choice of Service",
            value = answers["choiceOfService"] ?: "",
            options = listOf("", "Army", "Navy", "Air Force", "Coast Guard", "Any"),
            onValueChange = { onFieldChange("choiceOfService", it) }
        )
        PIQTextField(
            label = "Number of Chances Availed",
            value = answers["chancesAvailed"] ?: "",
            onValueChange = { onFieldChange("chancesAvailed", it) },
            keyboardType = KeyboardType.Number
        )
        
        // Details of all previous interviews - comes after Service Selection as per SSB PIQ
        // Note: Currently using simple text field. Dynamic list can be added later.

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary
    )
}

@Composable
private fun PIQTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    multiline: Boolean = false,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    isRequired: Boolean = false,
    enabled: Boolean = true,
    supportingText: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = if (placeholder.isNotEmpty()) {
            { Text(placeholder) }
        } else null,
        modifier = modifier.fillMaxWidth(),
        singleLine = !multiline,
        minLines = if (multiline) minLines else 1,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        supportingText = supportingText?.let { { Text(it) } },
        isError = false,
        enabled = enabled
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PIQDropdownField(
    label: String,
    value: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor()
        )
        
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun PrivacyWarningBanner() {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Privacy Notice",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "All information entered in this form is stored securely in the cloud. " +
                            "This is a practice form to help you familiarize yourself with the actual PIQ format. " +
                            "Please use generic or sample information only. Do not enter sensitive personal details.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }
    }
}

@Composable
private fun PIQNavigationBar(
    currentPage: PIQPage,
    onPreviousPage: () -> Unit,
    onNextPage: () -> Unit,
    canGoBack: Boolean,
    nextButtonText: String
) {
    Surface(
        shadowElevation = 8.dp,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (canGoBack) {
                OutlinedButton(onClick = onPreviousPage) {
                    Text("Previous")
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Button(onClick = onNextPage) {
                Text(nextButtonText)
            }
        }
    }
}

