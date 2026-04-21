import ComposeApp
import GoogleSignIn
import SwiftUI

@main
struct iOSApp: App {
    init() {
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
