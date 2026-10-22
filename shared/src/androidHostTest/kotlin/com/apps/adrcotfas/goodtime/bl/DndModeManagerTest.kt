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

import com.apps.adrcotfas.goodtime.data.settings.AppSettings
import com.apps.adrcotfas.goodtime.data.settings.UiSettings
import com.apps.adrcotfas.goodtime.fakes.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DndModeManagerTest {
    @Test
    fun `auto-starting another focus session preserves DND ownership until final exit`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val settingsRepository = settingsWithDndDuringWork()
            val controller = FakeDndModeController()
            val manager =
                DndModeManager(
                    dndModeController = controller,
                    settingsRepository = settingsRepository,
                    coroutineScope = this,
                )

            manager.onEvent(Event.Start())
            runCurrent()
            advanceTimeBy(2_500)
            runCurrent()
            assertTrue(controller.isEnabled)

            // An auto-started focus session arrives before the asynchronous exit runs.
            manager.onEvent(Event.Finished(type = TimerType.FOCUS, autostartNextSession = true))
            manager.onEvent(Event.Start(autoStarted = true))
            runCurrent()

            manager.onEvent(Event.Finished(type = TimerType.FOCUS))
            runCurrent()

            assertFalse(controller.isEnabled)
            assertEquals(listOf(true, false), controller.transitions)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `turning off the setting during a session still restores app-managed DND`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val settingsRepository = settingsWithDndDuringWork()
            val controller = FakeDndModeController()
            val manager =
                DndModeManager(
                    dndModeController = controller,
                    settingsRepository = settingsRepository,
                    coroutineScope = this,
                )

            manager.onEvent(Event.Start())
            runCurrent()
            advanceTimeBy(2_500)
            runCurrent()
            assertTrue(controller.isEnabled)

            settingsRepository.updateUiSettings { it.copy(dndDuringWork = false) }
            manager.onEvent(Event.Finished(type = TimerType.FOCUS))
            runCurrent()

            assertFalse(controller.isEnabled)
            assertEquals(listOf(true, false), controller.transitions)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `DND already enabled by the user is preserved`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val controller = FakeDndModeController(isEnabled = true)
            val manager =
                DndModeManager(
                    dndModeController = controller,
                    settingsRepository = settingsWithDndDuringWork(),
                    coroutineScope = this,
                )

            manager.onEvent(Event.Start())
            runCurrent()
            manager.onEvent(Event.Finished(type = TimerType.FOCUS))
            runCurrent()

            assertTrue(controller.isEnabled)
            assertEquals(emptyList(), controller.transitions)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun settingsWithDndDuringWork() = FakeSettingsRepository(
        AppSettings(uiSettings = UiSettings(dndDuringWork = true)),
    )

    private class FakeDndModeController(
        var isEnabled: Boolean = false,
    ) : DndModeController {
        val transitions = mutableListOf<Boolean>()

        override fun isDndModeEnabled() = isEnabled

        override fun toggleDndMode(enabled: Boolean) {
            transitions += enabled
            isEnabled = enabled
        }
    }
}
