import ComposeApp
import FirebaseCore
import FirebaseCrashlytics
import GoogleSignIn
import SwiftUI

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        #if DEBUG
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #endif
        GoogleSignInBridgeKt.registerGoogleSignInLauncher(launcher: IOSGoogleSignInLauncher())
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                }
        }
    }
}
