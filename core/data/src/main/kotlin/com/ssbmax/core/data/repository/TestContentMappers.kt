package com.ssbmax.core.data.repository

import com.ssbmax.core.domain.model.*

/**
 * Mappers for converting Firestore data to domain models
 * Extracted to keep TestContentRepositoryImpl under 300 lines
 */

@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.toOIRQuestion(): OIRQuestion? {
    return try {
        OIRQuestion(
            id = this["id"] as? String ?: return null,
            questionNumber = (this["questionNumber"] as? Number)?.toInt() ?: return null,
            type = OIRQuestionType.valueOf(this["type"] as? String ?: return null),
            questionText = this["questionText"] as? String ?: return null,
            options = (this["options"] as? List<Map<String, Any?>>)?.mapNotNull { it.toOIROption() } ?: return null,
            correctAnswerId = this["correctAnswerId"] as? String ?: return null,
            explanation = this["explanation"] as? String ?: "",
            difficulty = QuestionDifficulty.valueOf(this["difficulty"] as? String ?: "MEDIUM"),
            timeSeconds = (this["timeSeconds"] as? Number)?.toInt() ?: 60
        )
    } catch (e: Exception) {
        null
    }
}

internal fun Map<String, Any?>.toOIROption(): OIROption? {
    return try {
        OIROption(
            id = this["id"] as? String ?: return null,
            text = this["text"] as? String ?: return null,
            imageUrl = this["imageUrl"] as? String
        )
    } catch (e: Exception) {
        null
    }
}

@Suppress("UNCHECKED_CAST")
internal fun Map<String, Any?>.toPPDTQuestion(): PPDTQuestion? {
    return try {
        PPDTQuestion(
            id = this["id"] as? String ?: return null,
            imageUrl = this["imageUrl"] as? String ?: return null,
            imageDescription = this["imageDescription"] as? String ?: "",
            viewingTimeSeconds = (this["viewingTimeSeconds"] as? Number)?.toInt() ?: 30,
            writingTimeMinutes = (this["writingTimeMinutes"] as? Number)?.toInt() ?: 4,
            guidelines = (this["guidelines"] as? List<String>) ?: emptyList()
        )
    } catch (e: Exception) {
        null
    }
}

internal fun Map<String, Any?>.toTATQuestion(): TATQuestion? {
    return try {
        TATQuestion(
            id = this["id"] as? String ?: return null,
            imageUrl = this["imageUrl"] as? String ?: return null,
            sequenceNumber = (this["sequenceNumber"] as? Number)?.toInt() ?: return null,
            prompt = this["prompt"] as? String ?: "Write a story about what you see in the picture",
            viewingTimeSeconds = (this["viewingTimeSeconds"] as? Number)?.toInt() ?: 30,
            writingTimeMinutes = (this["writingTimeMinutes"] as? Number)?.toInt() ?: 4,
            minCharacters = (this["minCharacters"] as? Number)?.toInt() ?: 150,
            maxCharacters = (this["maxCharacters"] as? Number)?.toInt() ?: 800
        )
    } catch (e: Exception) {
        null
    }
}

internal fun Map<String, Any?>.toWATWord(): WATWord? {
    return try {
        WATWord(
            id = this["id"] as? String ?: return null,
            word = this["word"] as? String ?: return null,
            sequenceNumber = (this["sequenceNumber"] as? Number)?.toInt() ?: return null,
            timeAllowedSeconds = (this["timeAllowedSeconds"] as? Number)?.toInt() ?: 15
        )
    } catch (e: Exception) {
        null
    }
}

internal fun Map<String, Any?>.toSRTSituation(): SRTSituation? {
    return try {
        SRTSituation(
            id = this["id"] as? String ?: return null,
            situation = this["situation"] as? String ?: return null,
            sequenceNumber = (this["sequenceNumber"] as? Number)?.toInt() ?: return null,
            category = SRTCategory.valueOf(this["category"] as? String ?: "GENERAL"),
            timeAllowedSeconds = (this["timeAllowedSeconds"] as? Number)?.toInt() ?: 30
        )
    } catch (e: Exception) {
        null
    }
}

