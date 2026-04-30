package kupio.mobile.core.platform

import platform.Foundation.NSURLComponents
import platform.UIKit.UIApplication

class IosPhoneDialer : PhoneDialer {
    override fun openDialer(phone: String): Boolean {
        val sanitizedPhone = phone.trim()
        val url = NSURLComponents().apply {
            scheme = "tel"
            path = sanitizedPhone
        }.URL ?: return false
        val application = UIApplication.sharedApplication
        if (!application.canOpenURL(url)) return false
        application.openURL(url, options = emptyMap<Any?, Any>(), completionHandler = null)
        return true
    }
}
