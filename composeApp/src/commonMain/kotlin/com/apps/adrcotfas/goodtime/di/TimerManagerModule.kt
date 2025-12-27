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
package com.apps.adrcotfas.goodtime.di

import com.apps.adrcotfas.goodtime.bl.EventListener
import com.apps.adrcotfas.goodtime.bl.FinishedSessionsHandler
import com.apps.adrcotfas.goodtime.bl.TimeProvider
import com.apps.adrcotfas.goodtime.bl.TimerForegroundMonitor
import com.apps.adrcotfas.goodtime.bl.TimerManager
import com.apps.adrcotfas.goodtime.data.local.LocalDataRepository
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val timerManagerModule =
    module {
        single<TimerManager> {
            TimerManager(
                get<LocalDataRepository>(),
                get<SettingsRepository>(),
                get<List<EventListener>>(),
                get<TimeProvider>(),
                get<FinishedSessionsHandler>(),
                getWith("TimerManager"),
                coroutineScope = get(named(IO_SCOPE)),
            )
        }

        single {
            TimerForegroundMonitor(
                timerManager = get(),
                timeProvider = get(),
            )
        }
    }
