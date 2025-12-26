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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

actual class PurchaseManager actual constructor(
    private val settingsRepository: SettingsRepository,
    private val dataRepository: LocalDataRepository,
    private val ioScope: CoroutineScope,
    private val log: Logger,
) {
    private var started = false

    actual fun start() {
        if (started) return
        started = true

        // F-Droid distribution does not support in-app purchases.
        // Keep existing behavior: treat users as Pro.
        log.i { "F-Droid build: enabling Pro (no billing backend)" }
        ioScope.launch {
            settingsRepository.setPro(true)
        }
    }
}
