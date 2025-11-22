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

import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.contact_address
import goodtime_productivity.composeapp.generated.resources.feedback_title
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import platform.Foundation.NSBundle
import platform.Foundation.NSCharacterSet
import platform.Foundation.NSString
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice

class IosFeedbackHelper : FeedbackHelper {
    override fun sendFeedback() {
        runBlocking {
            val emailAddress = getString(Res.string.contact_address)
            val subject = getString(Res.string.feedback_title)
            val body =
                getFeedbackEmailBody(
                    deviceInfo = getDeviceInfo(),
                    appVersion = getAppVersion(),
                )

            // URL encode the subject and body
            val encodedSubject = subject.urlEncode()
            val encodedBody = body.urlEncode()

            val mailtoUrl = "mailto:$emailAddress?subject=$encodedSubject&body=$encodedBody"
            val nsUrl = NSURL.URLWithString(mailtoUrl) ?: return@runBlocking

            if (UIApplication.sharedApplication.canOpenURL(nsUrl)) {
                UIApplication.sharedApplication.openURL(nsUrl)
            }
        }
    }

    private fun getDeviceInfo(): String {
        val device = UIDevice.currentDevice
        val model = device.model
        val systemName = device.systemName
        val systemVersion = device.systemVersion
        return "$model $systemName $systemVersion"
    }

    private fun getAppVersion(): String {
        val bundle = NSBundle.mainBundle
        val version = bundle.objectForInfoDictionaryKey("CFBundleShortVersionString") as? String ?: "Unknown"
        val build = bundle.objectForInfoDictionaryKey("CFBundleVersion") as? String ?: "Unknown"
        return "$version($build)"
    }

    private fun String.urlEncode(): String {
        val nsString = NSString.create(string = this)
        return nsString.stringByAddingPercentEncodingWithAllowedCharacters(
            NSCharacterSet.URLQueryAllowedCharacterSet,
        ) ?: this
    }
}
