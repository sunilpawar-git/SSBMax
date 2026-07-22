package com.ssbmax.shared.ui.instructorgrading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.batch_detail_features_list
import ssbmax.shared.generated.resources.batch_detail_features_title
import ssbmax.shared.generated.resources.batch_detail_placeholder_description
import ssbmax.shared.generated.resources.batch_detail_placeholder_title
import ssbmax.shared.generated.resources.batch_detail_title
import ssbmax.shared.generated.resources.create_batch_features_list
import ssbmax.shared.generated.resources.create_batch_features_title
import ssbmax.shared.generated.resources.create_batch_placeholder_description
import ssbmax.shared.generated.resources.create_batch_placeholder_title
import ssbmax.shared.generated.resources.create_batch_title
import ssbmax.shared.generated.resources.instructor_analytics_features_list
import ssbmax.shared.generated.resources.instructor_analytics_features_title
import ssbmax.shared.generated.resources.instructor_analytics_placeholder_description
import ssbmax.shared.generated.resources.instructor_analytics_placeholder_title
import ssbmax.shared.generated.resources.instructor_analytics_title
import ssbmax.shared.generated.resources.instructor_students_features_list
import ssbmax.shared.generated.resources.instructor_students_features_title
import ssbmax.shared.generated.resources.instructor_students_placeholder_description
import ssbmax.shared.generated.resources.instructor_students_placeholder_title
import ssbmax.shared.generated.resources.instructor_students_title
import ssbmax.shared.generated.resources.student_detail_features_list
import ssbmax.shared.generated.resources.student_detail_features_title
import ssbmax.shared.generated.resources.student_detail_placeholder_description
import ssbmax.shared.generated.resources.student_detail_placeholder_title
import ssbmax.shared.generated.resources.student_detail_title
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector as ComposeImageVector

/**
 * KMP ports of five Android "Coming Soon" instructor placeholder screens
 * (`app/.../ui/instructor/{BatchDetail,CreateBatch,InstructorAnalytics,
 * InstructorStudents,StudentDetail}Screen.kt`). None of these have real
 * ViewModels or repository wiring in the Android original either -- each is
 * a static card describing planned functionality, faithfully ported
 * unchanged (no new behavior added). Kept in one file since each is a small
 * (~50-line) unchanged structural clone of the same placeholder shape.
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorStudentsScreen(
    onNavigateBack: () -> Unit,
    onStudentClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    PlaceholderScaffold(
        title = stringResource(Res.string.instructor_students_title),
        onNavigateBack = onNavigateBack,
        icon = Icons.Default.Person,
        placeholderTitle = stringResource(Res.string.instructor_students_placeholder_title),
        placeholderDescription = stringResource(Res.string.instructor_students_placeholder_description),
        featuresTitle = stringResource(Res.string.instructor_students_features_title),
        featuresList = stringResource(Res.string.instructor_students_features_list),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstructorAnalyticsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    PlaceholderScaffold(
        title = stringResource(Res.string.instructor_analytics_title),
        onNavigateBack = onNavigateBack,
        icon = Icons.Default.BarChart,
        placeholderTitle = stringResource(Res.string.instructor_analytics_placeholder_title),
        placeholderDescription = stringResource(Res.string.instructor_analytics_placeholder_description),
        featuresTitle = stringResource(Res.string.instructor_analytics_features_title),
        featuresList = stringResource(Res.string.instructor_analytics_features_list),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateBatchScreen(
    onNavigateBack: () -> Unit,
    onBatchCreated: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    PlaceholderScaffold(
        title = stringResource(Res.string.create_batch_title),
        onNavigateBack = onNavigateBack,
        icon = Icons.Default.GroupAdd,
        placeholderTitle = stringResource(Res.string.create_batch_placeholder_title),
        placeholderDescription = stringResource(Res.string.create_batch_placeholder_description),
        featuresTitle = stringResource(Res.string.create_batch_features_title),
        featuresList = stringResource(Res.string.create_batch_features_list),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDetailScreen(
    batchId: String,
    onNavigateBack: () -> Unit,
    onNavigateToStudent: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    PlaceholderScaffold(
        title = stringResource(Res.string.batch_detail_title, batchId),
        onNavigateBack = onNavigateBack,
        icon = Icons.Default.Group,
        placeholderTitle = stringResource(Res.string.batch_detail_placeholder_title),
        placeholderDescription = stringResource(Res.string.batch_detail_placeholder_description, batchId),
        featuresTitle = stringResource(Res.string.batch_detail_features_title),
        featuresList = stringResource(Res.string.batch_detail_features_list),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentDetailScreen(
    studentId: String,
    onNavigateBack: () -> Unit,
    onNavigateToSubmission: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    PlaceholderScaffold(
        title = stringResource(Res.string.student_detail_title, studentId),
        onNavigateBack = onNavigateBack,
        icon = Icons.Default.PersonSearch,
        placeholderTitle = stringResource(Res.string.student_detail_placeholder_title),
        placeholderDescription = stringResource(Res.string.student_detail_placeholder_description, studentId),
        featuresTitle = stringResource(Res.string.student_detail_features_title),
        featuresList = stringResource(Res.string.student_detail_features_list),
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceholderScaffold(
    title: String,
    onNavigateBack: () -> Unit,
    icon: ComposeImageVector,
    placeholderTitle: String,
    placeholderDescription: String,
    featuresTitle: String,
    featuresList: String,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = placeholderTitle,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = placeholderDescription,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = featuresTitle, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = featuresList, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
