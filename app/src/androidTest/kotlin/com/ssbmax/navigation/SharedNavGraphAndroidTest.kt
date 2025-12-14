package com.ssbmax.navigation

import androidx.activity.ComponentActivity
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4

@RunWith(AndroidJUnit4::class)
class SharedNavGraphAndroidTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun startDestination_shows_phase1_detail() {
        composeRule.setContent {
            val context = LocalContext.current
            val navController = TestNavHostController(context)
            LaunchedEffect(Unit) {
                navController.navigatorProvider.addNavigator(ComposeNavigator())
            }

            NavHost(
                navController = navController,
                startDestination = SSBMaxDestinations.Phase1Detail.route
            ) {
                sharedNavGraph(navController)
            }
        }

        composeRule.onNodeWithText("Phase 1 - Screening").assertIsDisplayed()
    }
}






