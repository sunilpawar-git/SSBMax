package com.ssbmax.lint

import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.checks.infrastructure.TestFile
import com.android.tools.lint.checks.infrastructure.TestLintTask.lint
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test

/**
 * Unit tests for NullableViewModelVarDetector
 * 
 * Tests verify that the lint rule correctly:
 * 1. Detects nullable Job? fields (memory leak risk)
 * 2. Detects nullable mutable vars (not configuration-change safe)
 * 3. Allows StateFlow/LiveData (proper reactive state)
 */
class NullableViewModelVarDetectorTest : LintDetectorTest() {
    
    override fun getDetector(): Detector = NullableViewModelVarDetector()
    
    override fun getIssues(): List<Issue> = listOf(
        NullableViewModelVarDetector.NULLABLE_STATE_ISSUE,
        NullableViewModelVarDetector.JOB_LEAK_ISSUE
    )
    
    @Test
    fun `detect nullable Job var in ViewModel`() {
        lint().files(
            kotlin("""
                package test
                import androidx.lifecycle.ViewModel
                import kotlinx.coroutines.Job
                
                class TestViewModel : ViewModel() {
                    private var timerJob: Job? = null
                }
            """).indented(),
            viewModelStub
        )
        .issues(NullableViewModelVarDetector.JOB_LEAK_ISSUE)
        .run()
        .expect("""
            src/test/TestViewModel.kt:6: Error: Storing Job? as ViewModel field causes memory leaks. Use viewModelScope.launch directly without storing Job reference. [ViewModelJobLeak]
                private var timerJob: Job? = null
                            ~~~~~~~~
            1 errors, 0 warnings
        """.trimIndent())
    }
    
    @Test
    fun `detect nullable String var in ViewModel`() {
        lint().files(
            kotlin("""
                package test
                import androidx.lifecycle.ViewModel
                
                class TestViewModel : ViewModel() {
                    private var sessionId: String? = null
                }
            """).indented(),
            viewModelStub
        )
        .issues(NullableViewModelVarDetector.NULLABLE_STATE_ISSUE)
        .run()
        .expect("""
            src/test/TestViewModel.kt:5: Error: Mutable nullable state sessionId should be in StateFlow. Move to UiState data class for lifecycle safety and configuration-change survival. [NullableViewModelState]
                private var sessionId: String? = null
                            ~~~~~~~~~
            1 errors, 0 warnings
        """.trimIndent())
    }
    
    @Test
    fun `detect nullable custom type var in ViewModel`() {
        lint().files(
            kotlin("""
                package test
                import androidx.lifecycle.ViewModel
                
                data class MySession(val id: String)
                
                class TestViewModel : ViewModel() {
                    private var currentSession: MySession? = null
                }
            """).indented(),
            viewModelStub
        )
        .issues(NullableViewModelVarDetector.NULLABLE_STATE_ISSUE)
        .run()
        .expect("""
            src/test/TestViewModel.kt:7: Error: Mutable nullable state currentSession should be in StateFlow. Move to UiState data class for lifecycle safety and configuration-change survival. [NullableViewModelState]
                    private var currentSession: MySession? = null
                                ~~~~~~~~~~~~~~
            1 errors, 0 warnings
        """.trimIndent())
    }
    
    @Test
    fun `allow StateFlow in ViewModel`() {
        lint().files(
            kotlin("""
                package test
                import androidx.lifecycle.ViewModel
                import kotlinx.coroutines.flow.MutableStateFlow
                import kotlinx.coroutines.flow.StateFlow
                
                class TestViewModel : ViewModel() {
                    private val _state = MutableStateFlow<String?>(null)
                    val state: StateFlow<String?> = _state
                }
            """).indented(),
            viewModelStub
        )
        .issues(NullableViewModelVarDetector.NULLABLE_STATE_ISSUE)
        .run()
        .expectClean()
    }
    
    @Test
    fun `allow LiveData in ViewModel`() {
        lint().files(
            kotlin("""
                package test
                import androidx.lifecycle.ViewModel
                import androidx.lifecycle.MutableLiveData
                
                class TestViewModel : ViewModel() {
                    private val _data = MutableLiveData<String?>()
                }
            """).indented(),
            viewModelStub,
            liveDataStub
        )
        .issues(NullableViewModelVarDetector.NULLABLE_STATE_ISSUE)
        .run()
        .expectClean()
    }
    
    @Test
    fun `allow non-nullable vars in ViewModel`() {
        lint().files(
            kotlin("""
                package test
                import androidx.lifecycle.ViewModel
                
                class TestViewModel : ViewModel() {
                    private var counter: Int = 0
                    private var name: String = ""
                }
            """).indented(),
            viewModelStub
        )
        .issues(NullableViewModelVarDetector.NULLABLE_STATE_ISSUE)
        .run()
        .expectClean()
    }
    
    @Test
    fun `allow val fields in ViewModel`() {
        lint().files(
            kotlin("""
                package test
                import androidx.lifecycle.ViewModel
                
                class TestViewModel : ViewModel() {
                    private val sessionId: String? = null
                    private val job: kotlinx.coroutines.Job? = null
                }
            """).indented(),
            viewModelStub
        )
        .issues(
            NullableViewModelVarDetector.NULLABLE_STATE_ISSUE,
            NullableViewModelVarDetector.JOB_LEAK_ISSUE
        )
        .run()
        .expectClean()
    }
    
    @Test
    fun `ignore non-ViewModel classes`() {
        lint().files(
            kotlin("""
                package test
                
                class NotAViewModel {
                    private var timerJob: kotlinx.coroutines.Job? = null
                    private var sessionId: String? = null
                }
            """).indented()
        )
        .issues(
            NullableViewModelVarDetector.NULLABLE_STATE_ISSUE,
            NullableViewModelVarDetector.JOB_LEAK_ISSUE
        )
        .run()
        .expectClean()
    }
    
    // Stubs for external dependencies
    private val viewModelStub: TestFile = kotlin("""
        package androidx.lifecycle
        abstract class ViewModel
    """).indented()
    
    private val liveDataStub: TestFile = kotlin("""
        package androidx.lifecycle
        open class LiveData<T>
        class MutableLiveData<T> : LiveData<T>()
    """).indented()
}

