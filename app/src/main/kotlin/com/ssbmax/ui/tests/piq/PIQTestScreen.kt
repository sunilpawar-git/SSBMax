package com.ssbmax.ui.tests.piq

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.PIQPage

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
        Text(
            "Personal & Family Details",
            style = MaterialTheme.typography.headlineSmall
        )

        // Personal Information
        SectionHeader("Personal Information")
        PIQTextField(
            label = "Full Name *",
            value = answers["fullName"] ?: "",
            onValueChange = { onFieldChange("fullName", it) },
            isRequired = true
        )
        PIQTextField(
            label = "Date of Birth *",
            value = answers["dateOfBirth"] ?: "",
            onValueChange = { onFieldChange("dateOfBirth", it) },
            placeholder = "DD/MM/YYYY",
            isRequired = true
        )
        PIQTextField(
            label = "Age",
            value = answers["age"] ?: "",
            onValueChange = { onFieldChange("age", it) },
            keyboardType = KeyboardType.Number
        )
        PIQTextField(
            label = "Gender *",
            value = answers["gender"] ?: "",
            onValueChange = { onFieldChange("gender", it) },
            placeholder = "Male/Female/Other",
            isRequired = true
        )
        PIQTextField(
            label = "Phone Number *",
            value = answers["phone"] ?: "",
            onValueChange = { onFieldChange("phone", it) },
            keyboardType = KeyboardType.Phone,
            isRequired = true
        )
        PIQTextField(
            label = "Email",
            value = answers["email"] ?: "",
            onValueChange = { onFieldChange("email", it) },
            keyboardType = KeyboardType.Email
        )
        PIQTextField(
            label = "Permanent Address *",
            value = answers["permanentAddress"] ?: "",
            onValueChange = { onFieldChange("permanentAddress", it) },
            multiline = true,
            minLines = 3,
            isRequired = true
        )
        PIQTextField(
            label = "Present Address",
            value = answers["presentAddress"] ?: "",
            onValueChange = { onFieldChange("presentAddress", it) },
            multiline = true,
            minLines = 3
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Father Details
        SectionHeader("Father's Information")
        PIQTextField(
            label = "Father's Name *",
            value = answers["fatherName"] ?: "",
            onValueChange = { onFieldChange("fatherName", it) },
            isRequired = true
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

        Spacer(modifier = Modifier.height(8.dp))

        // Mother Details
        SectionHeader("Mother's Information")
        PIQTextField(
            label = "Mother's Name *",
            value = answers["motherName"] ?: "",
            onValueChange = { onFieldChange("motherName", it) },
            isRequired = true
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
            "Education & Career Details",
            style = MaterialTheme.typography.headlineSmall
        )

        // Hobbies and Sports
        SectionHeader("Interests")
        PIQTextField(
            label = "Hobbies & Interests",
            value = answers["hobbies"] ?: "",
            onValueChange = { onFieldChange("hobbies", it) },
            multiline = true,
            minLines = 3,
            placeholder = "List your hobbies and interests"
        )
        PIQTextField(
            label = "Sports Played",
            value = answers["sports"] ?: "",
            onValueChange = { onFieldChange("sports", it) },
            multiline = true,
            minLines = 2,
            placeholder = "Sports you play or have played"
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Motivation
        SectionHeader("Motivation & Self Assessment")
        PIQTextField(
            label = "Why do you want to join the Defense Forces? *",
            value = answers["whyDefenseForces"] ?: "",
            onValueChange = { onFieldChange("whyDefenseForces", it) },
            multiline = true,
            minLines = 5,
            placeholder = "Explain your motivation...",
            isRequired = true
        )
        PIQTextField(
            label = "Your Strengths",
            value = answers["strengths"] ?: "",
            onValueChange = { onFieldChange("strengths", it) },
            multiline = true,
            minLines = 3,
            placeholder = "What are your key strengths?"
        )
        PIQTextField(
            label = "Areas for Improvement",
            value = answers["weaknesses"] ?: "",
            onValueChange = { onFieldChange("weaknesses", it) },
            multiline = true,
            minLines = 3,
            placeholder = "What areas would you like to improve?"
        )

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
    isRequired: Boolean = false
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
        supportingText = if (isRequired && value.isBlank()) {
            { Text("Required field", color = MaterialTheme.colorScheme.error) }
        } else null,
        isError = isRequired && value.isBlank()
    )
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

