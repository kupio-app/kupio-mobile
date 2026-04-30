package kupio.mobile.core.platform

import android.content.Context
import android.content.Intent
import android.net.Uri

class AndroidPhoneDialer(
    private val context: Context,
) : PhoneDialer {
    override fun openDialer(phone: String): Boolean {
        val sanitizedPhone = phone.trim()
        val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", sanitizedPhone, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching {
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}
