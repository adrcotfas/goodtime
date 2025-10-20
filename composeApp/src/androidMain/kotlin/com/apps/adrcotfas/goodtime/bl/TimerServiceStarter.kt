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

import android.content.Context
import com.apps.adrcotfas.goodtime.bl.TimerService.Companion.Action

class TimerServiceStarter(
    private val context: Context,
) : EventListener {
    override fun onEvent(event: Event) {
        when (event) {
            is Event.Start, is Event.Pause, is Event.AddOneMinute, is Event.UpdateActiveLabel -> startService()
            is Event.Reset -> startService(Action.Reset)
            is Event.Finished -> startServiceWithFinished(event.autostartNextSession)
            else -> {}
        }
    }

    private fun startService(action: Action = Action.StartOrUpdate) {
        context.startService(
            TimerService.createIntentWithAction(
                context,
                action,
            ),
        )
    }

    private fun startServiceWithFinished(autoStart: Boolean) {
        context.startService(
            TimerService.createFinishEvent(
                context,
                autoStart,
            ),
        )
    }
}
