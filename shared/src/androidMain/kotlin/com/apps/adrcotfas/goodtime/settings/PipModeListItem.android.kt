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
package com.apps.adrcotfas.goodtime.settings

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Process
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.apps.adrcotfas.goodtime.ui.CheckboxListItem
import com.apps.adrcotfas.goodtime.ui.LockedCheckboxListItem
import goodtime_productivity.shared.generated.resources.Res
import goodtime_productivity.shared.generated.resources.settings_click_to_grant_permission
import goodtime_productivity.shared.generated.resources.settings_pip_mode_desc
import goodtime_productivity.shared.generated.resources.settings_pip_mode_title
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun PipModeListItem(
    isPro: Boolean,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val isPipSupported =
        remember {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        }
    if (!isPipSupported) return

    if (!isPro) {
        LockedCheckboxListItem(
            title = stringResource(Res.string.settings_pip_mode_title),
            subtitle = stringResource(Res.string.settings_pip_mode_desc),
            checked = false,
            enabled = false,
        ) {}
        return
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()
    var isPipAllowedBySystem by remember { mutableStateOf(context.isPipAllowedBySystem()) }
    var wasPermissionRequested by remember { mutableStateOf(false) }

    LaunchedEffect(lifecycleState) {
        if (lifecycleState == Lifecycle.State.RESUMED) {
            isPipAllowedBySystem = context.isPipAllowedBySystem()
            if (wasPermissionRequested && isPipAllowedBySystem) {
                onCheckedChange(true)
            }
            if (!isPipAllowedBySystem) {
                onCheckedChange(false)
            }
        }
    }

    CheckboxListItem(
        title = stringResource(Res.string.settings_pip_mode_title),
        subtitle =
        if (isPipAllowedBySystem) {
            stringResource(Res.string.settings_pip_mode_desc)
        } else {
            stringResource(Res.string.settings_click_to_grant_permission)
        },
        checked = checked,
    ) {
        if (isPipAllowedBySystem) {
            onCheckedChange(it)
        } else {
            wasPermissionRequested = true
            openPipSystemSettings(context)
        }
    }
}

private fun Context.isPipAllowedBySystem(): Boolean {
    val appOps = getSystemService(AppOpsManager::class.java)
    val mode =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                Process.myUid(),
                packageName,
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_PICTURE_IN_PICTURE,
                Process.myUid(),
                packageName,
            )
        }
    return mode == AppOpsManager.MODE_ALLOWED
}

private fun openPipSystemSettings(context: Context) {
    val packageUri = Uri.fromParts("package", context.packageName, null)
    runCatching {
        context.startActivity(Intent("android.settings.PICTURE_IN_PICTURE_SETTINGS", packageUri))
    }.recoverCatching {
        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri))
    }
}
