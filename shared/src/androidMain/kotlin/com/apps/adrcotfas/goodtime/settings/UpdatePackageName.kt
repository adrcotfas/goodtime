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

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/** Marker class whose fully-qualified name matches the <activity-alias> in the manifest. */
class GoodtimeLauncherAlias

/**
 * Older versions let users rename the launcher to "Productivity", which disabled
 * [GoodtimeLauncherAlias] at runtime. That option (and its alias) is gone now, so re-enable the
 * Goodtime launcher for anyone still on the old override — otherwise they'd have no launcher icon.
 */
// runs on every boot/update forever; delete this whole path once "Productivity"
// installs have aged out (safe from ~mid-2027).
fun restoreGoodtimeLauncherIfNeeded(context: Context) {
    val pm = context.packageManager
    val alias = ComponentName(context, GoodtimeLauncherAlias::class.java)
    if (pm.getComponentEnabledSetting(alias) == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
        pm.setComponentEnabledSetting(
            alias,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP,
        )
    }
}
