package com.ssbmax.ui.instructor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssbmax.R

/**
 * Create Batch Screen
 * 
 * Allows instructors to create new student batches/groups with Name, Description, and Max Capacity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBatchScreen(
    onNavigateBack: () -> Unit,
    onBatchCreated: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CreateBatchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSuccess, uiState.createdBatchId) {
        if (uiState.isSuccess) {
            uiState.createdBatchId?.let { onBatchCreated(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.create_batch_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        modifier = modifier
    ) { padding ->
        CreateBatchContent(
            uiState = uiState,
            onNameChange = viewModel::onNameChanged,
            onDescriptionChange = viewModel::onDescriptionChanged,
            onMaxStudentsChange = viewModel::onMaxStudentsChanged,
            onSubmit = viewModel::createBatch,
            modifier = Modifier.padding(padding)
        )
    }
}

@Composable
private fun CreateBatchContent(
    uiState: CreateBatchUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onMaxStudentsChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (uiState.isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        uiState.error?.let {
            Text(
                text = stringResource(R.string.batch_error_prefix, it),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        CreateBatchForm(
            uiState = uiState,
            onNameChange = onNameChange,
            onDescriptionChange = onDescriptionChange,
            onMaxStudentsChange = onMaxStudentsChange,
            modifier = Modifier.weight(1f)
        )

        CreateBatchButton(
            isLoading = uiState.isLoading,
            onClick = onSubmit
        )
    }
}

@Composable
private fun CreateBatchForm(
    uiState: CreateBatchUiState,
    onNameChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onMaxStudentsChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = uiState.name,
            onValueChange = onNameChange,
            label = { Text(stringResource(R.string.batch_name_label)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = !uiState.isLoading
        )

        OutlinedTextField(
            value = uiState.description,
            onValueChange = onDescriptionChange,
            label = { Text(stringResource(R.string.batch_description_label)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 5,
            enabled = !uiState.isLoading
        )

        OutlinedTextField(
            value = uiState.maxStudents,
            onValueChange = onMaxStudentsChange,
            label = { Text(stringResource(R.string.batch_max_students_label)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            enabled = !uiState.isLoading
        )
    }
}

@Composable
private fun CreateBatchButton(
    isLoading: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = !isLoading
    ) {
        Text(
            text = if (isLoading) {
                stringResource(R.string.batch_creating)
            } else {
                stringResource(R.string.batch_create_button)
            }
        )
    }
}
