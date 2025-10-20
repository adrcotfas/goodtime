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
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity

class GoodtimeLauncherAlias

class ProductivityLauncherAlias

fun updateLauncherName(
    packageManager: PackageManager,
    dynamicLauncherActivity: ComponentActivity,
    index: Int,
) {
    val (aliasToEnable, aliasToDisable) =
        if (index == 0) {
            GoodtimeLauncherAlias::class.java to ProductivityLauncherAlias::class.java
        } else {
            ProductivityLauncherAlias::class.java to GoodtimeLauncherAlias::class.java
        }

    packageManager.setComponentEnabledSetting(
        ComponentName(
            dynamicLauncherActivity,
            aliasToEnable,
        ),
        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
        PackageManager.DONT_KILL_APP,
    )
    packageManager.setComponentEnabledSetting(
        ComponentName(
            dynamicLauncherActivity,
            aliasToDisable,
        ),
        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
        PackageManager.DONT_KILL_APP,
    )
}
