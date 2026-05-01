import BackgroundTasks
import ComposeApp
import GoogleSignIn
import SwiftUI
import UserNotifications

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

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

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?
    ) -> Bool {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }

        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: BackgroundSyncSchedulerImpl.Companion().TASK_IDENTIFIER,
            using: nil
        ) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }
            let title = NSLocalizedString("notif_unread_title", comment: "")
            let body: (Int32) -> String = { count in
                String(format: NSLocalizedString("notif_unread_body", comment: ""), count)
            }
            UnreadMessagesSyncTaskKt.performUnreadSync(title: title, body: body) { success in
                refreshTask.setTaskCompleted(success: success?.boolValue ?? false)
            }
        }

        return true
    }
}
