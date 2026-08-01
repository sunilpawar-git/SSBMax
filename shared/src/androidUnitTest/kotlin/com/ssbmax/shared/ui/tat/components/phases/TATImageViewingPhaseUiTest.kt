package com.ssbmax.shared.ui.tat.components.phases

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.runComposeUiTest
import com.ssbmax.shared.testing.ensureComposeResourcesContextInitialized
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Port of the pre-KMP-convergence
 * `app/src/androidTest/.../ui/tests/tat/TATImageCardAspectRatioTest.kt` onto
 * shared's [TATImageViewingPhase] (Phase 6a of the KMP-convergence plan).
 *
 * Real structural drift from the pre-cutover test this replaces: the
 * `Modifier.testTag("tat_image_card")`/`testTag("tat_blank_slide_card")`
 * tags the original test asserted on were dropped in the KMP port (verified
 * by reading `TATImageViewingPhase.kt` -- neither `TATImageCard` nor
 * `TATBlankSlideCard` carry a testTag anymore). The regression this test
 * protects -- picture #12 renders the blank-slide placeholder instead of
 * attempting to load an image, both using the same fixed 4:3 aspect ratio --
 * is now asserted through visible content instead: the picture's content
 * description exists for a normal picture and is absent for the blank slide,
 * and vice versa for the blank-slide instruction text.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
class TATImageViewingPhaseUiTest {

    companion object {
        private const val IMAGE_URL_TEST = "https://example.com/tat-test.jpg"
    }

    @Before
    fun setup() {
        ensureComposeResourcesContextInitialized()
    }

    @Test
    fun imageViewingPhase_regularPicture_showsPictureContentDescription() = runComposeUiTest {
        setContent {
            TATImageViewingPhase(imageUrl = IMAGE_URL_TEST, timeRemaining = 30, sequenceNumber = 1)
        }

        onNodeWithContentDescription("TAT picture 1", substring = true).assertIsDisplayed()
    }

    @Test
    fun imageViewingPhase_blankSlide_showsInstructionInsteadOfPicture() = runComposeUiTest {
        setContent {
            TATImageViewingPhase(imageUrl = IMAGE_URL_TEST, timeRemaining = 30, sequenceNumber = 12)
        }

        // Blank slide (picture 12) shows the imagination-prompt text, not an
        // AsyncImage with a picture content description.
        onNodeWithText("No picture", substring = true).assertIsDisplayed()
        onNodeWithContentDescription("TAT picture", substring = true).assertDoesNotExist()
    }

    @Test
    fun imageViewingPhase_midSequencePicture_showsPictureContentDescription() = runComposeUiTest {
        setContent {
            TATImageViewingPhase(imageUrl = IMAGE_URL_TEST, timeRemaining = 30, sequenceNumber = 6)
        }

        onNodeWithContentDescription("TAT picture 6", substring = true).assertIsDisplayed()
    }

    @Test
    fun imageViewingPhase_lastRegularPicture_showsPictureContentDescription() = runComposeUiTest {
        setContent {
            TATImageViewingPhase(imageUrl = IMAGE_URL_TEST, timeRemaining = 30, sequenceNumber = 11)
        }

        onNodeWithContentDescription("TAT picture 11", substring = true).assertIsDisplayed()
    }

    @Test
    fun imageViewingPhase_viewingInstructionsAndTimerDisplayed() = runComposeUiTest {
        setContent {
            TATImageViewingPhase(imageUrl = IMAGE_URL_TEST, timeRemaining = 20, sequenceNumber = 3)
        }

        onNodeWithText("Observe carefully").assertIsDisplayed()
        onNodeWithText("20", substring = true).assertIsDisplayed()
    }

    @Test
    fun imageViewingPhase_pictureNumber_reflectsSequenceNumber() = runComposeUiTest {
        setContent {
            TATImageViewingPhase(imageUrl = IMAGE_URL_TEST, timeRemaining = 30, sequenceNumber = 1)
        }

        onNodeWithText("Picture 1 of 12", substring = true).assertIsDisplayed()
    }
}
