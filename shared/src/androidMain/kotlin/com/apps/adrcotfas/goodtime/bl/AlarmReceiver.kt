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
package com.apps.adrcotfas.goodtime.bl

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.time.Duration.Companion.seconds

class AlarmReceiver :
    BroadcastReceiver(),
    KoinComponent {
    private val timerManager: TimerManager by inject()

    @Suppress("DEPRECATION")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        powerManager
            .newWakeLock(
                PowerManager.ACQUIRE_CAUSES_WAKEUP
                    or PowerManager.ON_AFTER_RELEASE
                    or PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                "Goodtime:AlarmReceiver",
            ).apply {
                acquire(10.seconds.inWholeMilliseconds)
            }
        val isCountdown = timerManager.timerData.value.isCurrentSessionCountdown()
        if (isCountdown) {
            timerManager.finish()
        } else {
            // hard limit was reached for count-up
            timerManager.reset()
        }
    }
}
