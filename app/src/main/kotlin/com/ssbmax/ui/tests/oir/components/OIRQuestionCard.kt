package com.ssbmax.ui.tests.oir.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

/**
 * Full question view: type badge, question text, optional figure, answer options, feedback.
 */
@Composable
internal fun OIRQuestionView(
    question: OIRQuestion,
    selectedOptionId: String?,
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

    val context = LocalContext.current

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "type-badge") {
            AssistChip(
                onClick = { },
                label = {
                    Text(question.type.displayName, style = MaterialTheme.typography.labelMedium)
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }

        item(key = "question-text") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text(
                    text = question.questionText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(20.dp)
                )
            }
        }

        // Figure for non-verbal / spatial questions
        question.questionImageUrl?.let { imageUrl ->
            item(key = "figure") {
                // remember(imageUrl) ensures the ImageRequest object is stable across timer
                // recompositions — prevents Coil from restarting the load every second.
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
                            // Reserve minimum height so options don't shift when the image loads.
                            .heightIn(min = 200.dp)
                            .padding(12.dp)
                    )
                }
            }
        }

        items(question.options, key = { it.id }) { option ->
            OIROptionCard(
                option = option,
                isSelected = option.id == selectedOptionId,
                isCorrect = showFeedback && option.id == question.correctAnswerId,
                isWrong = showFeedback && option.id == selectedOptionId && option.id != question.correctAnswerId,
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

/**
 * Single answer option card with selection / correct / wrong visual states.
 */
@Composable
internal fun OIROptionCard(
    option: OIROption,
    isSelected: Boolean,
    isCorrect: Boolean,
    isWrong: Boolean,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val backgroundColor = when {
        isCorrect -> MaterialTheme.colorScheme.tertiaryContainer
        isWrong   -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.primaryContainer
        else      -> MaterialTheme.colorScheme.surface
    }
    val borderColor = when {
        isCorrect -> MaterialTheme.colorScheme.tertiary
        isWrong   -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else      -> MaterialTheme.colorScheme.outline
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
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
            if (option.imageUrl != null) {
                val stableOptionRequest = remember(option.imageUrl) {
                    ImageRequest.Builder(context)
                        .data(option.imageUrl)
                        .crossfade(false)
                        .memoryCachePolicy(CachePolicy.ENABLED)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .build()
                }
                AsyncImage(
                    model = stableOptionRequest,
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
                isSelected -> Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
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
        }
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
