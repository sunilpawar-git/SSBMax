package com.ssbmax.shared.presentation.piq

import com.ssbmax.shared.domain.model.Education
import com.ssbmax.shared.domain.model.NCCTraining
import com.ssbmax.shared.domain.model.PIQAIScore
import com.ssbmax.shared.domain.model.PIQSubmission
import com.ssbmax.shared.domain.model.Sibling
import com.ssbmax.shared.domain.model.SubmissionStatus
import kotlin.random.Random

/**
 * Extracted from [PIQTestViewModel] to keep both files under the 300-line quality
 * limit -- a ~90-field form inherently produces a long flat mapping function.
 * Field-for-field port of `PIQTestViewModel.createSubmissionFromState()` /
 * `generateMockAIScore()` from the Android original.
 */
internal fun buildPIQSubmission(
    state: PIQTestUiState,
    userId: String,
    status: SubmissionStatus
): PIQSubmission {
    val answers = state.answers

    val siblings = mutableListOf<Sibling>()
    repeat(2) { index ->
        val prefix = "elderSibling${index + 1}_"
        val name = answers["${prefix}name"] ?: ""
        if (name.isNotBlank()) {
            siblings.add(
                Sibling(
                    name = name,
                    age = answers["${prefix}age"] ?: "",
                    education = answers["${prefix}education"] ?: "",
                    occupation = answers["${prefix}occupation"] ?: "",
                    income = answers["${prefix}income"] ?: ""
                )
            )
        }
    }
    repeat(2) { index ->
        val prefix = "youngerSibling${index + 1}_"
        val name = answers["${prefix}name"] ?: ""
        if (name.isNotBlank()) {
            siblings.add(
                Sibling(
                    name = name,
                    age = answers["${prefix}age"] ?: "",
                    education = answers["${prefix}education"] ?: "",
                    occupation = answers["${prefix}occupation"] ?: "",
                    income = answers["${prefix}income"] ?: ""
                )
            )
        }
    }

    val education10th = Education(
        level = "10th",
        institution = answers["education10th_institution"] ?: "",
        board = answers["education10th_board"] ?: "",
        year = answers["education10th_year"] ?: "",
        percentage = answers["education10th_percentage"] ?: "",
        mediumOfInstruction = answers["education10th_medium"] ?: "",
        boarderDayScholar = answers["education10th_boarder"] ?: "",
        outstandingAchievement = answers["education10th_achievement"] ?: ""
    )
    val education12th = Education(
        level = "12th",
        institution = answers["education12th_institution"] ?: "",
        board = answers["education12th_board"] ?: "",
        stream = answers["education12th_stream"] ?: "",
        year = answers["education12th_year"] ?: "",
        percentage = answers["education12th_percentage"] ?: "",
        mediumOfInstruction = answers["education12th_medium"] ?: "",
        boarderDayScholar = answers["education12th_boarder"] ?: "",
        outstandingAchievement = answers["education12th_achievement"] ?: ""
    )
    val educationGraduation = Education(
        level = "Graduation",
        institution = answers["educationGrad_institution"] ?: "",
        board = answers["educationGrad_university"] ?: "",
        year = answers["educationGrad_year"] ?: "",
        cgpa = answers["educationGrad_cgpa"] ?: "",
        mediumOfInstruction = answers["educationGrad_medium"] ?: "",
        boarderDayScholar = answers["educationGrad_boarder"] ?: "",
        outstandingAchievement = answers["educationGrad_achievement"] ?: ""
    )
    val educationPostGraduation = Education(
        level = "Post-Graduation",
        institution = answers["educationPG_institution"] ?: "",
        board = answers["educationPG_university"] ?: "",
        year = answers["educationPG_year"] ?: "",
        cgpa = answers["educationPG_cgpa"] ?: "",
        mediumOfInstruction = answers["educationPG_medium"] ?: "",
        boarderDayScholar = answers["educationPG_boarder"] ?: "",
        outstandingAchievement = answers["educationPG_achievement"] ?: ""
    )

    val nccTraining = NCCTraining(
        hasTraining = answers["ncc_hasTraining"]?.toBoolean() ?: false,
        totalTraining = answers["ncc_totalTraining"] ?: "",
        wing = answers["ncc_wing"] ?: "",
        division = answers["ncc_division"] ?: "",
        certificateObtained = answers["ncc_certificate"] ?: ""
    )

    return PIQSubmission(
        userId = userId,
        testId = state.testId,
        oirNumber = answers["oirNumber"] ?: "",
        selectionBoard = answers["selectionBoard"] ?: "",
        batchNumber = answers["batchNumber"] ?: "",
        chestNumber = answers["chestNumber"] ?: "",
        upscRollNumber = answers["upscRollNumber"] ?: "",
        fullName = answers["fullName"] ?: "",
        dateOfBirth = answers["dateOfBirth"] ?: "",
        age = answers["age"] ?: "",
        gender = "",
        phone = "",
        email = "",
        state = answers["state"] ?: "",
        district = answers["district"] ?: "",
        religion = answers["religion"] ?: "",
        scStObcStatus = answers["scStObcStatus"] ?: "",
        motherTongue = answers["motherTongue"] ?: "",
        maritalStatus = answers["maritalStatus"] ?: "",
        permanentAddress = answers["permanentAddress"] ?: "",
        presentAddress = answers["presentAddress"] ?: "",
        maximumResidence = answers["maximumResidence"] ?: "",
        maximumResidencePopulation = answers["maximumResidencePopulation"] ?: "",
        presentResidencePopulation = answers["presentResidencePopulation"] ?: "",
        permanentResidencePopulation = answers["permanentResidencePopulation"] ?: "",
        isDistrictHQ = answers["isDistrictHQ"]?.toBoolean() ?: false,
        height = answers["height"] ?: "",
        weight = answers["weight"] ?: "",
        fatherName = answers["fatherName"] ?: "",
        fatherOccupation = answers["fatherOccupation"] ?: "",
        fatherEducation = answers["fatherEducation"] ?: "",
        fatherIncome = answers["fatherIncome"] ?: "",
        motherName = answers["motherName"] ?: "",
        motherOccupation = answers["motherOccupation"] ?: "",
        motherEducation = answers["motherEducation"] ?: "",
        parentsAlive = answers["parentsAlive"] ?: "",
        ageAtFatherDeath = answers["ageAtFatherDeath"] ?: "",
        ageAtMotherDeath = answers["ageAtMotherDeath"] ?: "",
        guardianName = answers["guardianName"] ?: "",
        guardianOccupation = answers["guardianOccupation"] ?: "",
        guardianEducation = answers["guardianEducation"] ?: "",
        guardianIncome = answers["guardianIncome"] ?: "",
        siblings = siblings,
        presentOccupation = answers["presentOccupation"] ?: "",
        personalMonthlyIncome = answers["personalMonthlyIncome"] ?: "",
        education10th = education10th,
        education12th = education12th,
        educationGraduation = educationGraduation,
        educationPostGraduation = educationPostGraduation,
        hobbies = answers["hobbies"] ?: "",
        sports = answers["sports"] ?: "",
        sportsParticipation = state.sportsParticipation,
        extraCurricularActivities = state.extraCurricularActivities,
        positionsOfResponsibility = answers["positionsOfResponsibility"] ?: "",
        workExperience = state.workExperience,
        nccTraining = nccTraining,
        natureOfCommission = answers["natureOfCommission"] ?: "",
        choiceOfService = answers["choiceOfService"] ?: "",
        chancesAvailed = answers["chancesAvailed"] ?: "",
        previousInterviews = state.previousInterviews,
        whyDefenseForces = answers["whyDefenseForces"] ?: "",
        strengths = "",
        weaknesses = "",
        status = status,
        submittedAt = state.lastModifiedAt,
        lastModifiedAt = state.lastModifiedAt
    )
}

