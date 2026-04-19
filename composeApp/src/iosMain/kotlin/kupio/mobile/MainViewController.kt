package kupio.mobile

import androidx.compose.ui.window.ComposeUIViewController
import kupio.mobile.app.App
import kupio.mobile.core.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController {
        App()
    }
}
