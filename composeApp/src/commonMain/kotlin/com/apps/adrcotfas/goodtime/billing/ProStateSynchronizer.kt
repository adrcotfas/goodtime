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
package com.apps.adrcotfas.goodtime.billing

import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.TimerStyleData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.floor

internal const val DEFAULT_PRO_ENTITLEMENT_ID: String = "PREMIUM"

/**
 * Shared "pro entitlement changed" logic.
 *
 * The entitlement check itself is platform-specific; callers simply feed `hasPro`.
 */
internal class ProStateSynchronizer(
    private val settingsRepository: SettingsRepository,
    private val dataRepository: LocalDataRepository,
    private val ioScope: CoroutineScope,
    private val log: Logger,
    private val onProRevoked: suspend () -> Unit = {},
    private val onProGranted: suspend () -> Unit = {},
) {
    fun onHasProChanged(hasPro: Boolean) {
        ioScope.launch {
            val wasPro = settingsRepository.settings.first().isPro
            if (wasPro && !hasPro) {
                log.i { "Pro entitlement revoked; resetting Pro-only settings" }
                resetPreferencesOnProRevoked()
            }
            settingsRepository.setPro(hasPro)
            if (!wasPro && hasPro) {
                onProGranted()
            }
        }
    }

    private suspend fun resetPreferencesOnProRevoked() {
        resetTimerStyle()
        with(settingsRepository) {
            updateUiSettings {
                it.copy(fullscreenMode = false, screensaverMode = false)
            }
            setEnableTorch(false)
            setEnableFlashScreen(false)
            setInsistentNotification(false)
            activateDefaultLabel()
        }
        // Cancel auto-backups on refunds/revokes.
        val backupSettings = settingsRepository.settings.first().backupSettings
        if (backupSettings.autoBackupEnabled || backupSettings.cloudAutoBackupEnabled) {
            settingsRepository.setBackupSettings(
                backupSettings.copy(
                    autoBackupEnabled = false,
                    cloudAutoBackupEnabled = false,
                ),
            )
        }
        onProRevoked()
        dataRepository.archiveAllButDefault()
    }

    private suspend fun resetTimerStyle() {
        val oldTimerStyle = settingsRepository.settings.first().timerStyle
        val newTimerStyle =
            TimerStyleData(
                minSize = oldTimerStyle.minSize,
                maxSize = oldTimerStyle.maxSize,
                fontSize = floor(oldTimerStyle.maxSize * 0.9f),
                currentScreenWidth = oldTimerStyle.currentScreenWidth,
            )
        settingsRepository.updateTimerStyle { newTimerStyle }
    }
}
