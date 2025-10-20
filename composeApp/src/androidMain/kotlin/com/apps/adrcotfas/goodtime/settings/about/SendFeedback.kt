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
package com.apps.adrcotfas.goodtime.settings.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.common.getVersionCode
import com.apps.adrcotfas.goodtime.common.getVersionName

fun getDeviceInfo(): String {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    val version = Build.VERSION.SDK_INT
    return "$manufacturer $model API $version"
}

fun sendFeedback(context: Context) {
    val email = Intent(Intent.ACTION_SENDTO)
    email.data = Uri.Builder().scheme("mailto").build()
    email.putExtra(Intent.EXTRA_EMAIL, arrayOf(context.getString(R.string.contact_address)))
    email.putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.feedback_title))
    email.putExtra(
        Intent.EXTRA_TEXT,
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

        Device info: ${getDeviceInfo()}
        App version: ${context.getVersionName()}(${context.getVersionCode()})
        """.trimIndent(),
    )
    try {
        context.startActivity(Intent.createChooser(email, "Send feedback"))
    } catch (ex: ActivityNotFoundException) {
        Toast.makeText(context, "There are no email clients installed.", Toast.LENGTH_SHORT).show()
    }
}
