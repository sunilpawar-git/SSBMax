package com.ssbmax.shared.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasStateDescription


import androidx.compose.ui.test.runComposeUiTest
import com.ssbmax.shared.domain.model.AppTheme
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Phase 1 executable contract for theme roles and baseline semantics. */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class SemanticUiStandardsTest {

    @Test
    fun `all semantic roles resolve in light and dark themes`() = runComposeUiTest {
        var light: SemanticColors? = null
        var dark: SemanticColors? = null

        setContent {
            SSBMaxTheme(appTheme = AppTheme.LIGHT) { light = MaterialTheme.semanticColors }
            SSBMaxTheme(appTheme = AppTheme.DARK) { dark = MaterialTheme.semanticColors }
        }
        waitForIdle()

        assertSemanticColorsAreUsable(light)
        assertSemanticColorsAreUsable(dark)
        assertNotEquals(light?.selected, dark?.selected)
    }

    @Test
    fun `icon-only action has an accessible name and decorative icon is silent`() = runComposeUiTest {
        setContent { IconSemanticsFixture() }

        onNode(hasContentDescription(OPEN_MENU_LABEL)).assertExists()
        onAllNodes(hasContentDescription(DECORATIVE_ICON_LABEL)).assertCountEquals(0)
    }

    @Test
    fun `test answer semantics do not expose the correct answer`() = runComposeUiTest {
        setContent { AnswerSemanticsFixture(optionLabel = OPTION_B_LABEL, correctAnswer = OPTION_B_LABEL) }

        onNode(hasContentDescription(OPTION_B_LABEL)).assertExists()
        onNode(hasContentDescription("Correct answer: $OPTION_B_LABEL")).assertDoesNotExist()
    }

    @Test
    fun `state semantics expose loading error selected and disabled states`() = runComposeUiTest {
        setContent { StateSemanticsFixture() }

        onNode(hasStateDescription(LOADING_STATE)).assertExists()
        onNode(hasStateDescription(ERROR_STATE)).assertExists()
        onNode(hasStateDescription(SELECTED_STATE)).assertExists()
        onNode(hasStateDescription(DISABLED_STATE)).assertExists()
    }

    private fun assertSemanticColorsAreUsable(colors: SemanticColors?) {
        val resolved = colors
        assertNotNull(resolved)
        val roles = listOf(
            resolved.success, resolved.error, resolved.warning, resolved.informational,
            resolved.selected, resolved.disabled, resolved.skipped, resolved.testProgress,
            resolved.onSuccess, resolved.onError, resolved.onWarning, resolved.onInformational,
            resolved.onSelected, resolved.onDisabled, resolved.onSkipped, resolved.onTestProgress
        )
        roles.forEach { color -> assertNotEquals(androidx.compose.ui.graphics.Color.Unspecified, color) }
    }

    @Composable
    private fun IconSemanticsFixture() {
        SSBMaxTheme {
            Icon(Icons.Default.Star, contentDescription = null)
            IconButton(onClick = {}, modifier = Modifier) {
                Icon(Icons.Default.Menu, contentDescription = OPEN_MENU_LABEL)
            }
        }
    }

    @Composable
    private fun AnswerSemanticsFixture(optionLabel: String, correctAnswer: String) {
        SSBMaxTheme {
            // The correct answer is intentionally unused: active-test semantics must stay safe.
            Text(
                text = optionLabel,
                modifier = Modifier.semantics {
                    contentDescription = optionLabel
                }
            )
        }
    }

    @Composable
    private fun StateSemanticsFixture() {
        SSBMaxTheme {
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.semantics {
                stateDescription = LOADING_STATE
            })
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.semantics {
                stateDescription = ERROR_STATE
            })
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.semantics {
                stateDescription = SELECTED_STATE
            })
            Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.semantics {
                stateDescription = DISABLED_STATE
            })
        }
    }

    private companion object {
        const val OPEN_MENU_LABEL = "Open menu"
        const val DECORATIVE_ICON_LABEL = "Decorative icon"
        const val OPTION_B_LABEL = "Option B"
        const val LOADING_STATE = "Loading"
        const val ERROR_STATE = "Error"
        const val SELECTED_STATE = "Selected"
        const val DISABLED_STATE = "Disabled"
    }
}
