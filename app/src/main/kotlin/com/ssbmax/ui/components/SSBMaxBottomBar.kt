package com.ssbmax.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
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
                    Text(item.label)
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
    val label: String,
    val icon: ImageVector
)

/**
 * Student bottom navigation items
 */
private val studentBottomNavItems = listOf(
    BottomNavItem(
        route = SSBMaxDestinations.StudentHome.route,
        label = "Home",
        icon = Icons.Default.Home
    ),
    BottomNavItem(
        route = SSBMaxDestinations.StudentTests.route,
        label = "Tests",
        icon = Icons.Default.Quiz
    ),
    BottomNavItem(
        route = SSBMaxDestinations.StudentSubmissions.route,
        label = "Results",
        icon = Icons.Default.Assessment
    ),
    BottomNavItem(
        route = SSBMaxDestinations.StudentStudy.route,
        label = "Study",
        icon = Icons.AutoMirrored.Filled.MenuBook
    ),
    BottomNavItem(
        route = SSBMaxDestinations.StudentProfile.route,
        label = "Profile",
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
        icon = Icons.Default.Dashboard
    ),
    BottomNavItem(
        route = SSBMaxDestinations.InstructorStudents.route,
        label = "Students",
        icon = Icons.Default.People
    ),
    BottomNavItem(
        route = SSBMaxDestinations.InstructorGrading.route,
        label = "Grading",
        icon = Icons.Default.AssignmentTurnedIn
    ),
    BottomNavItem(
        route = SSBMaxDestinations.InstructorAnalytics.route,
        label = "Analytics",
        icon = Icons.Default.BarChart
    )
)

