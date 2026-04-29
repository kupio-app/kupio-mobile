package kupio.mobile.core.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

class IosPhoneDialer : PhoneDialer {
    override fun openDialer(phone: String): Boolean {
        val url = NSURL.URLWithString("tel:$phone") ?: return false
        val application = UIApplication.sharedApplication
        if (!application.canOpenURL(url)) return false
        application.openURL(url)
        return true
    }
}
