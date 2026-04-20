package kupio.mobile.features.auth.presentation

import android.content.Context
import android.content.ContextWrapper
import android.util.Log
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

private const val GoogleSignInTag = "KupioGoogleSignIn"

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
            Log.e(GoogleSignInTag, "Cannot launch Google sign-in without an activity")
            callback.onFailure("Google sign-in requires an Android activity.")
            return
        }
        if (BuildConfig.KUPIO_GOOGLE_SERVER_CLIENT_ID.isBlank()) {
            Log.e(GoogleSignInTag, "Missing server client id in BuildConfig")
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
                Log.d(GoogleSignInTag, "Launching Google credential request")
                val result = credentialManager.getCredential(
                    request = request,
                    context = currentActivity,
                )
                val credential = result.credential
                Log.d(
                    GoogleSignInTag,
                    "Received credential type=${credential.type}",
                )
                if (
                    credential is CustomCredential &&
                    credential.type in setOf(
                        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
                        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL,
                    )
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleCredential.idToken
                    if (idToken.isBlank()) {
                        Log.e(GoogleSignInTag, "Google returned a blank ID token")
                        callback.onFailure("Google did not return an ID token.")
                    } else {
                        Log.d(GoogleSignInTag, "Google sign-in produced a non-empty ID token")
                        callback.onSuccess(idToken)
                    }
                } else {
                    val message = "Unsupported Google credential type: ${credential.type}"
                    Log.e(GoogleSignInTag, message)
                    callback.onFailure(message)
                }
            } catch (exception: GetCredentialCancellationException) {
                val message = exception.message.orEmpty()
                Log.w(
                    GoogleSignInTag,
                    "Google sign-in was cancelled: $message",
                    exception,
                )
                if (message.contains("reauth failed", ignoreCase = true)) {
                    callback.onFailure(
                        "Google account re-authentication failed. " +
                            "Check the Android OAuth app setup for package kupio.mobile " +
                            "and its SHA fingerprints, then try again.",
                    )
                } else {
                    callback.onCancelled()
                }
            } catch (exception: NoCredentialException) {
                Log.e(
                    GoogleSignInTag,
                    "No Google credential available: ${exception.message}",
                    exception,
                )
                callback.onFailure("No Google account is available on this device.")
            } catch (exception: GoogleIdTokenParsingException) {
                Log.e(
                    GoogleSignInTag,
                    "Could not parse Google sign-in response: ${exception.message}",
                    exception,
                )
                callback.onFailure("Could not parse the Google sign-in response.")
            } catch (exception: GetCredentialException) {
                Log.e(
                    GoogleSignInTag,
                    "Credential manager failed: ${exception.message}",
                    exception,
                )
                callback.onFailure(
                    exception.message ?: "Google sign-in failed.",
                )
            } catch (exception: Throwable) {
                Log.e(
                    GoogleSignInTag,
                    "Unexpected Google sign-in failure: ${exception.message}",
                    exception,
                )
                callback.onFailure(
                    exception.message ?: "Google sign-in failed unexpectedly.",
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
