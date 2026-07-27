package com.ssbmax.ui.tests.oir.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.ssbmax.R
import com.ssbmax.core.domain.model.OIROption
import com.ssbmax.core.domain.model.OIRQuestion
import com.ssbmax.ui.tests.common.AnswerFeedbackEffect

@Composable
internal fun OIRQuestionView(
    question: OIRQuestion,
    selectedOptionIds: Set<String>,
    onOptionSelected: (String) -> Unit,
    showFeedback: Boolean,
    isCorrect: Boolean,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    AnswerFeedbackEffect(
        showFeedback = showFeedback,
        isCorrect = isCorrect,
        hapticFeedback = hapticFeedback
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "type-badge") {
            OIRTypeBadge(question.type.displayName)
        }

        item(key = "question-text") {
            OIRQuestionTextCard(question.questionText)
        }

        question.questionImageUrl?.let { imageUrl ->
            item(key = "figure") {
                OIRQuestionFigureCard(imageUrl)
            }
        }

        if (question.isMultiSelect) {
            item(key = "multi-select-hint") {
                OIRMultiSelectHint(selectedOptionIds.size)
            }
        }

        items(question.options, key = { it.id }) { option ->
            val isDimmed = question.isMultiSelect && !showFeedback &&
                selectedOptionIds.size >= 2 && option.id !in selectedOptionIds
            OIROptionCard(
                option = option,
                isSelected = option.id in selectedOptionIds,
                isCorrect = showFeedback && (if (question.isMultiSelect)
                    option.id in question.correctAnswerIds
                else
                    option.id == question.correctAnswerId),
                isWrong = showFeedback && option.id in selectedOptionIds &&
                    (if (question.isMultiSelect) option.id !in question.correctAnswerIds
                    else option.id != question.correctAnswerId),
                isMultiSelect = question.isMultiSelect,
                isDimmed = isDimmed,
                onClick = { if (!showFeedback) onOptionSelected(option.id) }
            )
        }

        if (showFeedback) {
            item(key = "feedback") {
                OIRFeedbackCard(isCorrect = isCorrect, explanation = question.explanation)
            }
        }
    }
}

@Composable
private fun OIRTypeBadge(displayName: String) {
    AssistChip(
        onClick = { },
        label = {
            Text(displayName, style = MaterialTheme.typography.labelMedium)
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    )
}

@Composable
private fun OIRQuestionTextCard(questionText: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Text(
            text = questionText,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(20.dp)
        )
    }
}

@Composable
private fun OIRQuestionFigureCard(imageUrl: String) {
    val context = LocalContext.current
    val stableRequest = remember(imageUrl) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(false)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        AsyncImage(
            model = stableRequest,
            contentDescription = stringResource(R.string.oir_question_figure_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .padding(12.dp)
        )
    }
}

@Composable
private fun OIRMultiSelectHint(selectionCount: Int) {
    val hintRes = if (selectionCount >= 2)
        R.string.oir_multi_select_selections_complete else R.string.oir_multi_select_hint
    SuggestionChip(
        onClick = { },
        label = { Text(stringResource(hintRes), style = MaterialTheme.typography.labelMedium) }
    )
}

@Composable
internal fun OIROptionCard(
    option: OIROption,
    isSelected: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    onClick: () -> Unit,
    isMultiSelect: Boolean = false,
    isDimmed: Boolean = false
) {
    val backgroundColor = when {
        isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
        isWrong -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isCorrect -> MaterialTheme.colorScheme.tertiary
        isWrong -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isDimmed) 0.4f else 1f)
            .border(2.dp, borderColor, MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            OIROptionContentInRow(option)
            OIROptionIndicator(isCorrect, isWrong, isSelected, isMultiSelect)
        }
    }
}

@Composable
private fun RowScope.OIROptionContentInRow(option: OIROption) {
    val context = LocalContext.current
    if (option.imageUrl != null) {
        val stableRequest = remember(option.imageUrl) {
            ImageRequest.Builder(context)
                .data(option.imageUrl)
                .crossfade(false)
                .memoryCachePolicy(CachePolicy.ENABLED)
                .diskCachePolicy(CachePolicy.ENABLED)
                .build()
        }
        AsyncImage(
            model = stableRequest,
            contentDescription = option.text,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 80.dp)
        )
    } else {
        Text(
            text = option.text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun OIROptionIndicator(
    isCorrect: Boolean,
    isWrong: Boolean,
    isSelected: Boolean,
    isMultiSelect: Boolean
) {
    when {
        isCorrect -> Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(24.dp)
        )
        isWrong -> Icon(
            imageVector = Icons.Default.Cancel,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(24.dp)
        )
        isSelected -> OIRSelectionIndicator(isMultiSelect)
    }
}

@Composable
private fun OIRSelectionIndicator(isMultiSelect: Boolean) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(if (isMultiSelect) RoundedCornerShape(4.dp) else CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(16.dp)
        )
    }
}


/**
 * Post-answer feedback card showing correct/incorrect verdict and explanation.
 */
@Composable
internal fun OIRFeedbackCard(isCorrect: Boolean, explanation: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCorrect) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.errorContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    contentDescription = null,
                    tint = if (isCorrect) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
                Text(
                    text = if (isCorrect) {
                        stringResource(R.string.oir_correct)
                    } else {
                        stringResource(R.string.oir_incorrect)
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(text = explanation, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
