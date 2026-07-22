package com.ssbmax.shared.presentation.study

import com.ssbmax.shared.domain.model.StudyProgress
import com.ssbmax.shared.domain.usecase.auth.ObserveCurrentUserUseCase
import com.ssbmax.shared.domain.usecase.study.GetStudyMaterialDetailUseCase
import com.ssbmax.shared.domain.usecase.study.GetStudyProgressUseCase
import com.ssbmax.shared.domain.usecase.study.SaveStudyProgressUseCase
import com.ssbmax.shared.domain.usecase.study.TrackStudySessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

/**
 * KMP port of the Android `app/.../ui/study/StudyMaterialDetailViewModel.kt`.
 * Handles material content loading and progress tracking.
 *
 * Deviations from the Android original, both following precedents already
 * established elsewhere in this phase:
 * - `SavedStateHandle`-injected `materialId` replaced with an explicit
 *   [loadMaterial] call the screen makes in a `LaunchedEffect(materialId)`,
 *   same "screen owns the ID, ViewModel is Koin-injected with no params"
 *   pattern as [com.ssbmax.shared.presentation.submissions.SubmissionDetailViewModel].
 * - The Android original's `Context`-dependent
 *   `StudyMaterialContentProvider.loadHTMLFromAssets` (Android asset-manager
 *   API) replaced with [StudyMaterialContentProvider.loadPIQFormHtml]
 *   (Compose Multiplatform resource reading, KMP-safe on both platforms).
 * - `System.currentTimeMillis()` (JVM-only) replaced with
 *   `Clock.System.now().toEpochMilliseconds()`.
 * - Plain-class + own-`CoroutineScope` ViewModel pattern (NOT
 *   `androidx.lifecycle.ViewModel`) -- `close()` (called from the screen's
 *   `DisposableEffect`) replaces `onCleared()` for ending the study session.
 */
class StudyMaterialDetailViewModel(
    private val getStudyMaterialDetail: GetStudyMaterialDetailUseCase,
    private val saveStudyProgress: SaveStudyProgressUseCase,
    private val trackStudySession: TrackStudySessionUseCase,
    private val getStudyProgress: GetStudyProgressUseCase,
    private val observeCurrentUser: ObserveCurrentUserUseCase
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _uiState = MutableStateFlow(StudyMaterialDetailUiState())
    val uiState: StateFlow<StudyMaterialDetailUiState> = _uiState.asStateFlow()

    private var materialId: String = ""

    fun close() {
        endStudySession()
        scope.cancel()
    }

    fun loadMaterial(materialId: String) {
        this.materialId = materialId
        loadMaterialContent()
        startStudySession()
    }

    private fun loadMaterialContent() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val cloudResult = getStudyMaterialDetail(materialId)

                val material = cloudResult.getOrNull()?.let { cloudMaterial ->
                    StudyMaterialContent(
                        id = cloudMaterial.id,
                        title = cloudMaterial.title,
                        category = cloudMaterial.category,
                        author = cloudMaterial.author.ifEmpty { "SSB Expert" },
                        publishedDate = "2025",
                        readTime = cloudMaterial.readTime.ifEmpty { "10 min read" },
                        content = cloudMaterial.contentMarkdown,
                        isPremium = cloudMaterial.isPremium,
                        tags = emptyList(),
                        relatedMaterials = emptyList()
                    )
                } ?: run {
                    if (materialId == "piq_form_reference") {
                        StudyMaterialContent(
                            id = "piq_form_reference",
                            title = "SSB PIQ Form (Reference)",
                            category = "PIQ Form",
                            author = "SSB",
                            publishedDate = "2025",
                            readTime = "5 min read",
                            content = StudyMaterialContentProvider.loadPIQFormHtml(),
                            isPremium = false,
                            tags = listOf("PIQ", "Form", "Reference"),
                            relatedMaterials = emptyList()
                        )
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, error = "Content not available. Please check your internet connection.")
                        }
                        return@launch
                    }
                }

                val currentUser = observeCurrentUser().first()
                val existingProgress = currentUser?.let {
                    getStudyProgress(it.id, materialId).getOrNull()
                }

                _uiState.update {
                    it.copy(
                        material = material,
                        readingProgress = existingProgress?.progress ?: 0f,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Unable to load content. Please check your internet connection and try again.")
                }
            }
        }
    }

    private fun startStudySession() {
        scope.launch {
            val currentUser = observeCurrentUser().first()
            if (currentUser != null) {
                val sessionResult = trackStudySession.startSession(currentUser.id, materialId)
                _uiState.update { it.copy(activeSessionId = sessionResult.getOrNull()?.id) }
            }
        }
    }

    fun updateProgress(progress: Float) {
        val coercedProgress = progress.coerceIn(0f, 100f)
        _uiState.update { it.copy(readingProgress = coercedProgress) }

        scope.launch {
            val currentUser = observeCurrentUser().first()
            if (currentUser != null) {
                val studyProgress = StudyProgress(
                    materialId = materialId,
                    userId = currentUser.id,
                    progress = coercedProgress,
                    lastReadAt = Clock.System.now().toEpochMilliseconds(),
                    timeSpent = 0L,
                    isCompleted = coercedProgress >= 100f
                )
                saveStudyProgress(studyProgress)
            }
        }
    }

    private fun endStudySession() {
        scope.launch {
            _uiState.value.activeSessionId?.let { id ->
                val progressIncrement = _uiState.value.readingProgress
                trackStudySession.endSession(id, progressIncrement)
            }
        }
    }
}

/**
 * UI State for Study Material Detail Screen
 */
data class StudyMaterialDetailUiState(
    val material: StudyMaterialContent? = null,
    val readingProgress: Float = 0f,
    val isLoading: Boolean = false,
    val error: String? = null,
    val activeSessionId: String? = null
)

/**
 * Study material content model
 */
data class StudyMaterialContent(
    val id: String,
    val title: String,
    val category: String,
    val author: String,
    val publishedDate: String,
    val readTime: String,
    val content: String,
    val isPremium: Boolean,
    val tags: List<String>,
    val relatedMaterials: List<RelatedMaterial>
)

/**
 * Related material item
 */
data class RelatedMaterial(
    val id: String,
    val title: String
)
