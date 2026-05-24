package kupio.mobile

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.mmk.kmpnotifier.extensions.onCreateOrOnNewIntent
import com.mmk.kmpnotifier.notification.NotifierManager
import kupio.mobile.app.App
import kupio.mobile.core.navigation.DeepLinkNavigator
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied - nothing to do here */ }

    private val deepLinkNavigator: DeepLinkNavigator by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        NotifierManager.onCreateOrOnNewIntent(intent)
        intent.data?.toString()?.let { deepLinkNavigator.handle(it) }

        setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .appiumTestTagsAsResourceIds(),
            ) {
                App()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        NotifierManager.onCreateOrOnNewIntent(intent)
        intent.data?.toString()?.let { deepLinkNavigator.handle(it) }
    }
}

private fun Modifier.appiumTestTagsAsResourceIds(): Modifier =
    if (BuildConfig.DEBUG) {
        semantics { testTagsAsResourceId = true }
    } else {
        this
    }
