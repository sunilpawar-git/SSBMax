package com.ssbmax.ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ssbmax.core.domain.model.UserRole

/**
 * Role selection screen for new users
 * Allows user to choose between Student and Instructor role
 */
@Composable
fun RoleSelectionScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onRoleSelected: (UserRole) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedRole by remember { mutableStateOf<UserRole?>(null) }
    
    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Welcome to SSBMax!",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "How would you like to use SSBMax?",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RoleCard(
                    title = "I'm a Student",
                    description = "Prepare for SSB interviews, take tests, and track progress",
                    icon = Icons.Default.School,
                    isSelected = selectedRole == UserRole.STUDENT,
                    onClick = { selectedRole = UserRole.STUDENT },
                    modifier = Modifier.weight(1f)
                )
                
                RoleCard(
                    title = "I'm an Instructor",
                    description = "Create batches, grade tests, and guide students",
                    icon = Icons.Default.Groups,
                    isSelected = selectedRole == UserRole.INSTRUCTOR,
                    onClick = { selectedRole = UserRole.INSTRUCTOR },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = {
                    selectedRole?.let { role ->
                        viewModel.setUserRole(role)
                        onRoleSelected(role)
                    }
                },
                enabled = selectedRole != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            TextButton(
                onClick = { selectedRole = UserRole.BOTH }
            ) {
                Text("I want to use both roles")
            }
        }
    }
}

@Composable
private fun RoleCard(
    title: String,
    description: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 2.dp,
            color = if (isSelected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            }
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

