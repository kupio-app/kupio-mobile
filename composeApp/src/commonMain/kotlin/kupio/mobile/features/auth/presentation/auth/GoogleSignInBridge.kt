package kupio.mobile.features.auth.presentation.auth

import androidx.compose.runtime.Composable

interface GoogleSignInLauncher {
    fun signIn(
        callback: GoogleSignInCallback,
    )
}

interface GoogleSignInCallback {
    fun onSuccess(
        idToken: String,
    )

    fun onFailure(
        message: String,
    )

    fun onCancelled()
}

private object UnavailableGoogleSignInLauncher : GoogleSignInLauncher {
    override fun signIn(
        callback: GoogleSignInCallback,
    ) {
        callback.onFailure("Google sign-in is not configured.")
    }
}

private var registeredGoogleSignInLauncher: GoogleSignInLauncher? = null

fun registerGoogleSignInLauncher(
    launcher: GoogleSignInLauncher,
) {
    registeredGoogleSignInLauncher = launcher
}

internal fun currentGoogleSignInLauncher(): GoogleSignInLauncher {
    return registeredGoogleSignInLauncher ?: UnavailableGoogleSignInLauncher
}

@Composable
expect fun rememberGoogleSignInLauncher(): GoogleSignInLauncher
