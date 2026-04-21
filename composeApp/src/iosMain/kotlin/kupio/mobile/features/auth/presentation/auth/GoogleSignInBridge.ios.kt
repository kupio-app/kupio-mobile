package kupio.mobile.features.auth.presentation.auth

import androidx.compose.runtime.Composable

@Composable
actual fun rememberGoogleSignInLauncher(): GoogleSignInLauncher {
    return currentGoogleSignInLauncher()
}
