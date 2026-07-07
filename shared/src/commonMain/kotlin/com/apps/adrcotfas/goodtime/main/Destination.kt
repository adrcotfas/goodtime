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
package com.apps.adrcotfas.goodtime.main

import kotlinx.serialization.Serializable

@Serializable
data object OnboardingDest

@Serializable
data object MainDest

val MainDest.route: String
    get() = "com.apps.adrcotfas.goodtime.main.MainDest"

@Serializable
data object LabelsDest

@Serializable
data class AddEditLabelDest(
    val name: String,
)

@Serializable
data object ArchivedLabelsDest

@Serializable
data object StatsDest

@Serializable
data object SettingsDest

@Serializable
data object UserInterfaceDest

@Serializable
data object TimerDurationsDest

@Serializable
data object NotificationSettingsDest

@Serializable
data object BackupDest

@Serializable
data object AboutDest

@Serializable
data object LicensesDest

@Serializable
data object AcknowledgementsDest

@Serializable
data object ProDest
