import BackgroundTasks
import ComposeApp
import FirebaseCore
import FirebaseCrashlytics
import GoogleSignIn
import SwiftUI
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate {

    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {

        // Initializes Firebase services used by the iOS app.
        FirebaseApp.configure()

        #if DEBUG
        // Disables Crashlytics collection for debug builds.
        Crashlytics.crashlytics().setCrashlyticsCollectionEnabled(false)
        #endif

        // Initializes KMPNotifier for iOS local notifications.
        // Push display is disabled because iOS remote push/APNs is not configured.
        NotifierManager.shared.initialize(
            configuration: NotificationPlatformConfigurationIos(
                showPushNotification: false,
                askNotificationPermissionOnStart: true,
                notificationSoundName: nil
            )
        )

        // Registers the iOS Google Sign-In launcher used from shared Kotlin code.
        GoogleSignInBridgeKt.registerGoogleSignInLauncher(
            launcher: IOSGoogleSignInLauncher()
        )

        // Requests permission to show local notifications on iOS.
        UNUserNotificationCenter.current().requestAuthorization(
            options: [.alert, .sound, .badge]
        ) { _, _ in }

        // Registers a background refresh task for unread message synchronization.
        BGTaskScheduler.shared.register(
            forTaskWithIdentifier: BackgroundSyncSchedulerImpl.Companion().TASK_IDENTIFIER,
            using: nil
        ) { task in
            guard let refreshTask = task as? BGAppRefreshTask else {
                task.setTaskCompleted(success: false)
                return
            }

            let title = NSLocalizedString("notif_unread_title", comment: "")
            let body: (KotlinInt) -> String = { count in
                String(
                    format: NSLocalizedString("notif_unread_body", comment: ""),
                    count.int32Value
                )
            }

            // Runs shared Kotlin unread-message sync and completes the iOS background task.
            UnreadMessagesSyncTaskKt.performUnreadSync(title: title, body: body) { success in
                refreshTask.setTaskCompleted(success: success.boolValue)
            }
        }

        return true
    }
}

@main
struct iOSApp: App {

    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    GIDSignIn.sharedInstance.handle(url)
                    if url.scheme == "kupio" {
                        DeepLinkBridgeKt.handleDeepLinkUri(uri: url.absoluteString)
                    }
                }
        }
    }
}
