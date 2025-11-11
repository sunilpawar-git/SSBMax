package com.ssbmax.ui.study

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Study Materials Screen - Browse study materials by category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMaterialsScreen(
    onNavigateToTopic: (String) -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToBookmarks: () -> Unit = {},
    viewModel: StudyMaterialsViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Materials") },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, "Search")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Section
            item {
                StudyMaterialsHeader(
                    totalArticles = uiState.totalArticles
                )
            }
            
            // Categories Section Header
            item {
                Text(
                    text = "Browse by Category",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Vertical Scrollable Category Cards
            items(uiState.categories) { category ->
                CategoryCardVertical(
                    category = category,
                    onClick = { onNavigateToTopic(getCategoryTopicName(category.type)) }
                )
            }
        }
    }
}

@Composable
private fun StudyMaterialsHeader(
    totalArticles: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.MenuBook,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Column {
                    Text(
                        text = "Comprehensive SSB Preparation",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Access study materials, guides, and tips",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            InfoChip(
                icon = Icons.AutoMirrored.Filled.Article,
                label = "$totalArticles Articles"
            )
        }
    }
}

@Composable
private fun InfoChip(
    icon: ImageVector,
    label: String
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun CategoryCardVertical(
    category: StudyCategoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            category.backgroundColor,
                            category.backgroundColor.copy(alpha = 0.7f)
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon Circle
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(category.iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = category.iconColor
                )
            }
            
            // Content
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = category.textColor,
                        modifier = Modifier.weight(1f)
                    )
                    
                    if (category.isPremium) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Premium",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
                
                Text(
                    text = "${category.articleCount} ${if (category.articleCount == 1) "article" else "articles"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = category.textColor.copy(alpha = 0.7f)
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = category.iconColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

// Helper function to map category to topic name
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

// Data Classes
data class StudyCategoryItem(
    val type: StudyCategory,
    val title: String,
    val icon: ImageVector,
    val articleCount: Int,
    val isPremium: Boolean,
    val backgroundColor: androidx.compose.ui.graphics.Color,
    val iconColor: androidx.compose.ui.graphics.Color,
    val textColor: androidx.compose.ui.graphics.Color
)

enum class StudyCategory {
    OIR_PREP,
    PPDT_TECHNIQUES,
    PSYCHOLOGY_TESTS,
    PIQ_PREP,
    GTO_TASKS,
    INTERVIEW_PREP,
    GENERAL_TIPS,
    CURRENT_AFFAIRS,
    PHYSICAL_FITNESS
}