/**
 * Synchronous mock AI quality estimate -- no Gemini call, matches the Android
 * original's `generateMockAIScore()` verbatim (including the fixed `totalFields = 18`
 * denominator and the completeness-derived pseudo-random category scores).
 */
internal fun buildMockPIQAIScore(submission: PIQSubmission): PIQAIScore {
    val totalFields = 18
    val filledFields = listOf(
        submission.fullName, submission.dateOfBirth,
        submission.fatherName, submission.motherName,
        submission.hobbies, submission.sports
    ).count { it.isNotBlank() }
    val completeness = (filledFields.toFloat() / totalFields * 100).toInt()

    val personalInfo = (completeness * 0.33f * 0.8f) + Random.nextFloat() * 5f
    val familyInfo = (completeness * 0.33f * 0.9f) + Random.nextFloat() * 3f
    val educationCareer = (completeness * 0.34f * 0.85f) + Random.nextFloat() * 4f
    val selfAssessment = if (completeness > 80) {
        17f + Random.nextFloat() * 8f
    } else {
        10f + Random.nextFloat() * 7f
    }
    val overall = personalInfo + familyInfo + educationCareer + selfAssessment

    return PIQAIScore(
        overallScore = overall.coerceIn(60f, 95f),
        personalInfoScore = personalInfo.coerceIn(15f, 25f),
        familyInfoScore = familyInfo.coerceIn(16f, 25f),
        motivationScore = educationCareer.coerceIn(12f, 25f),
        selfAssessmentScore = selfAssessment.coerceIn(10f, 25f),
        feedback = when {
            overall >= 85 -> "Excellent PIQ! Comprehensive information provided. Well-prepared for assessor questions."
            overall >= 75 -> "Good PIQ. Adequate information provided. Some areas could be more detailed."
            else -> "PIQ needs improvement. Add more details to all sections."
        },
        strengths = buildList {
            if (completeness > 80) add("Comprehensive information")
            if (submission.hobbies.isNotBlank()) add("Well-documented interests")
        },
        areasForImprovement = buildList {
            if (completeness < 70) add("Fill all sections completely")
            if (submission.hobbies.isBlank()) add("Add hobbies and interests")
        },
        completenessPercentage = completeness,
        clarityScore = 7.5f,
        consistencyScore = 8.0f + Random.nextFloat() * 2f
    )
}
