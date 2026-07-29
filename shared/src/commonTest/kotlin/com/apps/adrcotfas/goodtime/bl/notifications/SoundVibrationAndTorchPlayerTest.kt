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
package com.apps.adrcotfas.goodtime.bl.notifications

import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import com.apps.adrcotfas.goodtime.bl.Event
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.data.settings.SoundData
import com.apps.adrcotfas.goodtime.fakes.FakePlatformConfiguration
import com.apps.adrcotfas.goodtime.fakes.FakeTimeProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SoundVibrationAndTorchPlayerTest {
    private class RecordingSoundPlayer : SoundPlayer {
        var playCount = 0
        var stopCount = 0

        override fun play(timerType: TimerType) {
            playCount++
        }

        override fun play(
            soundData: SoundData,
            loop: Boolean,
            volume: Int?,
        ) {
            playCount++
        }

        override fun stop() {
            stopCount++
        }

        override fun close() {}
    }

    private class RecordingVibrationPlayer : VibrationPlayer {
        var startCount = 0
        var stopCount = 0

        override fun start() {
            startCount++
        }

        override fun start(strength: Int) {
            startCount++
        }

        override fun stop() {
            stopCount++
        }
    }

    private class RecordingTorchManager : TorchManager {
        var startCount = 0
        var stopCount = 0

        override fun isTorchAvailable() = true

        override fun start() {
            startCount++
        }

        override fun stop() {
            stopCount++
        }
    }

    private val timeProvider = FakeTimeProvider()
    private lateinit var soundPlayer: RecordingSoundPlayer
    private lateinit var vibrationPlayer: RecordingVibrationPlayer
    private lateinit var torchManager: RecordingTorchManager

    @BeforeTest
    fun setup() {
        soundPlayer = RecordingSoundPlayer()
        vibrationPlayer = RecordingVibrationPlayer()
        torchManager = RecordingTorchManager()
    }

    private fun player(isAndroid: Boolean) = SoundVibrationAndTorchPlayer(
        soundPlayer = soundPlayer,
        vibrationPlayer = vibrationPlayer,
        torchManager = torchManager,
        timeProvider = timeProvider,
        logger = Logger(StaticConfig()),
        platformConfiguration = FakePlatformConfiguration(isAndroid = isAndroid),
    )

    @Test
    fun `finished on Android always plays`() {
        val player = player(isAndroid = true)
        timeProvider.elapsedRealtime = 500_000

        player.onEvent(Event.SendToBackground(isTimerRunning = true, endTime = 100_000))
        player.onEvent(Event.Finished(type = TimerType.FOCUS)) // long after endTime

        assertEquals(1, soundPlayer.playCount)
        assertEquals(1, vibrationPlayer.startCount)
        assertEquals(1, torchManager.startCount)
    }

    @Test
    fun `finished on iOS long after the expected end does not play`() {
        val player = player(isAndroid = false)
        player.onEvent(Event.SendToBackground(isTimerRunning = true, endTime = 100_000))
        // user returns to the app well after the notification already fired
        timeProvider.elapsedRealtime = 100_000 + 5_000

        player.onEvent(Event.Finished(type = TimerType.FOCUS))

        assertEquals(0, soundPlayer.playCount)
        assertEquals(0, vibrationPlayer.startCount)
        assertEquals(0, torchManager.startCount)
    }

    @Test
    fun `finished on iOS within a second of the expected end plays`() {
        val player = player(isAndroid = false)
        player.onEvent(Event.SendToBackground(isTimerRunning = true, endTime = 100_000))
        timeProvider.elapsedRealtime = 100_000 + 500

        player.onEvent(Event.Finished(type = TimerType.FOCUS))

        assertEquals(1, soundPlayer.playCount)
        assertEquals(1, vibrationPlayer.startCount)
        assertEquals(1, torchManager.startCount)
    }

    @Test
    fun `finished on iOS with the app kept in foreground plays`() {
        val player = player(isAndroid = false)
        timeProvider.elapsedRealtime = 100_000

        // no SendToBackground happened, endTime stays 0
        player.onEvent(Event.Finished(type = TimerType.FOCUS))

        assertEquals(1, soundPlayer.playCount)
    }

    @Test
    fun `manual start stops any ongoing playback but auto start does not`() {
        val player = player(isAndroid = true)

        player.onEvent(Event.Start(autoStarted = false))
        assertEquals(1, soundPlayer.stopCount)
        assertEquals(1, vibrationPlayer.stopCount)
        assertEquals(1, torchManager.stopCount)

        player.onEvent(Event.Start(autoStarted = true))
        assertEquals(1, soundPlayer.stopCount)
    }

    @Test
    fun `reset stops playback`() {
        val player = player(isAndroid = true)

        player.onEvent(Event.Reset)

        assertEquals(1, soundPlayer.stopCount)
        assertEquals(1, vibrationPlayer.stopCount)
        assertEquals(1, torchManager.stopCount)
    }
}
