package com.ssbmax.shared.ui.study

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.presentation.study.StudyCategory
import com.ssbmax.shared.presentation.study.StudyMaterialsViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.study_browse_by_category
import ssbmax.shared.generated.resources.study_materials_title

/**
 * KMP port of the Android `app/.../ui/study/StudyMaterialsScreen.kt` -- browse
 * study materials by category. Private composables (header/card) extracted
 * into [StudyMaterialsComponents] to stay under the 300-line Quality Limit.
 * See [StudyMaterialsViewModel] for the ViewModel-level porting notes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMaterialsScreen(
    onNavigateToTopic: (String) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToBookmarks: () -> Unit = {},
    viewModel: StudyMaterialsViewModel = koinInject(),
    modifier: Modifier = Modifier
) {
    DisposableEffect(Unit) {
        onDispose { viewModel.close() }
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.study_materials_title)) },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { StudyMaterialsHeader(totalArticles = uiState.totalArticles) }

            item {
                Text(
                    text = stringResource(Res.string.study_browse_by_category),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(uiState.categories) { category ->
                CategoryCardVertical(
                    category = category,
                    onClick = { onNavigateToTopic(getCategoryTopicName(category.type)) }
                )
            }
        }
    }
}

private fun getCategoryTopicName(category: StudyCategory): String {
    return when (category) {
        StudyCategory.OIR_PREP -> "OIR"
        StudyCategory.PPDT_TECHNIQUES -> "PPDT"
        StudyCategory.PSYCHOLOGY_TESTS -> "PSYCHOLOGY"
        StudyCategory.PIQ_PREP -> "PIQ"
        StudyCategory.GTO_TASKS -> "GTO"
        StudyCategory.INTERVIEW_PREP -> "INTERVIEW"
        else -> "GENERAL"
    }
}
