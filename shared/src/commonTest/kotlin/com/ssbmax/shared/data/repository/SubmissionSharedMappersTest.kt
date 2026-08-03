package com.ssbmax.shared.data.repository

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins [OLQAnalysisResultDto]'s missing-field default for `overallScore` at `5f` — see the DTO's
 * own class doc for why (matches TAT/WAT/SRT/SDT's actual Android code path, `PsychTestMapper` →
 * `OLQResultMapper.parseSharedOLQResult`, not PPDT's separate `SubmissionMappers.parseOLQResult`,
 * which used `0f`). Regression guard against silently drifting this back to `0f`, which would only
 * be correct for 1 of the 5 OLQ-scored test types sharing this DTO.
 */
class SubmissionSharedMappersTest {

    @Test
    fun olqAnalysisResultDto_overallScoreDefaultsToFive_matchingTatWatSrtSdtAndroidOriginal() {
        val dto = OLQAnalysisResultDto()
        assertEquals(5f, dto.overallScore)
    }
}
