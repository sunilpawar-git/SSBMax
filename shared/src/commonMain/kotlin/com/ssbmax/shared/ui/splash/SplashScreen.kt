package com.ssbmax.shared.ui.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ssbmax.shared.presentation.splash.SplashNavigationEvent
import com.ssbmax.shared.presentation.splash.SplashViewModel
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ssbmax.shared.generated.resources.Res
import ssbmax.shared.generated.resources.splash_app_name
import ssbmax.shared.generated.resources.splash_tagline
import ssbmax.shared.generated.resources.splash_version

/**
 * Splash screen with auto-authentication check — Phase 5 KMP port of the
 * Android original (app/.../ui/splash/SplashScreen.kt). First screen ported
 * as the "one full vertical slice" for this phase (per the migration plan's
 * step 2), chosen because it has no Android-only Compose APIs (no
 * `LocalContext`, no `ActivityResultContracts`) unlike e.g. LoginScreen,
 * which needs a real Google Sign-In expect/actual shim not yet built.
 *
 * Two deliberate deviations from the Android original, both noted, not
 * silently different:
 * - `koinInject()`, not `koinViewModel()` / a default-arg constructor param —
 *   this codebase's KMP ViewModels are plain classes (see [SplashViewModel]
 *   doc), so they're resolved like any other Koin single/factory.
 * - `collectAsState()`, not `collectAsStateWithLifecycle()` — the Android
 *   original uses the latter, but this screen uses plain `collectAsState()`
 *   to sidestep needing a real `LifecycleOwner` on iOS; revisit if iOS's
 *   CMP `LifecycleOwner` support is confirmed sufficient in a later phase.
 * - Dropped two unused imports present in the Android original
 *   (`androidx.compose.foundation.Image`, `androidx.compose.ui.res.painterResource`)
 *   — grep-confirmed dead in the original (no `Image(...)` call in the body).
 */
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToHome: (isStudent: Boolean) -> Unit,
    onNavigateToRoleSelection: () -> Unit,
    onNavigateToProfileOnboarding: () -> Unit
) {
    val viewModel = koinInject<SplashViewModel>()
    val navigationEvent by viewModel.navigationEvent.collectAsState()

    LaunchedEffect(navigationEvent) {
        when (navigationEvent) {
            is SplashNavigationEvent.NavigateToLogin -> onNavigateToLogin()
            is SplashNavigationEvent.NavigateToStudentHome -> onNavigateToHome(true)
            is SplashNavigationEvent.NavigateToInstructorHome -> onNavigateToHome(false)
            is SplashNavigationEvent.NavigateToRoleSelection -> onNavigateToRoleSelection()
            is SplashNavigationEvent.NavigateToProfileOnboarding -> onNavigateToProfileOnboarding()
            null -> { /* Stay on splash */ }
        }
    }

    val alpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "splash_fade_in"
    )

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
            modifier = Modifier.fillMaxSize().alpha(alpha),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(Res.string.splash_app_name),
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.alpha(scale)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(Res.string.splash_tagline),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                )
                Spacer(modifier = Modifier.height(48.dp))
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(32.dp)
                )
            }
            Text(
                text = stringResource(Res.string.splash_version),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 32.dp)
            )
        }
    }
}
