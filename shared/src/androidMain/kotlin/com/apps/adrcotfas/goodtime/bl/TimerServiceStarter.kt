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
import android.content.Intent
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.TimerService.Companion.Action

class TimerServiceStarter(
    private val context: Context,
    private val log: Logger,
) : EventListener {
    override fun onEvent(event: Event) {
        when (event) {
            is Event.Start, is Event.Pause, is Event.AddOneMinute, is Event.UpdateActiveLabel -> startService()

            is Event.Reset -> startService(Action.Reset)

            // on autostart the service stays foreground and the next Start updates it
            is Event.Finished -> if (!event.autostartNextSession) startService(Action.Finished)

            else -> {}
        }
    }

    fun startService(action: Action = Action.StartOrUpdate) {
        start(
            TimerService.createIntentWithAction(
                context,
                action,
            ),
            action,
        )
    }

    private fun start(
        intent: Intent,
        action: Action,
    ) {
        try {
            context.startService(intent)
        } catch (e: IllegalStateException) {
            log.w(e) { "Could not start the service for $action" }
        }
    }
}
