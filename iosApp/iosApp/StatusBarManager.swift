/**
 *     Goodtime Productivity
 *     Copyright (C) 2025 Adrian Cotfas
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

import SwiftUI
import Observation
import ComposeApp

/// Manages the visibility of the iOS status bar.
/// Bridges between Kotlin (StatusBarState) and SwiftUI (@Observable state).
@Observable
class StatusBarManager: StatusBarDelegate {
    /// Shared singleton instance
    static let shared = StatusBarManager()

    /// Whether the status bar should be hidden (observable by SwiftUI)
    var isStatusBarHidden = false

    private init() {
        // Register as delegate to receive updates from Kotlin
        StatusBarState.shared.statusBarDelegate = self
        print("[StatusBarManager] Initialized and registered as delegate")
    }

    /// Called from Kotlin when status bar visibility changes
    func onStatusBarVisibilityChanged(isHidden: Bool) {
        print("[StatusBarManager] Status bar visibility changed: \(isHidden)")
        // Update the observable state on main thread
        DispatchQueue.main.async {
            self.isStatusBarHidden = isHidden
        }
    }
}
