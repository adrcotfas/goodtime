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
package com.apps.adrcotfas.goodtime.common

/**
 * Platform-specific feedback helper for sending user feedback.
 */
interface FeedbackHelper {
    /**
     * Opens the platform's email client with pre-filled feedback information.
     */
    fun sendFeedback()
}

/**
 * Generates the email body template for feedback.
 * @param deviceInfo Platform-specific device information (e.g., "Samsung Galaxy S21 API 31")
 * @param appVersion The app version string (e.g., "3.0.0(123)")
 */
fun getFeedbackEmailBody(
    deviceInfo: String,
    appVersion: String,
): String =
    """
    * Pick a category:

    Feedback:
       - What do you like about the app?
       - What can be improved?

    Feature Request:
       - Describe the feature you would like to see.
       - How would this feature benefit you?

    Found Bug:
       - Describe the issue you encountered.
       - What are the steps to reproduce the issue?

    Device info: $deviceInfo
    App version: $appVersion
    """.trimIndent()
