package com.ssbmax.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.ssbmax.R
import com.ssbmax.core.domain.model.UserRole
import com.ssbmax.navigation.SSBMaxDestinations

/**
 * Bottom Navigation Bar for SSBMax app
 * Shows different items based on user role
 */
@Composable
fun SSBMaxBottomBar(
    currentRoute: String,
    userRole: UserRole,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        val items = when {
            userRole.isStudent -> studentBottomNavItems
            userRole.isInstructor -> instructorBottomNavItems
            else -> studentBottomNavItems // Default to student
        }
        
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(stringResource(item.labelResId))
                },
                alwaysShowLabel = true
            )
        }
    }
}

/**
 * Bottom navigation item data class
 */
data class BottomNavItem(
    val route: String,
    val label: String, // Keeping for content description
    val labelResId: Int, // String resource ID for display
    val icon: ImageVector
)

/**
 * Student bottom navigation items
 */
private val studentBottomNavItems = listOf(
    BottomNavItem(
        route = SSBMaxDestinations.StudentHome.route,
        label = "Home",
        labelResId = R.string.nav_home,
        icon = Icons.Default.Home
    ),
    BottomNavItem(
        route = SSBMaxDestinations.StudentTests.route,
        label = "Tests",
        labelResId = R.string.nav_tests,
        icon = Icons.Default.Quiz
    ),
    BottomNavItem(
        route = SSBMaxDestinations.StudentSubmissions.route,
        label = "Results",
        labelResId = R.string.nav_results,
        icon = Icons.Default.Assessment
    ),
    BottomNavItem(
        route = SSBMaxDestinations.StudentStudy.route,
        label = "Study",
        labelResId = R.string.nav_study,
        icon = Icons.AutoMirrored.Filled.MenuBook
    ),
    BottomNavItem(
        route = SSBMaxDestinations.StudentProfile.route,
        label = "Profile",
        labelResId = R.string.nav_profile,
        icon = Icons.Default.Person
    )
)

/**
 * Instructor bottom navigation items
 */
private val instructorBottomNavItems = listOf(
    BottomNavItem(
        route = SSBMaxDestinations.InstructorHome.route,
        label = "Home",
        labelResId = R.string.nav_home,
        icon = Icons.Default.Dashboard
    ),
    BottomNavItem(
        route = SSBMaxDestinations.InstructorStudents.route,
        label = "Students",
        labelResId = R.string.nav_students,
        icon = Icons.Default.People
    ),
    BottomNavItem(
        route = SSBMaxDestinations.InstructorGrading.route,
        label = "Grading",
        labelResId = R.string.nav_grading,
        icon = Icons.Default.AssignmentTurnedIn
    ),
    BottomNavItem(
        route = SSBMaxDestinations.InstructorAnalytics.route,
        label = "Analytics",
        labelResId = R.string.nav_analytics,
        icon = Icons.Default.BarChart
    )
)

