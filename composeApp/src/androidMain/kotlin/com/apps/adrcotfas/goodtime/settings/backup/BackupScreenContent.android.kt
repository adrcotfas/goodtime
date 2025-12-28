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
package com.apps.adrcotfas.goodtime.settings.backup

import android.net.Uri
import androidx.core.net.toUri

/**
 * Formats a content URI path to a human-readable folder path.
 * Example: content://...tree/primary%3ADocuments%2FGoodtime -> Documents/Goodtime
 *
 * This implementation properly handles Android's document tree URIs by:
 * - Using Android's Uri.decode() for proper URL decoding
 * - Extracting the document ID from the tree URI structure
 * - Removing storage volume prefixes (primary:, home:, etc.)
 */
actual fun formatFolderPath(uriPath: String): String {
    return try {
        val uri = uriPath.toUri()

        // Extract the document ID from the tree URI
        // Tree URIs have the format: content://authority/tree/documentId
        val documentId = uri.lastPathSegment ?: return uriPath

        // Decode the document ID
        val decoded = Uri.decode(documentId)

        // Remove storage volume prefix (e.g., "primary:", "home:", etc.)
        // The prefix format is always "{volume}:{path}"
        val colonIndex = decoded.indexOf(':')
        if (colonIndex != -1 && colonIndex < decoded.length - 1) {
            decoded.substring(colonIndex + 1)
        } else {
            // Fallback if no colon found
            decoded
        }
    } catch (e: Exception) {
        // If anything fails, return the original path
        uriPath
    }
}
