package kupio.mobile.features.auth.presentation

import android.content.Context
import android.content.ContextWrapper
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import androidx.lifecycle.lifecycleScope
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import kupio.mobile.BuildConfig
import kotlinx.coroutines.launch

@Composable
actual fun rememberGoogleSignInLauncher(): GoogleSignInLauncher {
    val context = LocalContext.current
    val activity = context.findActivity()

    return remember(activity) {
        AndroidGoogleSignInLauncher(activity = activity)
    }
}

private class AndroidGoogleSignInLauncher(
    private val activity: ComponentActivity?,
) : GoogleSignInLauncher {
    override fun signIn(
        callback: GoogleSignInCallback,
    ) {
        val currentActivity = activity
        if (currentActivity == null) {
            callback.onFailure("Google sign-in requires an Android activity.")
            return
        }
        if (BuildConfig.KUPIO_GOOGLE_SERVER_CLIENT_ID.isBlank()) {
            callback.onFailure("Set KUPIO_GOOGLE_SERVER_CLIENT_ID before using Google sign-in.")
            return
        }

        currentActivity.lifecycleScope.launch {
            val credentialManager = CredentialManager.create(currentActivity)
            val googleOption = GetSignInWithGoogleOption.Builder(
                BuildConfig.KUPIO_GOOGLE_SERVER_CLIENT_ID,
            ).build()
            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleOption)
                .build()

            try {
                val result = credentialManager.getCredential(
                    request = request,
                    context = currentActivity,
                )
                val credential = result.credential
                if (
                    credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleCredential.idToken
                    if (idToken.isBlank()) {
                        callback.onFailure("Google did not return an ID token.")
                    } else {
                        callback.onSuccess(idToken)
                    }
                } else {
                    callback.onFailure("Unexpected Google sign-in response.")
                }
            } catch (_: GetCredentialCancellationException) {
                callback.onCancelled()
            } catch (_: NoCredentialException) {
                callback.onFailure("No Google account is available on this device.")
            } catch (_: GoogleIdTokenParsingException) {
                callback.onFailure("Could not parse the Google sign-in response.")
            } catch (exception: GetCredentialException) {
                callback.onFailure(
                    exception.message ?: "Google sign-in failed.",
                )
            }
        }
    }
}

private tailrec fun Context.findActivity(): ComponentActivity? {
    return when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}
