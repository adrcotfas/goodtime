import SwiftUI
import ComposeApp

@main
struct iOSApp: App {

    init() {
        // Set up the Live Activity delegate
        LiveActivityBridge.companion.setDelegate(del: LiveActivityDelegateImpl())
        print("[iOSApp] Live Activity delegate registered")

        // Initialize the intent handler (it will get TimerManager lazily when needed)
        _ = LiveActivityIntentHandler.shared
        print("[iOSApp] Live Activity intent handler initialized")
    }

	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}