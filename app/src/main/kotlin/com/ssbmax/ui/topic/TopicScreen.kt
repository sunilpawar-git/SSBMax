package com.ssbmax.ui.topic

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.core.domain.model.TestType
import com.ssbmax.ui.components.TabSwipeableContent
import com.ssbmax.ui.components.MarkdownText

/**
 * Topic Screen with 3 tabs: Introduction, Study Material, Tests
 * Shows detailed information about a specific SSB test topic
 */
@Composable
fun TopicScreen(
    topicId: String,
    initialTab: Int = 0,
    onNavigateBack: () -> Unit,
    onNavigateToStudyMaterial: (String) -> Unit = {},
    onNavigateToTest: (String) -> Unit = {},
    viewModel: TopicViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val testType = topicId // Alias for compatibility
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(initialTab) }
    
    Scaffold(
        topBar = {
            TopicTopBar(
                title = uiState.topicTitle,
                testType = testType,
                onNavigateBack = onNavigateBack
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    label = { Text("Overview") },
                    alwaysShowLabel = true
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.MenuBook, contentDescription = null) },
                    label = { Text("Study Material") },
                    alwaysShowLabel = true
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Assignment, contentDescription = null) },
                    label = { Text(if (testType.equals("PIQ", ignoreCase = true) || testType.equals("PIQ_FORM", ignoreCase = true)) "Fill PIQ" else "Tests") },
                    alwaysShowLabel = true
                )
            }
        }
    ) { paddingValues ->
        // Tab Content with Swipe Gesture Support
        TabSwipeableContent(
            currentIndex = selectedTab,
            totalTabs = 3,
            onTabChange = { newTab -> selectedTab = newTab },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (selectedTab) {
                0 -> IntroductionTab(
                    introduction = uiState.introduction,
                    isLoading = uiState.isLoading
                )
                1 -> StudyMaterialTab(
                    materials = uiState.studyMaterials,
                    isLoading = uiState.isLoading,
                    onMaterialClick = onNavigateToStudyMaterial
                )
                2 -> TestsTab(
                    tests = uiState.availableTests,
                    topicId = topicId,
                    isLoading = uiState.isLoading,
                    onTestClick = { testType ->
                        // Convert TestType to testId string for navigation
                        val testId = "${testType.name.lowercase()}_standard"
                        onNavigateToTest(testId)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TopicTopBar(
    title: String,
    testType: String,
    onNavigateBack: () -> Unit
) {
    TopAppBar(
        title = {
            Column(modifier = Modifier.padding(end = 8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                // Breadcrumb
                Text(
                    text = "SSB Preparation > ${formatBreadcrumbText(testType)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

/**
 * Format breadcrumb text for display
 * - Keeps acronyms uppercase (OIR, PPDT, TAT, WAT, SRT, GTO, IO, PIQ)
 * - Converts other text to Title Case
 */
private fun formatBreadcrumbText(text: String): String {
    val upperCaseAcronyms = setOf("OIR", "PPDT", "TAT", "WAT", "SRT", "GTO", "IO", "PIQ", "SD")
    val normalized = text.trim().uppercase()
    
    return if (upperCaseAcronyms.contains(normalized)) {
        normalized
    } else {
        // Convert to title case for multi-word entries
        text.split("_", "-", " ")
            .joinToString(" ") { word ->
                word.lowercase().replaceFirstChar { it.uppercase() }
            }
    }
}

@Composable
private fun IntroductionTab(
    introduction: String,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Lightbulb,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Use formatted text renderer instead of plain text
                    MarkdownText(
                        content = introduction,
                        textColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyMaterialTab(
    materials: List<StudyMaterialItem>,
    isLoading: Boolean,
    onMaterialClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    if (materials.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No study materials available yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(materials) { material ->
            StudyMaterialCard(
                material = material,
                onClick = { onMaterialClick(material.id) }
            )
        }
    }
}

@Composable
private fun StudyMaterialCard(
    material: StudyMaterialItem,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                if (material.isPremium) Icons.Default.Lock else getStudyMaterialIcon(material.id),
                contentDescription = null,
                tint = if (material.isPremium)
                    MaterialTheme.colorScheme.tertiary
                else
                    getStudyMaterialColor(material.id), // Use diverse vibrant colors
                modifier = Modifier.size(40.dp)
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = material.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = material.duration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (material.isPremium) {
                AssistChip(
                    onClick = { },
                    label = { Text("Premium", style = MaterialTheme.typography.labelSmall) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
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

@Composable
private fun TestsTab(
    tests: List<TestType>,
    topicId: String,
    isLoading: Boolean,
    onTestClick: (TestType) -> Unit,
    modifier: Modifier = Modifier
) {
    if (isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }
    
    if (tests.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "No tests available yet",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (topicId.equals("GTO", ignoreCase = true)) {
            // Special handling for GTO tests - divided into Day 1 and Day 2
            val day1Tests = tests.take(4) // First 4 tests
            val day2Tests = tests.drop(4) // Last 4 tests

            // Day 1 Section
            item {
                DayHeader("Day 1")
            }
            items(day1Tests) { test ->
                TestCard(
                    test = test,
                    onClick = { onTestClick(test) }
                )
            }

            // Day 2 Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                DayHeader("Day 2")
            }
            items(day2Tests) { test ->
                TestCard(
                    test = test,
                    onClick = { onTestClick(test) }
                )
            }
        } else {
            // Default behavior for other topics
        items(tests) { test ->
            TestCard(
                test = test,
                onClick = { onTestClick(test) }
            )
        }
        }
    }
}

@Composable
private fun DayHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(
            thickness = 2.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
        )
    }
}

@Composable
private fun TestCard(
    test: TestType,
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
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = getTestColor(test).copy(alpha = 0.2f),
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        getTestIcon(test),
                        contentDescription = null,
                        tint = getTestColor(test),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = test.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = test.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }
            
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = "Start Test",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// Helper functions
private fun getTestColor(testType: TestType): Color {
    return when (testType) {
        TestType.OIR -> Color(0xFF1976D2)
        TestType.PPDT -> Color(0xFF388E3C)
        TestType.PIQ -> Color(0xFF009688)
        TestType.TAT -> Color(0xFFD32F2F)
        TestType.WAT -> Color(0xFFF57C00)
        TestType.SRT -> Color(0xFF7B1FA2)
        TestType.SD -> Color(0xFF0097A7)
        // GTO Tasks - vibrant colors consistent with app design
        TestType.GTO_GD -> Color(0xFF2196F3)      // Bright Blue
        TestType.GTO_GPE -> Color(0xFF4CAF50)     // Bright Green
        TestType.GTO_PGT -> Color(0xFFFF9800)     // Bright Orange
        TestType.GTO_GOR -> Color(0xFF9C27B0)     // Bright Purple
        TestType.GTO_HGT -> Color(0xFF00BCD4)     // Bright Cyan
        TestType.GTO_LECTURETTE -> Color(0xFF3F51B5) // Bright Indigo
        TestType.GTO_IO -> Color(0xFF8BC34A)      // Bright Light Green
        TestType.GTO_CT -> Color(0xFFFF5722)      // Bright Deep Orange
        TestType.IO -> Color(0xFF455A64)
    }
}

private fun getStudyMaterialColor(materialId: String): Color {
    // Create a diverse color palette for study materials
    // Use material ID to deterministically assign colors
    val colorPalette = listOf(
        Color(0xFF1976D2), // Bright Blue
        Color(0xFF4CAF50), // Bright Green
        Color(0xFFFF9800), // Bright Orange
        Color(0xFF9C27B0), // Bright Purple
        Color(0xFF00BCD4), // Bright Cyan
        Color(0xFF3F51B5), // Bright Indigo
        Color(0xFF8BC34A), // Bright Light Green
        Color(0xFFFF5722), // Bright Deep Orange
        Color(0xFFE91E63), // Bright Pink
        Color(0xFF009688), // Bright Teal
        Color(0xFFF44336), // Bright Red
        Color(0xFF673AB7), // Bright Deep Purple
        Color(0xFFCDDC39), // Bright Lime
        Color(0xFF795548), // Brown
        Color(0xFF607D8B)  // Blue Grey
    )

    // Use hash of material ID to get consistent color assignment
    val hash = materialId.hashCode()
    val colorIndex = (hash % colorPalette.size).let { if (it < 0) it + colorPalette.size else it }

    return colorPalette[colorIndex]
}

private fun getStudyMaterialIcon(materialId: String): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        // OIR Materials
        materialId.contains("oir_1") -> Icons.Default.Psychology // Understanding OIR Test Pattern
        materialId.contains("oir_2") -> Icons.Default.School // Verbal Reasoning Mastery
        materialId.contains("oir_3") -> Icons.Default.Extension // Non-Verbal Reasoning Strategies
        materialId.contains("oir_4") -> Icons.Default.Schedule // Time Management in OIR
        materialId.contains("oir_5") -> Icons.Default.Warning // Common Mistakes to Avoid
        materialId.contains("oir_6") -> Icons.Default.Assignment // Practice Sets with Solutions
        materialId.contains("oir_7") -> Icons.Default.Calculate // Mental Math Shortcuts

        // PPDT Materials
        materialId.contains("ppdt_1") -> Icons.Default.Info // PPDT Test Overview
        materialId.contains("ppdt_2") -> Icons.Default.Edit // Story Writing Techniques
        materialId.contains("ppdt_3") -> Icons.Default.Groups // Group Discussion Strategies
        materialId.contains("ppdt_4") -> Icons.Default.Visibility // Character Perception Skills
        materialId.contains("ppdt_5") -> Icons.Default.LibraryBooks // Sample PPDT Stories
        materialId.contains("ppdt_6") -> Icons.Default.Error // Common PPDT Mistakes

        // Psychology Materials
        materialId.contains("psy_1") -> Icons.Default.Psychology // Psychology Tests Overview
        materialId.contains("psy_2") -> Icons.Default.Lightbulb // TAT Mastery Guide
        materialId.contains("psy_3") -> Icons.Default.Chat // WAT Response Strategies
        materialId.contains("psy_4") -> Icons.Default.Extension // SRT Situation Analysis
        materialId.contains("psy_5") -> Icons.Default.Person // Self Description Writing
        materialId.contains("psy_6") -> Icons.Default.Star // Officer Like Qualities Explained
        materialId.contains("psy_7") -> Icons.Default.Assignment // Psychology Test Practice Sets
        materialId.contains("psy_8") -> Icons.Default.FitnessCenter // Psychological Mindset Development

        // PIQ Materials
        materialId.contains("piq_form") -> Icons.Default.Assignment // SSB PIQ Form (Reference)
        materialId.contains("piq_1") -> Icons.Default.Help // PIQ Form Guide
        materialId.contains("piq_2") -> Icons.Default.CheckCircle // Self-Consistency Tips
        materialId.contains("piq_3") -> Icons.Default.Warning // Common PIQ Mistakes

        // GTO Materials
        materialId.contains("gto_1") -> Icons.Default.Groups // GTO Tasks Overview
        materialId.contains("gto_2") -> Icons.Default.Forum // Group Discussion Mastery
        materialId.contains("gto_3") -> Icons.Default.TrendingUp // Progressive Group Task Tips
        materialId.contains("gto_4") -> Icons.Default.People // Half Group Task Techniques
        materialId.contains("gto_5") -> Icons.Default.Mic // Lecturette Preparation
        materialId.contains("gto_6") -> Icons.Default.Leaderboard // Command Task Leadership
        materialId.contains("gto_7") -> Icons.Default.DirectionsRun // Snake Race & FGT Strategies

        // Interview Materials
        materialId.contains("int_1") -> Icons.Default.RecordVoiceOver // SSB Interview Process
        materialId.contains("int_2") -> Icons.Default.Person // Personal Interview Preparation
        materialId.contains("int_3") -> Icons.Default.Public // Current Affairs Mastery
        materialId.contains("int_4") -> Icons.Default.MilitaryTech // Military Knowledge Basics
        materialId.contains("int_5") -> Icons.Default.Face // Interview Body Language
        materialId.contains("int_6") -> Icons.Default.QuestionAnswer // Common Interview Questions
        materialId.contains("int_7") -> Icons.Default.PlayCircle // Mock Interview Scenarios

        // Conference Materials
        materialId.contains("conf_") -> Icons.Default.AccountBalance // Conference related

        // Medicals Materials
        materialId.contains("med_") -> Icons.Default.LocalHospital // Medical related

        // SSB Overview Materials
        materialId.contains("ssb_") -> Icons.Default.Explore // SSB Overview

        // Default fallback
        else -> Icons.Default.MenuBook
    }
}

private fun getTestIcon(testType: TestType): androidx.compose.ui.graphics.vector.ImageVector {
    return when (testType) {
        TestType.OIR -> Icons.Default.Psychology
        TestType.PPDT -> Icons.Default.Image
        TestType.PIQ -> Icons.Default.Assignment
        TestType.TAT, TestType.WAT, TestType.SRT, TestType.SD -> Icons.Default.EditNote
        // GTO Tasks
        TestType.GTO_GD -> Icons.Default.Forum
        TestType.GTO_GPE -> Icons.Default.Map
        TestType.GTO_PGT -> Icons.Default.TrendingUp
        TestType.GTO_GOR -> Icons.Default.DirectionsRun
        TestType.GTO_HGT -> Icons.Default.People
        TestType.GTO_LECTURETTE -> Icons.Default.Mic
        TestType.GTO_IO -> Icons.Default.Person
        TestType.GTO_CT -> Icons.Default.MilitaryTech
        TestType.IO -> Icons.Default.RecordVoiceOver
    }
}

