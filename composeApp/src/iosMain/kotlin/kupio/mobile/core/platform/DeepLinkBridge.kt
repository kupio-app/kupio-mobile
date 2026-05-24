package kupio.mobile.core.platform

import kupio.mobile.core.navigation.DeepLinkNavigator
import org.koin.mp.KoinPlatformTools

fun handleDeepLinkUri(uri: String) {
    KoinPlatformTools.defaultContext().get().get<DeepLinkNavigator>().handle(uri)
}
