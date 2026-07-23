package com.ssbmax.shared.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.ssbmax.navigation.BottomNavItem
import com.ssbmax.shared.domain.model.UserRole
import org.jetbrains.compose.resources.stringResource

/**
 * KMP port of the Android `app/.../ui/components/SSBMaxBottomBar.kt`.
 *
 * Deviation from the Android original, named explicitly: in `app`,
 * `SSBMaxScaffold.shouldShowBottomBar` is hardcoded `return false` -- the
 * Android bottom bar is wired but never actually shown (dead UI, confirmed
 * by reading `app/.../ui/components/SSBMaxScaffold.kt`). This port makes it
 * real: [com.ssbmax.shared.ui.components.SSBMaxAppScaffold] shows it for
 * every authenticated top-level destination (the 5 student / 4 instructor
 * routes below), closing this migration's own named gap ("nav drawer +
 * bottom nav bar not ported") rather than porting forward the Android
 * original's disabled state.
 *
 * Item route/label pairs now come from [BottomNavItem] (`shared`'s
 * `SSBMaxDestinations.kt`) instead of a second parallel list -- that sealed
 * class previously had no real caller anywhere (dead data, not wired to any
 * UI), which is also why its `title` was still a hardcoded `String` until
 * this session (see [BottomNavItem]'s own class doc for that fix). Icons
 * stay local to this file since they're a UI-only concern the domain-shaped
 * `BottomNavItem` doesn't need to carry.
 */
@Composable
fun SSBMaxBottomBar(
    currentRoute: String,
    userRole: UserRole,
    onNavigate: (String) -> Unit
) {
    NavigationBar {
        val items = if (userRole.isInstructor && !userRole.isStudent) {
            instructorBottomNavItems
        } else {
            studentBottomNavItems
        }

        items.forEach { (item, icon) ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { onNavigate(item.route) },
                icon = { Icon(imageVector = icon, contentDescription = null) },
                label = { Text(stringResource(item.titleRes)) },
                alwaysShowLabel = true
            )
        }
    }
}

private val studentBottomNavItems: List<Pair<BottomNavItem, ImageVector>> = listOf(
    BottomNavItem.StudentHome to Icons.Default.Home,
    BottomNavItem.StudentTests to Icons.Default.Quiz,
    BottomNavItem.StudentSubmissions to Icons.Default.Assessment,
    BottomNavItem.StudentStudy to Icons.AutoMirrored.Filled.MenuBook,
    BottomNavItem.StudentProfile to Icons.Default.Person
)

private val instructorBottomNavItems: List<Pair<BottomNavItem, ImageVector>> = listOf(
    BottomNavItem.InstructorHome to Icons.Default.Dashboard,
    BottomNavItem.InstructorStudents to Icons.Default.People,
    BottomNavItem.InstructorGrading to Icons.Default.AssignmentTurnedIn,
    BottomNavItem.InstructorAnalytics to Icons.Default.BarChart
)
