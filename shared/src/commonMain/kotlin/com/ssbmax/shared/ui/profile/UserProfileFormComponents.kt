package com.ssbmax.shared.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.domain.model.EntryType
import com.ssbmax.shared.domain.model.Gender

/**
 * Avatar placeholder + gender/entry-type selector chips for
 * [UserProfileScreen], split into their own file to keep the main screen
 * file under this repo's 300-line Quality Limit.
 */
@Composable
internal fun ProfilePicturePlaceholder(initials: String) {
    Box(
        modifier = Modifier
            .size(100.dp)
            .background(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun GenderSelector(
    selectedGender: Gender?,
    onGenderSelected: (Gender) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Gender.entries.forEach { gender ->
            FilterChip(
                selected = selectedGender == gender,
                onClick = { onGenderSelected(gender) },
                label = { Text(gender.displayName) },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
internal fun EntryTypeSelector(
    selectedEntryType: EntryType?,
    onEntryTypeSelected: (EntryType) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        EntryType.entries.forEach { entryType ->
            FilterChip(
                selected = selectedEntryType == entryType,
                onClick = { onEntryTypeSelected(entryType) },
                label = { Text(entryType.displayName) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
