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

import android.content.Context
import androidx.work.WorkerParameters
import co.touchlab.kermit.LoggerConfig
import com.apps.adrcotfas.goodtime.bl.AlarmManagerHandler
import com.apps.adrcotfas.goodtime.bl.DndModeManager
import com.apps.adrcotfas.goodtime.bl.EventListener
import com.apps.adrcotfas.goodtime.bl.TimerServiceStarter
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

class KoinModuleVerifyTest {
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun `android module graph resolves`() {
        module {
            includes(
                coroutineScopeModule,
                platformModule,
                coreModule(isDebug = false),
                localDataModule,
                coreBackupModule,
                androidCommonBackupModule,
                timerManagerModule,
                viewModelModule,
                mainModule,
            )
        }.verify(
            extraTypes =
            listOf(
                Context::class,
                EventListener::class,
                DndModeManager::class,
                AlarmManagerHandler::class,
                TimerServiceStarter::class,
                LoggerConfig::class,
                WorkerParameters::class,
            ),
        )
    }
}
