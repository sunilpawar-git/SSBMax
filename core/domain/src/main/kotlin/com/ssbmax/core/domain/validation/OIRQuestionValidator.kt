package com.ssbmax.core.domain.validation

import com.ssbmax.core.domain.model.OIROption
import com.ssbmax.core.domain.model.OIRQuestion
import com.ssbmax.core.domain.model.OIRQuestionType
import com.ssbmax.core.domain.model.QuestionDifficulty

/**
 * Comprehensive OIR Question Validator
 * 
 * Catches all data corruption issues that we've encountered:
 * 1. Missing or empty questionText
 * 2. Malformed option IDs (single letter vs "opt_X" format)
 * 3. Malformed correctAnswerId (e.g., "opt_103_b" instead of "opt_b")
 * 4. Missing options
 * 5. Incorrect number of options
 * 6. Duplicate option IDs
 * 7. CorrectAnswerId not matching any option
 * 8. Empty option text
 * 9. Invalid question IDs
 * 
 * This validator should be run BEFORE questions are used in tests.
 */
object OIRQuestionValidator {
    
    /**
     * Validates a single OIR question
     * Returns ValidationResult with detailed error messages
     */
    fun validate(question: OIRQuestion): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        // 1. Validate Question ID
        if (question.id.isBlank()) {
            errors.add("Question ID is blank")
        } else if (!isValidQuestionId(question.id)) {
            warnings.add("Question ID format unusual: ${question.id} (expected: oir_XXX or oir_q_XXXX)")
        }
        
        // 2. Validate Question Number
        if (question.questionNumber <= 0) {
            errors.add("Invalid question number: ${question.questionNumber} (must be > 0)")
        }
        
        // 3. Validate Question Text
        if (question.questionText.isBlank()) {
            errors.add("Question text is empty or blank")
        } else if (question.questionText.length < 10) {
            warnings.add("Question text suspiciously short (${question.questionText.length} chars): '${question.questionText}'")
        }
        
        // Check for field name typos (common issue)
        if (question.questionText.contains("\"question\":", ignoreCase = true)) {
            errors.add("Question text contains raw JSON - possible field name error")
        }
        
        // 4. Validate Options List
        if (question.options.isEmpty()) {
            errors.add("No options provided")
        } else {
            // Check option count (should be 4 for MCQ)
            if (question.options.size != 4) {
                warnings.add("Unusual number of options: ${question.options.size} (expected 4)")
            }
            
            // Check for duplicate option IDs
            val optionIds = question.options.map { it.id }
            val duplicates = optionIds.groupingBy { it }.eachCount().filter { it.value > 1 }
            if (duplicates.isNotEmpty()) {
                errors.add("Duplicate option IDs found: ${duplicates.keys}")
            }
            
            // Validate each option
            question.options.forEachIndexed { index, option ->
                validateOption(option, index, errors, warnings)
            }
        }
        
        // 5. Validate CorrectAnswerId Format
        if (question.correctAnswerId.isBlank()) {
            errors.add("CorrectAnswerId is empty")
        } else {
            // Check for common malformed patterns
            when {
                // Single letter format (e.g., "a", "b", "c", "d")
                question.correctAnswerId.length == 1 && question.correctAnswerId.matches(Regex("[a-dA-D]")) -> {
                    errors.add("CorrectAnswerId is single letter '${question.correctAnswerId}' (should be 'opt_${question.correctAnswerId.lowercase()}')")
                }
                
                // Malformed format with question number (e.g., "opt_103_b")
                question.correctAnswerId.matches(Regex("opt_\\d+_[a-d]")) -> {
                    val correctFormat = question.correctAnswerId.substringAfterLast("_")
                    errors.add("CorrectAnswerId has question number embedded: '${question.correctAnswerId}' (should be 'opt_$correctFormat')")
                }
                
                // Check if it follows standard format
                !question.correctAnswerId.matches(Regex("opt_[a-d]")) -> {
                    warnings.add("CorrectAnswerId format unusual: '${question.correctAnswerId}' (expected format: opt_a, opt_b, opt_c, or opt_d)")
                }
            }
            
            // 6. Validate CorrectAnswerId Matches an Option
            val correctOptionExists = question.options.any { it.id == question.correctAnswerId }
            if (!correctOptionExists) {
                errors.add("CorrectAnswerId '${question.correctAnswerId}' does not match any option ID. Available: ${question.options.map { it.id }}")
            }
        }
        
        // 7. Validate Explanation
        if (question.explanation.isBlank()) {
            warnings.add("Explanation is empty (good practice to provide one)")
        }
        
        // 8. Validate Time Allocation
        if (question.timeSeconds <= 0) {
            errors.add("Time allocation invalid: ${question.timeSeconds}s (must be > 0)")
        } else if (question.timeSeconds > 300) {
            warnings.add("Time allocation unusually high: ${question.timeSeconds}s (>5 minutes)")
        }
        
