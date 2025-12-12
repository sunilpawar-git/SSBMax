package com.ssbmax.ui.tests.gto.common

import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.ssbmax.core.domain.model.gto.GTOSubmission
import com.ssbmax.core.domain.model.gto.GTOTestType
import com.ssbmax.core.domain.repository.GTORepository
import com.ssbmax.utils.ErrorLogger
import com.ssbmax.workers.GTOAnalysisWorker
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Helper for submitting GTO tests
 */
class GTOTestSubmissionHelper(
    private val gtoRepository: GTORepository,
    private val workManager: WorkManager
) {
    
    suspend fun submitTest(
        submission: GTOSubmission,
        testType: GTOTestType,
        userId: String,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val submissionId = submission.id
            
            val submitResult = gtoRepository.submitTest(submission)
            if (submitResult.isFailure) {
                throw submitResult.exceptionOrNull() ?: Exception("Unknown submission error")
            }
            
            gtoRepository.recordTestUsage(
                userId = userId,
                testType = testType,
                submissionId = submissionId
            )
            
            gtoRepository.updateProgress(
                userId = userId,
                completedTestType = testType
            )
            
            val workRequest = OneTimeWorkRequestBuilder<GTOAnalysisWorker>()
                .setInputData(workDataOf(
                    GTOAnalysisWorker.KEY_SUBMISSION_ID to submissionId
                ))
                .build()
            
            workManager.enqueue(workRequest)
            
            onSuccess(submissionId)
            
        } catch (e: Exception) {
            ErrorLogger.log(e, "Failed to submit GTO test")
            onError("Failed to submit test. Please try again.")
        }
    }
}
