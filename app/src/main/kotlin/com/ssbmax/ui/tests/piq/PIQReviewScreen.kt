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
import androidx.compose.ui.unit.dp
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
                title = { Text("Review Your Information") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
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
                        Text("Submit PIQ")
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
            // Completion warning
            val missingRequiredFields = getMissingRequiredFields(answers)
            if (missingRequiredFields.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Incomplete Information",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "The following required fields are missing:\n${missingRequiredFields.joinToString("\n• ", "• ")}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            // Page 1 Summary
            ReviewSection(
                title = "Personal & Family Details",
                onEdit = { onEdit(PIQPage.PAGE_1) }
            ) {
                ReviewField("Full Name", answers["fullName"])
                ReviewField("Date of Birth", answers["dateOfBirth"])
                ReviewField("Age", answers["age"])
                ReviewField("Gender", answers["gender"])
                ReviewField("Phone", answers["phone"])
                ReviewField("Email", answers["email"])
                ReviewField("Permanent Address", answers["permanentAddress"])
                ReviewField("Present Address", answers["presentAddress"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Father's Information", style = MaterialTheme.typography.titleSmall)
                ReviewField("Name", answers["fatherName"])
                ReviewField("Occupation", answers["fatherOccupation"])
                ReviewField("Education", answers["fatherEducation"])
                ReviewField("Income", answers["fatherIncome"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                Text("Mother's Information", style = MaterialTheme.typography.titleSmall)
                ReviewField("Name", answers["motherName"])
                ReviewField("Occupation", answers["motherOccupation"])
                ReviewField("Education", answers["motherEducation"])
            }

            // Page 2 Summary
            ReviewSection(
                title = "Education & Career Details",
                onEdit = { onEdit(PIQPage.PAGE_2) }
            ) {
                ReviewField("Hobbies & Interests", answers["hobbies"])
                ReviewField("Sports", answers["sports"])
                
                Divider(modifier = Modifier.padding(vertical = 8.dp))
                ReviewField("Why Defense Forces?", answers["whyDefenseForces"])
                ReviewField("Strengths", answers["strengths"])
                ReviewField("Areas for Improvement", answers["weaknesses"])
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Submit confirmation dialog
        if (showSubmitDialog) {
            AlertDialog(
                onDismissRequest = { showSubmitDialog = false },
                title = { Text("Submit PIQ?") },
                text = {
                    val missingFields = getMissingRequiredFields(answers)
                    Text(
                        if (missingFields.isNotEmpty()) {
                            "You have incomplete information. Are you sure you want to submit?"
                        } else {
                            "Once submitted, you cannot edit this form. Continue?"
                        }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showSubmitDialog = false
                        onSubmit()
                    }) {
                        Text("Submit")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSubmitDialog = false }) {
                        Text("Cancel")
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
                    Icon(Icons.Default.Edit, "Edit")
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

private fun getMissingRequiredFields(answers: Map<String, String>): List<String> {
    val required = mapOf(
        "fullName" to "Full Name",
        "dateOfBirth" to "Date of Birth",
        "gender" to "Gender",
        "phone" to "Phone Number",
        "permanentAddress" to "Permanent Address",
        "fatherName" to "Father's Name",
        "motherName" to "Mother's Name",
        "whyDefenseForces" to "Why Defense Forces"
    )
    
    return required.filter { (key, _) ->
        answers[key].isNullOrBlank()
    }.values.toList()
}

