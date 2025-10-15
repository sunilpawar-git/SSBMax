package com.ssbmax.ui.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ssbmax.R

/**
 * Splash screen with auto-authentication check
 * Displays for 2-3 seconds while checking auth state
 */
@Composable
fun SplashScreen(
    viewModel: SplashViewModel = hiltViewModel(),
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: (isStudent: Boolean) -> Unit,
    onNavigateToRoleSelection: () -> Unit
) {
    val navigationEvent by viewModel.navigationEvent.collectAsStateWithLifecycle()
    
    // Handle navigation events
    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {
            is SplashNavigationEvent.NavigateToLogin -> onNavigateToLogin()
            is SplashNavigationEvent.NavigateToStudentHome -> onNavigateToHome(true)
            is SplashNavigationEvent.NavigateToInstructorHome -> onNavigateToHome(false)
            is SplashNavigationEvent.NavigateToRoleSelection -> onNavigateToRoleSelection()
            null -> { /* Stay on splash */ }
        }
    }
    
    // Animation for fade-in effect
    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "splash_fade_in"
    )
    
    // Pulsing animation for logo
    val infiniteTransition = rememberInfiniteTransition(label = "splash_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )
    
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo placeholder - replace with actual logo
                Text(
                    text = "SSBMax",
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.alpha(scale)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Your Path to SSB Success",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                
                Spacer(modifier = Modifier.height(48.dp))
                
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            
            // Version info at bottom
            Text(
                text = "Version 1.0.0",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}

