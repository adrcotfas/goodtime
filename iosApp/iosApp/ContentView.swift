import UIKit
import SwiftUI
import ComposeApp

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    // Observe the status bar manager to react to fullscreen changes
    @State private var statusBarManager = StatusBarManager.shared

    var body: some View {
        ComposeView()
            .ignoresSafeArea()
            .statusBar(hidden: statusBarManager.isStatusBarHidden)
    }
}
