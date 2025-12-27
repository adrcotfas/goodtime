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
import Foundation
import UIKit
import UniformTypeIdentifiers
import ComposeApp

/// Bridges Kotlin backup/export & restore/import to iOS UI.
final class BackupUiManager: NSObject, BackupUiDelegate {
    static let shared = BackupUiManager()

    private var importDelegate: DocumentPickerDelegate?

    private override init() {
        super.init()
        BackupUiBridge.shared.delegate = self
        print("[BackupUiManager] Initialized and registered as delegate")
    }

    func startExport(token: String, filePath: String, mimeType: String) {
        DispatchQueue.main.async {
            guard let presenter = Self.topViewController() else {
                print("[BackupUiManager] No presenter available for export")
                BackupUiBridge.shared.complete(token: token, result: BackupPromptResult.failed)
                return
            }

            let url = URL(fileURLWithPath: filePath)
            let controller = UIActivityViewController(activityItems: [url], applicationActivities: nil)
            controller.completionWithItemsHandler = { _, completed, _, error in
                if completed && (error == nil) {
                    BackupUiBridge.shared.complete(token: token, result: BackupPromptResult.success)
                } else if !completed && (error == nil) {
                    // User dismissed without saving/sharing.
                    BackupUiBridge.shared.complete(token: token, result: BackupPromptResult.cancelled)
                } else {
                    BackupUiBridge.shared.complete(token: token, result: BackupPromptResult.failed)
                }
            }
            presenter.present(controller, animated: true)
        }
    }

    func startImport(token: String, destinationPath: String) {
        DispatchQueue.main.async {
            guard let presenter = Self.topViewController() else {
                print("[BackupUiManager] No presenter available for import")
                BackupUiBridge.shared.complete(token: token, result: BackupPromptResult.failed)
                return
            }

            // Allow picking any file; Kotlin validates SQLite header after copy.
            let picker = UIDocumentPickerViewController(forOpeningContentTypes: [UTType.data], asCopy: true)
            let delegate = DocumentPickerDelegate(token: token, destinationPath: destinationPath)
            picker.delegate = delegate

            // Keep delegate alive until picker completes.
            self.importDelegate = delegate

            presenter.present(picker, animated: true)
        }
    }

    private final class DocumentPickerDelegate: NSObject, UIDocumentPickerDelegate {
        private let token: String
        private let destinationPath: String

        init(token: String, destinationPath: String) {
            self.token = token
            self.destinationPath = destinationPath
        }

        func documentPicker(_ controller: UIDocumentPickerViewController, didPickDocumentsAt urls: [URL]) {
            guard let url = urls.first else {
                BackupUiBridge.shared.complete(token: token, result: BackupPromptResult.cancelled)
                return
            }

            let destinationURL = URL(fileURLWithPath: destinationPath)

            let needsSecurity = url.startAccessingSecurityScopedResource()
            defer {
                if needsSecurity {
                    url.stopAccessingSecurityScopedResource()
                }
            }

            do {
                // Ensure destination directory exists.
                let dir = destinationURL.deletingLastPathComponent()
                try FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)

                // Replace existing file.
                if FileManager.default.fileExists(atPath: destinationURL.path) {
                    try FileManager.default.removeItem(at: destinationURL)
                }

                try FileManager.default.copyItem(at: url, to: destinationURL)
                BackupUiBridge.shared.complete(token: token, result: BackupPromptResult.success)
            } catch {
                print("[BackupUiManager] Import copy failed: \(error)")
                BackupUiBridge.shared.complete(token: token, result: BackupPromptResult.failed)
            }
        }

        func documentPickerWasCancelled(_ controller: UIDocumentPickerViewController) {
            BackupUiBridge.shared.complete(token: token, result: BackupPromptResult.cancelled)
        }
    }

    private static func topViewController() -> UIViewController? {
        // Support multi-scene apps (iOS 13+).
        let scenes = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .filter { $0.activationState == .foregroundActive || $0.activationState == .foregroundInactive }

        let windows = scenes.flatMap { $0.windows }
        let keyWindow = windows.first(where: { $0.isKeyWindow }) ?? windows.first
        guard var top = keyWindow?.rootViewController else { return nil }

        while let presented = top.presentedViewController {
            top = presented
        }
        return top
    }
}
