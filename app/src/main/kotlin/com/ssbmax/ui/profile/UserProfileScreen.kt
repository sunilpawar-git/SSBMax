package com.ssbmax.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssbmax.core.domain.model.EntryType
import com.ssbmax.core.domain.model.Gender

/**
 * User Profile Screen - Allows users to create/edit their profile
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    onNavigateBack: () -> Unit,
    onProfileSaved: () -> Unit,
    isOnboarding: Boolean = false,
    viewModel: UserProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Prevent back navigation during onboarding if profile incomplete
    androidx.activity.compose.BackHandler(enabled = isOnboarding && uiState.profile == null) {
        // Do nothing - prevent back press during onboarding
    }

    // Navigate back when profile is saved
    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            viewModel.resetSavedState()
            onProfileSaved()
        }
    }

    // Load existing profile data into form
    LaunchedEffect(uiState.profile) {
        uiState.profile?.let { profile ->
            viewModel.updateFullName(profile.fullName)
            viewModel.updateAge(profile.age.toString())
            viewModel.updateGender(profile.gender)
            viewModel.updateEntryType(profile.entryType)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isOnboarding) "Complete Your Profile" else "User Profile") },
                navigationIcon = {
                    if (!isOnboarding || uiState.profile != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, "Back")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                UserProfileForm(
                    uiState = uiState,
                    onFullNameChange = viewModel::updateFullName,
                    onAgeChange = viewModel::updateAge,
                    onGenderChange = viewModel::updateGender,
                    onEntryTypeChange = viewModel::updateEntryType,
                    onSave = viewModel::saveProfile
                )
            }
        }
    }
}

@Composable
private fun UserProfileForm(
    uiState: UserProfileUiState,
    onFullNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onEntryTypeChange: (EntryType) -> Unit,
    onSave: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile Picture Placeholder
        ProfilePicturePlaceholder(
            initials = if (uiState.fullName.isNotBlank()) {
                uiState.fullName.split(" ")
                    .take(2)
                    .mapNotNull { it.firstOrNull() }
                    .joinToString("")
                    .uppercase()
            } else "U"
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Full Name Field
        OutlinedTextField(
            value = uiState.fullName,
            onValueChange = onFullNameChange,
            label = { Text("Full Name *") },
            placeholder = { Text("Enter your full name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Age Field
        OutlinedTextField(
            value = uiState.age?.toString() ?: "",
            onValueChange = onAgeChange,
            label = { Text("Age *") },
            placeholder = { Text("18-35") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Gender Selection
        Text(
            text = "Gender *",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        GenderSelector(
            selectedGender = uiState.gender,
            onGenderSelected = onGenderChange
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Entry Type Selection
        Text(
            text = "Type of Entry *",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        EntryTypeSelector(
            selectedEntryType = uiState.entryType,
            onEntryTypeSelected = onEntryTypeChange
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Error Message
        if (uiState.error != null) {
            Text(
                text = uiState.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Save Button
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.isLoading
        ) {
            Text("Save Profile")
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun ProfilePicturePlaceholder(initials: String) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun GenderSelector(
    selectedGender: Gender?,
    onGenderSelected: (Gender) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Gender.values().forEach { gender ->
            FilterChip(
                selected = selectedGender == gender,
                onClick = { onGenderSelected(gender) },
                label = { Text(gender.displayName) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun EntryTypeSelector(
    selectedEntryType: EntryType?,
    onEntryTypeSelected: (EntryType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EntryType.values().forEach { entryType ->
            FilterChip(
                selected = selectedEntryType == entryType,
                onClick = { onEntryTypeSelected(entryType) },
                label = { Text(entryType.displayName) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

