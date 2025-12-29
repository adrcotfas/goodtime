import SwiftUI
import ComposeApp
import UserNotifications

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate
    @Environment(\.scenePhase) private var scenePhase

	var body: some Scene {
		WindowGroup {
			ContentView()
        }.onChange(of: scenePhase) {
            if scenePhase == .active {
                // Clear notifications when app becomes active
                UNUserNotificationCenter.current().removeAllDeliveredNotifications()
            }
        }
    }

    class AppDelegate: NSObject, UIApplicationDelegate {
        func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey : Any]? = nil) -> Bool {

            PurchasePlatform_iosKt.configurePurchasesFromPlatform()

            // Register Live Activity delegate
            if #available(iOS 16.1, *) {
                LiveActivityBridge.companion.shared.setDelegate(delegate: GoodtimeLiveActivityManager.shared)
                print("[iOSApp] Live Activity delegate registered")
            }

            // Initialize the intent handler (it will get TimerManager lazily when needed)
            _ = LiveActivityIntentHandler.shared
            print("[iOSApp] Live Activity intent handler initialized")

            // Initialize status bar manager to enable fullscreen mode support
            _ = StatusBarManager.shared
            print("[iOSApp] Status bar manager initialized")

            // Initialize backup UI bridge (share sheet + document picker)
            _ = BackupUiManager.shared
            print("[iOSApp] Backup UI manager initialized")

            // Register background task for cloud backups
            BackgroundTaskIOSKt.registerCloudBackupTask()
            print("[iOSApp] Cloud backup background task registered")

            return true
        }
    }
}
