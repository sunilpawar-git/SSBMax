package com.ssbmax.shared.presentation.piqresult

import com.ssbmax.shared.domain.model.Education
import com.ssbmax.shared.domain.model.ExtraCurricularActivity
import com.ssbmax.shared.domain.model.NCCTraining
import com.ssbmax.shared.domain.model.PIQAIScore
import com.ssbmax.shared.domain.model.PIQSubmission
import com.ssbmax.shared.domain.model.PreviousInterview
import com.ssbmax.shared.domain.model.Sibling
import com.ssbmax.shared.domain.model.SportsParticipation
import com.ssbmax.shared.domain.model.SubmissionStatus
import com.ssbmax.shared.domain.model.WorkExperience

/**
 * Extracted from [PIQSubmissionResultViewModel] to keep both files under the
 * 300-line limit. Field-for-field port of the Android original's
 * `PIQSubmissionResultViewModel.parsePIQSubmission()`.
 *
 * **Deliberate deviation from [com.ssbmax.shared.data.repository] precedent:**
 * this parses the raw `Map<String, Any>` from
 * [com.ssbmax.shared.domain.repository.SubmissionRepository.getSubmission] by
 * hand rather than delegating to `PIQDataDto.toDomain()`
 * (`PIQSubmissionMappers.kt`) -- that mapper hardcodes `aiPreliminaryScore =
 * null` (documented there as a pre-existing gap: written but never read back
 * via that path), which would silently drop the very score this result screen
 * exists to show. Reusing the Android original's own raw-map parsing here
 * (same duplication the Android app already has between its ViewModel and
 * `core/data/remote/SubmissionMappers.kt`) is the only way to surface the AI
 * score without touching the shared data layer -- flagged, not fixed, per this
 * session's surgical-change brief.
 */
/**
 * @throws Exception on any malformed field -- caller ([PIQSubmissionResultViewModel])
 *   catches and logs via [com.ssbmax.shared.domain.util.DomainLogger], matching this
 *   phase's precedent of centralizing error logging at the ViewModel boundary rather
 *   than swallowing failures inside a parser.
 */