        return ValidationResult(
            isValid = errors.isEmpty(),
            questionId = question.id,
            errors = errors,
            warnings = warnings
        )
    }
    
    /**
     * Validates a single option
     */
    private fun validateOption(
        option: OIROption,
        index: Int,
        errors: MutableList<String>,
        warnings: MutableList<String>
    ) {
        // Check option ID format
        when {
            option.id.isBlank() -> {
                errors.add("Option #${index + 1} has blank ID")
            }
            
            // Single letter ID (corrupted data)
            option.id.length == 1 && option.id.matches(Regex("[a-dA-D]")) -> {
                errors.add("Option #${index + 1} has single-letter ID '${option.id}' (should be 'opt_${option.id.lowercase()}')")
            }
            
            // Standard format check
            !option.id.matches(Regex("opt_[a-d]")) && !option.id.matches(Regex("opt_\\d+[a-d]")) -> {
                warnings.add("Option #${index + 1} ID format unusual: '${option.id}' (expected: opt_a, opt_b, opt_c, or opt_d)")
            }
        }
        
        // Check option text
        if (option.text.isBlank()) {
            errors.add("Option '${option.id}' has empty text")
        } else if (option.text.length < 1) {
            warnings.add("Option '${option.id}' has very short text: '${option.text}'")
        }
    }
    
    /**
     * Validates a batch of questions
     * Returns BatchValidationResult with overall statistics
     */
    fun validateBatch(questions: List<OIRQuestion>, batchName: String = "unknown"): BatchValidationResult {
        val results = questions.map { validate(it) }
        
        val validCount = results.count { it.isValid }
        val invalidCount = results.count { !it.isValid }
        val totalErrors = results.sumOf { it.errors.size }
        val totalWarnings = results.sumOf { it.warnings.size }
        
        return BatchValidationResult(
            batchName = batchName,
            totalQuestions = questions.size,
            validQuestions = validCount,
            invalidQuestions = invalidCount,
            totalErrors = totalErrors,
            totalWarnings = totalWarnings,
            results = results,
            errorsByQuestion = results.filter { !it.isValid }
        )
    }
    
    /**
     * Checks if question ID follows expected patterns
     */
    private fun isValidQuestionId(id: String): Boolean {
        return id.matches(Regex("oir_\\d+")) || // Legacy: oir_103
               id.matches(Regex("oir_q_\\d{4}")) // Standard: oir_q_0103
    }
    
    /**
     * Quick validation - throws exception if question is invalid
     * Use this in critical paths where you want to fail fast
     */
    fun validateOrThrow(question: OIRQuestion) {
        val result = validate(question)
        if (!result.isValid) {
            throw InvalidQuestionException(
                questionId = question.id,
                errors = result.errors,
                message = "Question ${question.id} validation failed: ${result.errors.joinToString("; ")}"
            )
        }
    }
    
    /**
     * Validates and filters out invalid questions
     * Returns only valid questions + logs issues
     */
    fun validateAndFilter(
        questions: List<OIRQuestion>,
        onInvalidQuestion: (ValidationResult) -> Unit = {}
    ): List<OIRQuestion> {
        return questions.filter { question ->
            val result = validate(question)
            if (!result.isValid) {
                onInvalidQuestion(result)
                false
            } else {
                true
            }
        }
    }
}

/**
 * Result of validating a single question
 */
data class ValidationResult(
    val isValid: Boolean,
    val questionId: String,
    val errors: List<String>,
    val warnings: List<String>
) {
    fun toLogString(): String {
        val sb = StringBuilder()
        sb.appendLine("Question: $questionId")
        sb.appendLine("  Status: ${if (isValid) "✅ VALID" else "❌ INVALID"}")
        
        if (errors.isNotEmpty()) {
            sb.appendLine("  Errors (${errors.size}):")
            errors.forEach { sb.appendLine("    - $it") }
        }
        
        if (warnings.isNotEmpty()) {
            sb.appendLine("  Warnings (${warnings.size}):")
            warnings.forEach { sb.appendLine("    - $it") }
        }
        
        return sb.toString()
    }
}

/**
 * Result of validating a batch of questions
 */
data class BatchValidationResult(
    val batchName: String,
    val totalQuestions: Int,
    val validQuestions: Int,
    val invalidQuestions: Int,
    val totalErrors: Int,
    val totalWarnings: Int,
    val results: List<ValidationResult>,
    val errorsByQuestion: List<ValidationResult>
) {
    val isFullyValid: Boolean
        get() = invalidQuestions == 0
    
    val hasWarnings: Boolean
        get() = totalWarnings > 0
    
    fun toSummaryString(): String {
        return buildString {
            appendLine("=".repeat(60))
            appendLine("Batch Validation Report: $batchName")
            appendLine("=".repeat(60))
            appendLine("Total Questions: $totalQuestions")
            appendLine("✅ Valid: $validQuestions (${(validQuestions * 100.0 / totalQuestions).toInt()}%)")
            appendLine("❌ Invalid: $invalidQuestions")
            appendLine("⚠️  Total Errors: $totalErrors")
            appendLine("⚠️  Total Warnings: $totalWarnings")
            
            if (errorsByQuestion.isNotEmpty()) {
                appendLine()
                appendLine("Invalid Questions:")
                errorsByQuestion.forEach { result ->
                    appendLine()
                    append(result.toLogString())
                }
            }
            
            appendLine("=".repeat(60))
        }
    }
}

/**
 * Exception thrown when a question fails validation
 */
class InvalidQuestionException(
    val questionId: String,
    val errors: List<String>,
    message: String
) : Exception(message)


