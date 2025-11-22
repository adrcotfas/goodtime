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

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.contact_address
import goodtime_productivity.composeapp.generated.resources.feedback_title
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString

class AndroidFeedbackHelper(
    private val context: Context,
) : FeedbackHelper {
    override fun sendFeedback() {
        runBlocking {
            val email = Intent(Intent.ACTION_SENDTO)
            email.data = Uri.Builder().scheme("mailto").build()
            email.putExtra(Intent.EXTRA_EMAIL, arrayOf(getString(Res.string.contact_address)))
            email.putExtra(Intent.EXTRA_SUBJECT, getString(Res.string.feedback_title))
            email.putExtra(
                Intent.EXTRA_TEXT,
                getFeedbackEmailBody(
                    deviceInfo = getDeviceInfo(),
                    appVersion = "${context.getVersionName()}(${context.getVersionCode()})",
                ),
            )
            try {
                val chooser =
                    Intent.createChooser(email, "Send feedback").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                context.startActivity(chooser)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, "There are no email clients installed.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getDeviceInfo(): String {
        val manufacturer = Build.MANUFACTURER
        val model = Build.MODEL
        val version = Build.VERSION.SDK_INT
        return "$manufacturer $model API $version"
    }
}