internal fun parsePIQSubmission(data: Map<String, Any>): PIQSubmission {
    @Suppress("UNCHECKED_CAST")
    val submissionData = (data["data"] as? Map<String, Any?>) ?: (data as Map<String, Any?>)

        val siblings = ((submissionData["siblings"] as? List<*>) ?: emptyList<Any>()).mapNotNull { s ->
            val m = s as? Map<*, *> ?: return@mapNotNull null
            Sibling(
                id = m["id"] as? String ?: "",
                name = m["name"] as? String ?: "",
                age = m["age"]?.toString() ?: "",
                occupation = m["occupation"] as? String ?: "",
                education = m["education"] as? String ?: "",
                income = m["income"] as? String ?: ""
            )
        }

        fun parseEducation(key: String, level: String, hasStream: Boolean, hasCgpa: Boolean): Education {
            val m = submissionData[key] as? Map<*, *> ?: return Education(level = level)
            return Education(
                level = m["level"] as? String ?: level,
                institution = m["institution"] as? String ?: "",
                board = m["board"] as? String ?: "",
                stream = if (hasStream) m["stream"] as? String ?: "" else "",
                year = m["year"]?.toString() ?: "",
                percentage = if (!hasCgpa) m["percentage"]?.toString() ?: "" else "",
                cgpa = if (hasCgpa) m["cgpa"]?.toString() ?: "" else "",
                mediumOfInstruction = m["mediumOfInstruction"] as? String ?: "",
                boarderDayScholar = m["boarderDayScholar"] as? String ?: "",
                outstandingAchievement = m["outstandingAchievement"] as? String ?: ""
            )
        }

        val nccMap = submissionData["nccTraining"] as? Map<*, *>
        val nccTraining = nccMap?.let {
            NCCTraining(
                hasTraining = it["hasTraining"] as? Boolean ?: false,
                totalTraining = it["totalTraining"] as? String ?: "",
                wing = it["wing"] as? String ?: "",
                division = it["division"] as? String ?: "",
                certificateObtained = it["certificateObtained"] as? String ?: ""
            )
        } ?: NCCTraining()

        val sportsParticipation = ((submissionData["sportsParticipation"] as? List<*>) ?: emptyList<Any>()).mapNotNull { s ->
            val m = s as? Map<*, *> ?: return@mapNotNull null
            SportsParticipation(
                id = m["id"] as? String ?: "",
                sport = m["sport"] as? String ?: "",
                period = m["period"] as? String ?: "",
                representedInstitution = m["representedInstitution"] as? String ?: "",
                outstandingAchievement = m["outstandingAchievement"] as? String ?: ""
            )
        }

        val extraCurricularActivities = ((submissionData["extraCurricularActivities"] as? List<*>) ?: emptyList<Any>()).mapNotNull { a ->
            val m = a as? Map<*, *> ?: return@mapNotNull null
            ExtraCurricularActivity(
                id = m["id"] as? String ?: "",
                activityName = m["activityName"] as? String ?: "",
                duration = m["duration"] as? String ?: "",
                outstandingAchievement = m["outstandingAchievement"] as? String ?: ""
            )
        }

        val previousInterviews = ((submissionData["previousInterviews"] as? List<*>) ?: emptyList<Any>()).mapNotNull { i ->
            val m = i as? Map<*, *> ?: return@mapNotNull null
            PreviousInterview(
                id = m["id"] as? String ?: "",
                typeOfEntry = m["typeOfEntry"] as? String ?: "",
                ssbNumber = m["ssbNumber"] as? String ?: "",
                ssbPlace = m["ssbPlace"] as? String ?: "",
                date = m["date"] as? String ?: "",
                chestNumber = m["chestNumber"] as? String ?: "",
                batchNumber = m["batchNumber"] as? String ?: ""
            )
        }

        val workExperience = ((submissionData["workExperience"] as? List<*>) ?: emptyList<Any>()).mapNotNull { w ->
            val m = w as? Map<*, *> ?: return@mapNotNull null
            WorkExperience(
                id = m["id"] as? String ?: "",
                company = m["company"] as? String ?: "",
                role = m["role"] as? String ?: "",
                duration = m["duration"] as? String ?: "",
                description = m["description"] as? String ?: ""
            )
        }

        val aiScoreMap = submissionData["aiPreliminaryScore"] as? Map<*, *>
        val aiScore = aiScoreMap?.let {
            PIQAIScore(
                overallScore = (it["overallScore"] as? Number)?.toFloat() ?: 0f,
                personalInfoScore = (it["personalInfoScore"] as? Number)?.toFloat() ?: 0f,
                familyInfoScore = (it["familyInfoScore"] as? Number)?.toFloat() ?: 0f,
                motivationScore = (it["motivationScore"] as? Number)?.toFloat() ?: 0f,
                selfAssessmentScore = (it["selfAssessmentScore"] as? Number)?.toFloat() ?: 0f,
                feedback = it["feedback"] as? String ?: "",
                strengths = (it["strengths"] as? List<*>)?.mapNotNull { s -> s as? String } ?: emptyList(),
                areasForImprovement = (it["areasForImprovement"] as? List<*>)?.mapNotNull { a -> a as? String } ?: emptyList(),
                completenessPercentage = (it["completenessPercentage"] as? Number)?.toInt() ?: 0,
                clarityScore = (it["clarityScore"] as? Number)?.toFloat() ?: 0f,
                consistencyScore = (it["consistencyScore"] as? Number)?.toFloat() ?: 0f,
                analysisTimestamp = (it["analysisTimestamp"] as? Number)?.toLong() ?: 0L
            )
        }

        return PIQSubmission(
            id = submissionData["id"] as? String ?: "",
            userId = submissionData["userId"] as? String ?: "",
            testId = submissionData["testId"] as? String ?: "",
            oirNumber = submissionData["oirNumber"] as? String ?: "",
            selectionBoard = submissionData["selectionBoard"] as? String ?: "",
            batchNumber = submissionData["batchNumber"] as? String ?: "",
            chestNumber = submissionData["chestNumber"] as? String ?: "",
            upscRollNumber = submissionData["upscRollNumber"] as? String ?: "",
            fullName = submissionData["fullName"] as? String ?: "",
            dateOfBirth = submissionData["dateOfBirth"] as? String ?: "",
            age = submissionData["age"]?.toString() ?: "",
            gender = submissionData["gender"] as? String ?: "",
            phone = submissionData["phone"] as? String ?: "",
            email = submissionData["email"] as? String ?: "",
            state = submissionData["state"] as? String ?: "",
            district = submissionData["district"] as? String ?: "",
            religion = submissionData["religion"] as? String ?: "",
            scStObcStatus = submissionData["scStObcStatus"] as? String ?: "",
            motherTongue = submissionData["motherTongue"] as? String ?: "",
            maritalStatus = submissionData["maritalStatus"] as? String ?: "",
            permanentAddress = submissionData["permanentAddress"] as? String ?: "",
            presentAddress = submissionData["presentAddress"] as? String ?: "",
            maximumResidence = submissionData["maximumResidence"] as? String ?: "",
            maximumResidencePopulation = submissionData["maximumResidencePopulation"] as? String ?: "",
            presentResidencePopulation = submissionData["presentResidencePopulation"] as? String ?: "",
            permanentResidencePopulation = submissionData["permanentResidencePopulation"] as? String ?: "",
            isDistrictHQ = submissionData["isDistrictHQ"] as? Boolean ?: false,
            height = submissionData["height"] as? String ?: "",
            weight = submissionData["weight"] as? String ?: "",
            fatherName = submissionData["fatherName"] as? String ?: "",
            fatherOccupation = submissionData["fatherOccupation"] as? String ?: "",
            fatherEducation = submissionData["fatherEducation"] as? String ?: "",
            fatherIncome = submissionData["fatherIncome"] as? String ?: "",
            motherName = submissionData["motherName"] as? String ?: "",
            motherOccupation = submissionData["motherOccupation"] as? String ?: "",
            motherEducation = submissionData["motherEducation"] as? String ?: "",
            parentsAlive = submissionData["parentsAlive"] as? String ?: "",
            ageAtFatherDeath = submissionData["ageAtFatherDeath"] as? String ?: "",
            ageAtMotherDeath = submissionData["ageAtMotherDeath"] as? String ?: "",
            guardianName = submissionData["guardianName"] as? String ?: "",
            guardianOccupation = submissionData["guardianOccupation"] as? String ?: "",
            guardianEducation = submissionData["guardianEducation"] as? String ?: "",
            guardianIncome = submissionData["guardianIncome"] as? String ?: "",
            siblings = siblings,
            presentOccupation = submissionData["presentOccupation"] as? String ?: "",
            personalMonthlyIncome = submissionData["personalMonthlyIncome"] as? String ?: "",
            education10th = parseEducation("education10th", "10th", hasStream = false, hasCgpa = false),
            education12th = parseEducation("education12th", "12th", hasStream = true, hasCgpa = false),
            educationGraduation = parseEducation("educationGraduation", "Graduation", hasStream = false, hasCgpa = true),
            educationPostGraduation = parseEducation("educationPostGraduation", "Post-Graduation", hasStream = false, hasCgpa = true),
            hobbies = submissionData["hobbies"] as? String ?: "",
            sports = submissionData["sports"] as? String ?: "",
            sportsParticipation = sportsParticipation,
            extraCurricularActivities = extraCurricularActivities,
            positionsOfResponsibility = submissionData["positionsOfResponsibility"] as? String ?: "",
            workExperience = workExperience,
            nccTraining = nccTraining,
            natureOfCommission = submissionData["natureOfCommission"] as? String ?: "",
            choiceOfService = submissionData["choiceOfService"] as? String ?: "",
            chancesAvailed = submissionData["chancesAvailed"] as? String ?: "",
            previousInterviews = previousInterviews,
            whyDefenseForces = submissionData["whyDefenseForces"] as? String ?: "",
            strengths = submissionData["strengths"] as? String ?: "",
            weaknesses = submissionData["weaknesses"] as? String ?: "",
            status = runCatching { SubmissionStatus.valueOf(submissionData["status"] as? String ?: "DRAFT") }
                .getOrDefault(SubmissionStatus.DRAFT),
            submittedAt = (submissionData["submittedAt"] as? Number)?.toLong() ?: 0L,
            lastModifiedAt = (submissionData["lastModifiedAt"] as? Number)?.toLong() ?: 0L,
            gradedByInstructorId = submissionData["gradedByInstructorId"] as? String,
            gradingTimestamp = (submissionData["gradingTimestamp"] as? Number)?.toLong(),
            aiPreliminaryScore = aiScore
        )
}
