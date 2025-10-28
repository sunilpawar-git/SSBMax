package com.ssbmax.ui.study

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.ui.components.BreadcrumbBar
import com.ssbmax.ui.components.BreadcrumbItem
import com.ssbmax.ui.components.MarkdownText

/**
 * Study Material Detail Screen
 * Displays rich content with reading progress tracking and bookmarking
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyMaterialDetailScreen(
    categoryId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRelatedMaterial: (String) -> Unit = {},
    viewModel: StudyMaterialDetailViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val materialId = categoryId // Alias for compatibility
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Track reading progress based on scroll position
    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        if (listState.layoutInfo.totalItemsCount > 0) {
            val progress = (listState.firstVisibleItemIndex.toFloat() / 
                listState.layoutInfo.totalItemsCount.toFloat()) * 100f
            viewModel.updateProgress(progress)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study Material") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        val shareText = "Check out this SSB study material: ${uiState.material?.title}\n\n" +
                            "Study with SSBMax - Your complete SSB preparation companion"
                        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(android.content.Intent.EXTRA_SUBJECT, "SSB Study Material")
                            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
                    }) {
                        Icon(Icons.Default.Share, "Share")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = uiState.error ?: "Error loading material",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            uiState.material?.let { material ->
                LazyColumn(
                    state = listState,
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Breadcrumb Navigation
                    item {
                        BreadcrumbBar(
                            items = listOf(
                                BreadcrumbItem("Study Materials", null, isClickable = true),
                                BreadcrumbItem(material.category, null, isClickable = false),
                                BreadcrumbItem(material.title, null, isClickable = false)
                            ),
                            onItemClick = { item ->
                                if (item.title == "Study Materials") {
                                    onNavigateBack()
                                }
                            }
                        )
                    }
                    
                    // Reading Progress Indicator
                    item {
                        LinearProgressIndicator(
                            progress = { uiState.readingProgress / 100f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    
                    // Header Card
                    item {
                        MaterialHeaderCard(material = material)
                    }
                    
                    // Content
                    item {
                        ContentCard(content = material.content)
                    }
                    
                    // Tags
                    if (material.tags.isNotEmpty()) {
                        item {
                            TagsSection(tags = material.tags)
                        }
                    }
                    
                    // Related Materials
                    if (material.relatedMaterials.isNotEmpty()) {
                        item {
                            Text(
                                "Related Materials",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        
                        items(material.relatedMaterials) { relatedMaterial ->
                            RelatedMaterialCard(
                                material = relatedMaterial,
                                onClick = { onNavigateToRelatedMaterial(relatedMaterial.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialHeaderCard(
    material: StudyMaterialContent,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = material.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = material.author,
                    style = MaterialTheme.typography.bodyMedium
                )
                
                Icon(
                    Icons.Default.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = material.readTime,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = material.category,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun ContentCard(
    content: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Use centralized markdown renderer for consistent formatting
            MarkdownText(content = content)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsSection(
    tags: List<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Tags",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tags.filter { it.isNotBlank() }.forEach { tag ->
                AssistChip(
                    onClick = { /* TODO: Filter by tag */ },
                    label = { Text(tag) },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Tag,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun RelatedMaterialCard(
    material: RelatedMaterial,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Open",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

