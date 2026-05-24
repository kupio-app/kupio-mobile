package kupio.mobile.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

class AndroidUrlOpener(private val context: Context) : UrlOpener {
    override fun openUrl(url: String) {
        val uri = Uri.parse(url)
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
        customTabsIntent.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        customTabsIntent.launchUrl(context, uri)
    }
}
