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
        errorCode: String,
    )

    fun onCancelled()
}

object GoogleSignInFailureCode {
    const val NotConfigured = "not_configured"
    const val ActivityUnavailable = "activity_unavailable"
    const val MissingIdToken = "missing_id_token"
    const val NoCredential = "no_credential"
    const val InvalidResponse = "invalid_response"
    const val Failed = "failed"
}

private object UnavailableGoogleSignInLauncher : GoogleSignInLauncher {
    override fun signIn(
        callback: GoogleSignInCallback,
    ) {
        callback.onFailure(GoogleSignInFailureCode.NotConfigured)
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
