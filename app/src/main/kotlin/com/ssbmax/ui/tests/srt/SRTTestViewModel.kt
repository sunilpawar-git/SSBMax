package com.ssbmax.ui.tests.srt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssbmax.core.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for SRT Test Screen
 */
@HiltViewModel
class SRTTestViewModel @Inject constructor(
    // TODO: Inject TestRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(SRTTestUiState())
    val uiState: StateFlow<SRTTestUiState> = _uiState.asStateFlow()
    
    fun loadTest(testId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                // TODO: Load from repository
                val situations = generateMockSituations()
                val config = SRTTestConfig()
                
                _uiState.update { it.copy(
                    isLoading = false,
                    testId = testId,
                    situations = situations,
                    config = config,
                    phase = SRTPhase.INSTRUCTIONS
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }
    
    fun startTest() {
        _uiState.update { it.copy(
            phase = SRTPhase.IN_PROGRESS,
            currentSituationIndex = 0,
            startTime = System.currentTimeMillis()
        ) }
    }
    
    fun updateResponse(response: String) {
        val maxLength = _uiState.value.config?.maxResponseLength ?: 200
        if (response.length <= maxLength) {
            _uiState.update { it.copy(currentResponse = response) }
        }
    }
    
    fun moveToNext() {
        val state = _uiState.value
        val currentSituation = state.currentSituation ?: return
        
        // Save current response
        val response = SRTSituationResponse(
            situationId = currentSituation.id,
            situation = currentSituation.situation,
            response = state.currentResponse,
            charactersCount = state.currentResponse.length,
            timeTakenSeconds = 30, // TODO: Track actual time
            submittedAt = System.currentTimeMillis(),
            isSkipped = false
        )
        
        val updatedResponses = state.responses.toMutableList().apply {
            removeAll { it.situationId == response.situationId }
            add(response)
        }
        
        _uiState.update { it.copy(responses = updatedResponses) }
        
        // Move to next or review
        if (state.currentSituationIndex < state.situations.size - 1) {
            _uiState.update { it.copy(
                currentSituationIndex = state.currentSituationIndex + 1,
                currentResponse = ""
            ) }
        } else {
            // All situations shown, go to review
            _uiState.update { it.copy(phase = SRTPhase.REVIEW) }
        }
    }
    
    fun skipSituation() {
        val state = _uiState.value
        val currentSituation = state.currentSituation ?: return
        
        // Save skipped response
        val response = SRTSituationResponse(
            situationId = currentSituation.id,
            situation = currentSituation.situation,
            response = "",
            charactersCount = 0,
            timeTakenSeconds = 0,
            submittedAt = System.currentTimeMillis(),
            isSkipped = true
        )
        
        val updatedResponses = state.responses.toMutableList().apply {
            removeAll { it.situationId == response.situationId }
            add(response)
        }
        
        _uiState.update { it.copy(
            responses = updatedResponses,
            currentResponse = ""
        ) }
        
        // Move to next
        if (state.currentSituationIndex < state.situations.size - 1) {
            _uiState.update { it.copy(
                currentSituationIndex = state.currentSituationIndex + 1
            ) }
        } else {
            // All situations shown, go to review
            _uiState.update { it.copy(phase = SRTPhase.REVIEW) }
        }
    }
    
    fun editResponse(index: Int) {
        val state = _uiState.value
        if (index in state.situations.indices) {
            val responseToEdit = state.responses.getOrNull(index)
            _uiState.update { it.copy(
                currentSituationIndex = index,
                currentResponse = responseToEdit?.response ?: "",
                phase = SRTPhase.IN_PROGRESS
            ) }
        }
    }
    
    fun submitTest() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            
            try {
                val state = _uiState.value
                
                // Create submission
                val totalTimeMinutes = ((System.currentTimeMillis() - state.startTime) / 60000).toInt()
                val submission = SRTSubmission(
                    userId = "current_user", // TODO: Get from auth
                    testId = state.testId,
                    responses = state.responses,
                    totalTimeTakenMinutes = totalTimeMinutes,
                    submittedAt = System.currentTimeMillis(),
                    aiPreliminaryScore = generateMockAIScore(state.responses)
                )
                
                // TODO: Save to repository
                val submissionId = submission.id
                
                _uiState.update { it.copy(
                    isLoading = false,
                    isSubmitted = true,
                    submissionId = submissionId,
                    phase = SRTPhase.SUBMITTED
                ) }
            } catch (e: Exception) {
                _uiState.update { it.copy(
                    isLoading = false,
                    error = e.message
                ) }
            }
        }
    }
    
    private fun generateMockSituations(): List<SRTSituation> {
        val situations = listOf(
            "You are the captain of your college team. During an important match, you notice that your best player is not feeling well but insists on playing." to SRTCategory.LEADERSHIP,
            "You witness a senior colleague taking credit for your junior's work in a meeting." to SRTCategory.ETHICAL_DILEMMA,
            "While traveling alone at night, you see an elderly person who has fallen and needs help." to SRTCategory.RESPONSIBILITY,
            "Your team is losing a crucial match, and team morale is very low." to SRTCategory.TEAMWORK,
            "You discover that your close friend has been cheating in exams." to SRTCategory.ETHICAL_DILEMMA,
            "During a group trek, one member gets injured and cannot walk." to SRTCategory.CRISIS_MANAGEMENT,
            "You have to choose between attending an important family function or a crucial team practice." to SRTCategory.DECISION_MAKING,
            "A stranger asks you to lend them money for emergency medical treatment." to SRTCategory.INTERPERSONAL,
            "You see a group of people harassing someone on the street." to SRTCategory.COURAGE,
            "Your subordinate makes a serious mistake that could impact the entire project." to SRTCategory.LEADERSHIP
        )
        
        // Repeat and shuffle to get 60 situations
        return (situations + situations + situations + situations + situations + situations)
            .take(60)
            .mapIndexed { index, (situation, category) ->
                SRTSituation(
                    id = "srt_s_${index + 1}",
                    situation = "$situation What would you do?",
                    sequenceNumber = index + 1,
                    category = category,
                    timeAllowedSeconds = 30
                )
            }
    }
    
    private fun generateMockAIScore(responses: List<SRTSituationResponse>): SRTAIScore {
        val validResponses = responses.filter { it.isValidResponse }
        
        return SRTAIScore(
            overallScore = 76f,
            leadershipScore = 16f,
            decisionMakingScore = 15f,
            practicalityScore = 15f,
            initiativeScore = 15f,
            socialResponsibilityScore = 15f,
            feedback = "Shows good practical judgment and leadership qualities. Responses indicate initiative and social responsibility.",
            categoryWiseScores = mapOf(
                SRTCategory.LEADERSHIP to 78f,
                SRTCategory.DECISION_MAKING to 74f,
                SRTCategory.CRISIS_MANAGEMENT to 76f,
                SRTCategory.INTERPERSONAL to 75f,
                SRTCategory.ETHICAL_DILEMMA to 80f
            ),
            positiveTraits = listOf(
                "Proactive approach",
                "Considers team welfare",
                "Ethical decision making",
                "Takes responsibility"
            ),
            concerningPatterns = emptyList(),
            responseQuality = ResponseQuality.GOOD,
            strengths = listOf(
                "Leadership qualities",
                "Practical solutions",
                "Ethical awareness"
            ),
            areasForImprovement = listOf(
                "Consider more options before deciding",
                "Show more initiative in difficult situations"
            )
        )
    }
}

/**
 * UI State for SRT Test
 */
data class SRTTestUiState(
    val isLoading: Boolean = true,
    val testId: String = "",
    val situations: List<SRTSituation> = emptyList(),
    val config: SRTTestConfig? = null,
    val currentSituationIndex: Int = 0,
    val responses: List<SRTSituationResponse> = emptyList(),
    val currentResponse: String = "",
    val phase: SRTPhase = SRTPhase.INSTRUCTIONS,
    val startTime: Long = System.currentTimeMillis(),
    val isSubmitted: Boolean = false,
    val submissionId: String? = null,
    val error: String? = null
) {
    val currentSituation: SRTSituation?
        get() = situations.getOrNull(currentSituationIndex)
    
    val completedSituations: Int
        get() = responses.size
    
    val validResponseCount: Int
        get() = responses.count { it.isValidResponse }
    
    val progress: Float
        get() = if (situations.isEmpty()) 0f else (completedSituations.toFloat() / situations.size)
    
    val canMoveToNext: Boolean
        get() {
            val minLength = config?.minResponseLength ?: 20
            val maxLength = config?.maxResponseLength ?: 200
            return currentResponse.length in minLength..maxLength
        }
}

