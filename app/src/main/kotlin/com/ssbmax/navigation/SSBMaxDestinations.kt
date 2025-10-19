package com.ssbmax.navigation

/**
 * Type-safe navigation destinations for SSBMax app
 */
sealed class SSBMaxDestinations(val route: String) {
    // Authentication Flow
    data object Splash : SSBMaxDestinations("splash")
    data object Login : SSBMaxDestinations("login")
    data object RoleSelection : SSBMaxDestinations("role_selection")
    
    // Student Flow
    data object StudentHome : SSBMaxDestinations("student/home")
    data object StudentTests : SSBMaxDestinations("student/tests")
    data object StudentSubmissions : SSBMaxDestinations("student/submissions")
    data object StudentStudy : SSBMaxDestinations("student/study")
    data object StudentProfile : SSBMaxDestinations("student/profile")
    
    // Instructor Flow
    data object InstructorHome : SSBMaxDestinations("instructor/home")
    data object InstructorStudents : SSBMaxDestinations("instructor/students")
    data object InstructorGrading : SSBMaxDestinations("instructor/grading")
    data object InstructorAnalytics : SSBMaxDestinations("instructor/analytics")
    
    // Phase Screens
    data object Phase1Detail : SSBMaxDestinations("phase1/detail")
    data object Phase2Detail : SSBMaxDestinations("phase2/detail")
    
    // Test Screens - Phase 1
    data object OIRTest : SSBMaxDestinations("test/oir/{testId}") {
        fun createRoute(testId: String) = "test/oir/$testId"
    }
    data object OIRTestResult : SSBMaxDestinations("test/oir/result/{sessionId}") {
        fun createRoute(sessionId: String) = "test/oir/result/$sessionId"
    }
    data object PPDTTest : SSBMaxDestinations("test/ppdt/{testId}") {
        fun createRoute(testId: String) = "test/ppdt/$testId"
    }
    data object PPDTSubmissionResult : SSBMaxDestinations("test/ppdt/result/{submissionId}") {
        fun createRoute(submissionId: String) = "test/ppdt/result/$submissionId"
    }
    
    // Test Screens - Phase 2 Psychology Tests
    data object TATTest : SSBMaxDestinations("test/tat/{testId}") {
        fun createRoute(testId: String) = "test/tat/$testId"
    }
    data object TATSubmissionResult : SSBMaxDestinations("test/tat/result/{submissionId}") {
        fun createRoute(submissionId: String) = "test/tat/result/$submissionId"
    }
    data object WATTest : SSBMaxDestinations("test/wat/{testId}") {
        fun createRoute(testId: String) = "test/wat/$testId"
    }
    data object WATSubmissionResult : SSBMaxDestinations("test/wat/result/{submissionId}") {
        fun createRoute(submissionId: String) = "test/wat/result/$submissionId"
    }
    data object SRTTest : SSBMaxDestinations("test/srt/{testId}") {
        fun createRoute(testId: String) = "test/srt/$testId"
    }
    data object SRTSubmissionResult : SSBMaxDestinations("test/srt/result/{submissionId}") {
        fun createRoute(submissionId: String) = "test/srt/result/$submissionId"
    }
    
    // Test Screens - Phase 2 Other Tests
    data object GTOTest : SSBMaxDestinations("test/gto/{testId}") {
        fun createRoute(testId: String) = "test/gto/$testId"
    }
    data object IOTest : SSBMaxDestinations("test/io/{testId}") {
        fun createRoute(testId: String) = "test/io/$testId"
    }
    
    // Study Materials
    data object StudyMaterialsList : SSBMaxDestinations("study/materials")
    data object StudyMaterialDetail : SSBMaxDestinations("study/material/{categoryId}") {
        fun createRoute(categoryId: String) = "study/material/$categoryId"
    }
    
    // Topic Screens (Phase > Topic with tabs)
    data object TopicScreen : SSBMaxDestinations("topic/{topicId}") {
        fun createRoute(topicId: String) = "topic/$topicId"
    }
    
    // Premium/Subscription
    data object UpgradeScreen : SSBMaxDestinations("premium/upgrade")
    
    // Notifications
    data object NotificationCenter : SSBMaxDestinations("notifications/center")
    
    // Settings
    data object Settings : SSBMaxDestinations("settings")
    
    // Batch Management
    data object JoinBatch : SSBMaxDestinations("batch/join")
    data object CreateBatch : SSBMaxDestinations("batch/create")
    data object BatchDetail : SSBMaxDestinations("batch/{batchId}") {
        fun createRoute(batchId: String) = "batch/$batchId"
    }
    
    // Student Details (for instructors)
    data object StudentDetail : SSBMaxDestinations("instructor/student/{studentId}") {
        fun createRoute(studentId: String) = "instructor/student/$studentId"
    }
    
    // Test Grading (for instructors)
    data object InstructorGradingDetail : SSBMaxDestinations("instructor/grading/{submissionId}") {
        fun createRoute(submissionId: String) = "instructor/grading/$submissionId"
    }
    
    // Submission Detail (for students - view their own submission)
    data object SubmissionDetail : SSBMaxDestinations("submission/{submissionId}") {
        fun createRoute(submissionId: String) = "submission/$submissionId"
    }
}

/**
 * Navigation drawer items for quick access
 */
sealed class DrawerItem(
    val route: String,
    val title: String,
    val description: String? = null
) {
    // Student Drawer Items
    data object Phase1Tests : DrawerItem("drawer/phase1", "Phase 1 Tests", "OIR & PPDT")
    data object Phase2Tests : DrawerItem("drawer/phase2", "Phase 2 Tests", "Psychology, GTO, IO")
    data object AllTests : DrawerItem("drawer/all_tests", "All Tests")
    data object StudyMaterials : DrawerItem("drawer/study", "Study Materials")
    data object MyBatches : DrawerItem("drawer/batches", "My Batches")
    
    // Instructor Drawer Items
    data object PendingGrading : DrawerItem("drawer/pending", "Pending Grading")
    data object MyStudents : DrawerItem("drawer/students", "My Students")
    data object BatchManagement : DrawerItem("drawer/batch_mgmt", "Batch Management")
    data object Analytics : DrawerItem("drawer/analytics", "Analytics Dashboard")
}

/**
 * Bottom navigation items
 */
sealed class BottomNavItem(
    val route: String,
    val title: String
) {
    // Student Bottom Nav
    data object StudentHome : BottomNavItem(SSBMaxDestinations.StudentHome.route, "Home")
    data object StudentTests : BottomNavItem(SSBMaxDestinations.StudentTests.route, "Tests")
    data object StudentSubmissions : BottomNavItem(SSBMaxDestinations.StudentSubmissions.route, "Results")
    data object StudentStudy : BottomNavItem(SSBMaxDestinations.StudentStudy.route, "Study")
    data object StudentProfile : BottomNavItem(SSBMaxDestinations.StudentProfile.route, "Profile")
    
    // Instructor Bottom Nav
    data object InstructorHome : BottomNavItem(SSBMaxDestinations.InstructorHome.route, "Home")
    data object InstructorStudents : BottomNavItem(SSBMaxDestinations.InstructorStudents.route, "Students")
    data object InstructorGrading : BottomNavItem(SSBMaxDestinations.InstructorGrading.route, "Grading")
    data object InstructorAnalytics : BottomNavItem(SSBMaxDestinations.InstructorAnalytics.route, "Analytics")
}

