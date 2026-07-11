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
package com.apps.adrcotfas.goodtime.appkilltest

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * End-to-end tests for recovering a running timer after the OS abruptly kills the app
 * (Samsung-style FGS kill). The test process instruments itself, so it stays alive across
 * the kill and drives everything as a user would, through UiAutomator and shell.
 *
 * Runs on a device/emulator only:
 * ```
 * ./gradlew :appKillTest:pixel6Api35GoogleDebugAndroidTest      # managed emulator
 * ./gradlew :appKillTest:connectedGoogleDebugAndroidTest        # attached device
 * ```
 */
@RunWith(AndroidJUnit4::class)
class KillRecoveryTest {
    private val driver = GoodtimeDriver()

    @Before
    fun freshStart() {
        // wipe data; each test then seeds and launches (some need auto-start enabled)
        driver.shell("pm clear ${GoodtimeDriver.PACKAGE}")
        // pm clear revokes runtime permissions; without POST_NOTIFICATIONS the foreground
        // service runs but posts nothing, which is exactly what these tests assert on
        driver.shell("pm grant ${GoodtimeDriver.PACKAGE} android.permission.POST_NOTIFICATIONS")
    }

    /**
     * Core regression: killing a running countdown must not lose it. After the sticky
     * service restart, the in-progress notification is re-adopted and the alarm re-armed.
     * The old code left the notification orphaned and the alarm gone.
     */
    @Test
    fun killMidSession_reArmsAlarmAndKeepsNotification() {
        prepare()
        startTimer()
        driver.awaitNotification(FOCUS_IN_PROGRESS)
        // background the app (the alarm is only armed off the foreground), like the real
        // scenario: running session, screen off, then the OS kills the process
        driver.pressHome()
        driver.awaitAlarmArmed("alarm should be armed once backgrounded")

        val pid = driver.pid()
        driver.killApp()
        driver.waitForProcessRestart(pid)

        assertTrue(
            "in-progress notification should be re-adopted after restart",
            driver.notificationVisible(FOCUS_IN_PROGRESS),
        )
        driver.awaitAlarmArmed("alarm should be re-armed after restart")
    }

    /**
     * The countdown expires while the app is dead. The alarm fires into a cold process,
     * which must finish the session and post the finished notification.
     */
    @Test
    fun killMidSession_alarmStillFiresAndFinishes() {
        prepare()
        startTimer()
        driver.awaitNotification(FOCUS_IN_PROGRESS)
        driver.pressHome()

        val pid = driver.pid()
        driver.killApp()
        driver.waitForProcessRestart(pid)

        driver.awaitNotification(FOCUS_COMPLETE)
    }

    /**
     * The session finishes (overtime phase) and then the app is killed before the user
     * reacts. Reopening must restore the finished state, not a blank reset timer.
     */
    @Test
    fun killDuringOvertime_restoresFinishedState() {
        prepare()
        startTimer()
        driver.pressHome()
        driver.awaitNotification(FOCUS_COMPLETE)

        // in the overtime phase the service already stopped (START_NOT_STICKY), so the
        // process does not auto-restart; recovery happens when the user reopens the app
        driver.killApp()
        driver.launchApp()

        assertTrue(
            "the finished session dialog should be restored",
            driver.hasText(FOCUS_COMPLETE),
        )
    }

    /**
     * Tapping a finished-notification action after an overtime kill cold-starts the
     * process; the action must still take effect (here: start the break).
     */
    @Test
    fun notificationAction_afterOvertimeKill_startsBreak() {
        prepare()
        startTimer()
        driver.pressHome()
        driver.awaitNotification(FOCUS_COMPLETE)

        // no auto-restart in overtime; tapping the notification action cold-starts the
        // process via its PendingIntent and must still start the break
        driver.killApp()
        driver.tapNotificationAction(START_BREAK)

        driver.awaitNotification(BREAK_IN_PROGRESS)
    }

    /**
     * Auto-start break is on and the process is killed mid-focus. The countdown expires while
     * the resurrected process is backgrounded; the finish must chain into auto-starting the
     * break rather than stopping at the finished dialog.
     */
    @Test
    fun killMidFocus_withAutostart_startsBreakAutomatically() {
        prepare(autostart = true)
        startTimer()
        driver.awaitNotification(FOCUS_IN_PROGRESS)
        driver.pressHome()

        val pid = driver.pid()
        driver.killApp()
        driver.waitForProcessRestart(pid)

        // alarm fires in the cold process -> finish -> next(AUTO) starts the break
        driver.awaitNotification(BREAK_IN_PROGRESS)
    }

    /**
     * After the break auto-starts, killing during it must restore the running break — not the
     * previous finished focus. Guards that the auto-started session is what gets persisted
     * (the Finished event is intentionally not persisted when auto-start follows).
     */
    @Test
    fun killDuringAutostartedBreak_restoresRunningBreak() {
        prepare(autostart = true)
        startTimer()
        driver.pressHome()
        driver.awaitNotification(BREAK_IN_PROGRESS) // focus finished, break auto-started

        val pid = driver.pid()
        driver.killApp()
        driver.waitForProcessRestart(pid)

        assertTrue(
            "the running break should be re-adopted after restart",
            driver.notificationVisible(BREAK_IN_PROGRESS),
        )
    }

    // --- flow helpers ------------------------------------------------------

    /** Seeds a known state (optionally with auto-start break) and launches the app. */
    private fun prepare(autostart: Boolean = false) {
        driver.seedState(autostart = autostart)
        driver.launchApp()
    }

    private fun startTimer() {
        // tapping the center of the timer screen starts a reset countdown
        driver.device.click(driver.device.displayWidth / 2, driver.device.displayHeight / 2)
        driver.device.waitForIdle()
    }

    companion object {
        private const val FOCUS_IN_PROGRESS = "Focus session in progress"
        private const val FOCUS_COMPLETE = "Focus complete"
        private const val BREAK_IN_PROGRESS = "Break in progress"
        private const val START_BREAK = "Start break"
    }
}
