package kupio.mobile.features.auth.presentation.auth

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
            callback.onFailure(GoogleSignInFailureCode.ActivityUnavailable)
            return
        }
        if (BuildConfig.KUPIO_GOOGLE_SERVER_CLIENT_ID.isBlank()) {
            Log.e(GoogleSignInTag, "Missing server client id in BuildConfig")
            callback.onFailure(GoogleSignInFailureCode.NotConfigured)
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
                    credential.type in setOf(
                        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL,
                        GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_SIWG_CREDENTIAL,
                    )
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleCredential.idToken
                    if (idToken.isBlank()) {
                        callback.onFailure(GoogleSignInFailureCode.MissingIdToken)
                    } else {
                        callback.onSuccess(idToken)
                    }
                } else {
                    Log.e(GoogleSignInTag, "Unsupported Google credential type: ${credential.type}")
                    callback.onFailure(GoogleSignInFailureCode.InvalidResponse)
                }
            } catch (exception: GetCredentialCancellationException) {
                val message = exception.message.orEmpty()
                if (message.contains("reauth failed", ignoreCase = true)) {
                    Log.e(
                        GoogleSignInTag,
                        "Google account re-authentication failed. " +
                            "Check the Android OAuth app setup for package kupio.mobile " +
                            "and its SHA fingerprints, then try again.",
                        exception,
                    )
                    callback.onFailure(GoogleSignInFailureCode.Failed)
                } else {
                    callback.onCancelled()
                }
            } catch (exception: NoCredentialException) {
                callback.onFailure(GoogleSignInFailureCode.NoCredential)
            } catch (exception: GoogleIdTokenParsingException) {
                Log.e(GoogleSignInTag, "Could not parse the Google sign-in response.", exception)
                callback.onFailure(GoogleSignInFailureCode.InvalidResponse)
            } catch (exception: GetCredentialException) {
                Log.e(GoogleSignInTag, "Google sign-in failed.", exception)
                callback.onFailure(GoogleSignInFailureCode.Failed)
            } catch (exception: Throwable) {
                Log.e(GoogleSignInTag, "Google sign-in failed unexpectedly.", exception)
                callback.onFailure(GoogleSignInFailureCode.Failed)
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
