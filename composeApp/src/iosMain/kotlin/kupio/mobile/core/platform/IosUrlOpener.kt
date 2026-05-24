package kupio.mobile.core.platform

import platform.Foundation.NSURL
import platform.SafariServices.SFSafariViewController
import platform.UIKit.UIApplication

class IosUrlOpener : UrlOpener {
    override fun openUrl(url: String) {
        val nsUrl = NSURL.URLWithString(url) ?: return
        val sfvc = SFSafariViewController(nsUrl)
        UIApplication.sharedApplication.keyWindow
            ?.rootViewController
            ?.presentViewController(sfvc, animated = true, completion = null)
    }
}
