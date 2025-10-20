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

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.LocaleManager
import android.content.ContentResolver
import android.content.Context
import android.content.Context.ALARM_SERVICE
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.LocaleList
import android.os.PowerManager
import android.provider.OpenableColumns
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import java.io.File
import kotlin.time.Duration.Companion.days

tailrec fun Context.findActivity(): ComponentActivity? =
    when (this) {
        is ComponentActivity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun Context.getAppLanguage(): String {
    val locale = resources.configuration.locales[0]
    val currentAppLocales: LocaleList =
        this
            .getSystemService(LocaleManager::class.java)
            .getApplicationLocales(this.packageName)

    return (
        if (!currentAppLocales.isEmpty) {
            currentAppLocales[0].getDisplayLanguage(locale)
        } else {
            locale.displayLanguage
        }
    ).replaceFirstChar { it.uppercase() }
}

fun Context.getFileName(uri: Uri): String? =
    when (uri.scheme) {
        ContentResolver.SCHEME_CONTENT -> getContentFileName(uri)
        else -> uri.path?.let(::File)?.name
    }?.substringBeforeLast('.')

private fun Context.getContentFileName(uri: Uri): String? =
    runCatching {
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            cursor.moveToFirst()
            return@use cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME).let(cursor::getString)
        }
    }.getOrNull()

fun Context.getVersionName(): String {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    return packageInfo.versionName!!
}

fun Context.getVersionCode(): Long {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    val verCode = PackageInfoCompat.getLongVersionCode(packageInfo)
    return verCode
}

@SuppressLint("BatteryLife")
fun Context.askForDisableBatteryOptimization() {
    val intent = Intent()
    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
    intent.data = ("package:" + this.packageName).toUri()
    this.startActivity(intent)
}

fun Context.askForAlarmPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val intent =
            Intent().apply {
                action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
            }
        this.startActivity(intent)
    }
}

fun Context.isIgnoringBatteryOptimizations(): Boolean {
    val powerManager = this.getSystemService(Context.POWER_SERVICE) as PowerManager
    return powerManager.isIgnoringBatteryOptimizations(this.packageName)
}

fun Context.shouldAskForAlarmPermission(): Boolean {
    val alarmManager: AlarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
    return (
        (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU
        ) &&
            !alarmManager.canScheduleExactAlarms()
    )
}

fun Context.areNotificationsEnabled(): Boolean = NotificationManagerCompat.from(this).areNotificationsEnabled()

fun Context.openUrl(url: String) {
    val intent =
        Intent(Intent.ACTION_VIEW).apply {
            data = url.toUri()
        }
    startActivity(intent)
}

fun Context.installIsOlderThan10Days(): Boolean {
    val installTime = packageManager.getPackageInfo(packageName, 0).firstInstallTime
    return System.currentTimeMillis() - installTime > 10.days.inWholeMilliseconds
}

fun Context.takePersistableUriPermission(uri: Uri) {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    contentResolver.takePersistableUriPermission(uri, flags)
}

fun Context.releasePersistableUriPermission(uri: Uri) {
    contentResolver.releasePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
}

fun Context.isUriPersisted(uri: Uri): Boolean = contentResolver.persistedUriPermissions.any { it.uri == uri }
