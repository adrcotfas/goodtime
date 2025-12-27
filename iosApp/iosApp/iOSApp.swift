import SwiftUI
import ComposeApp
import UserNotifications

@main
struct iOSApp: App {
    @Environment(\.scenePhase) private var scenePhase

    init() {
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
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
        }.onChange(of: scenePhase) { oldPhase, newPhase in
            if newPhase == .active {
                // Clear notifications when app becomes active
                UNUserNotificationCenter.current().removeAllDeliveredNotifications()
            }
        }
    }
}
