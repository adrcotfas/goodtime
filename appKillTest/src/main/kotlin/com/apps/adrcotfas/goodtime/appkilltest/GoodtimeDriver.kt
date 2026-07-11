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

import android.os.ParcelFileDescriptor
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue

/**
 * Black-box control of the app for the kill-recovery tests. Everything goes through
 * UiAutomator + shell; this code runs in the test process, so killing the app leaves it alive.
 */
class GoodtimeDriver {
    val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    fun shell(command: String): String = device.executeShellCommand(command).trim()

    fun pid(): String = shell("pidof $PACKAGE")

    fun isProcessAlive(): Boolean = pid().isNotEmpty()

    /** True while the app has an exact alarm scheduled for the countdown end. */
    fun alarmArmed(): Boolean = shell("dumpsys alarm").contains("$PACKAGE/.bl.AlarmReceiver")

    fun awaitAlarmArmed(message: String) {
        assertTrue(message, waitFor(UI_TIMEOUT_MS) { alarmArmed() })
    }

    /**
     * SIGKILL the app the way an aggressive OEM would — abruptly, no lifecycle callbacks.
     * `am kill` refuses foreground-service processes, so signal it as the app's own uid.
     */
    fun killApp() {
        val pid = pid()
        assertTrue("app is not running, cannot kill it", pid.isNotEmpty())
        shell("run-as $PACKAGE kill -9 $pid")
        assertTrue(
            "process survived kill -9",
            waitFor(KILL_TIMEOUT_MS) { !isProcessAlive() },
        )
    }

    /**
     * Writes the bundled fixtures straight into the (freshly cleared) app's data dir, so it
     * starts past onboarding/the tutorial with a 1-minute focus profile — no setup UI to drive.
     * The app must not be running. Fixtures are regenerated with appKillTest/seed/README.md.
     * @param autostart use the settings fixture with "auto start break" enabled.
     */
    fun seedState(autostart: Boolean = false) {
        val settings =
            if (autostart) {
                "seed/productivity_settings_autostart.preferences_pb"
            } else {
                "seed/productivity_settings.preferences_pb"
            }
        pushAsset(settings, "files/productivity_settings.preferences_pb")
        pushAsset("seed/goodtime-db", "databases/goodtime-db")
    }

    /**
     * Streams a test-APK asset straight into the app's private dir. executeShellCommand runs
     * via Runtime.exec (no shell), so pipes/redirection don't work — instead stream the raw
     * bytes to `dd`, which takes its output path as an argument.
     */
    private fun pushAsset(assetPath: String, destRelPath: String) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bytes = instrumentation.context.assets.open(assetPath).use { it.readBytes() }

        shell("run-as $PACKAGE mkdir -p ${destRelPath.substringBeforeLast('/')}")

        // fds[0] = command stdout (read), fds[1] = command stdin (write)
        val fds = instrumentation.uiAutomation.executeShellCommandRw("run-as $PACKAGE dd of=$destRelPath")
        ParcelFileDescriptor.AutoCloseOutputStream(fds[1]).use { it.write(bytes) }
        ParcelFileDescriptor.AutoCloseInputStream(fds[0]).use { it.readBytes() } // wait for dd to finish
    }

    fun launchApp() {
        shell("am start -n $PACKAGE/.settings.GoodtimeLauncherAlias")
        device.wait(Until.hasObject(By.pkg(PACKAGE).depth(0)), UI_TIMEOUT_MS)
    }

    fun pressHome() {
        device.pressHome()
        device.waitForIdle()
    }

    /** Waits for a fresh process to exist (e.g. after a sticky service restart). */
    fun waitForProcessRestart(previousPid: String): String {
        assertTrue(
            "process did not restart after kill",
            waitFor(RESTART_TIMEOUT_MS) {
                val p = pid()
                p.isNotEmpty() && p != previousPid
            },
        )
        return pid()
    }

    fun openNotifications() {
        device.openNotification()
        device.wait(Until.hasObject(By.textContains("")), UI_TIMEOUT_MS)
    }

    fun notificationVisible(text: String): Boolean {
        openNotifications()
        val found = device.wait(Until.hasObject(By.textContains(text)), UI_TIMEOUT_MS) ?: false
        device.pressBack() // close the shade
        return found
    }

    fun awaitNotification(text: String) {
        assertTrue(
            "notification '$text' never appeared",
            waitFor(NOTIFICATION_TIMEOUT_MS) { notificationVisible(text) },
        )
    }

    fun tapNotificationAction(text: String) {
        openNotifications()
        val action =
            device.wait(Until.findObject(By.textContains(text)), UI_TIMEOUT_MS)
        assertNotNull("notification action '$text' not found", action)
        action.click()
        device.waitForIdle()
    }

    fun hasText(text: String): Boolean = device.wait(Until.hasObject(By.textContains(text)), UI_TIMEOUT_MS) ?: false

    private fun waitFor(timeoutMs: Long, condition: () -> Boolean): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(POLL_MS)
        }
        return condition()
    }

    companion object {
        const val PACKAGE = "com.apps.adrcotfas.goodtime"

        private const val POLL_MS = 500L
        private const val UI_TIMEOUT_MS = 5_000L
        private const val KILL_TIMEOUT_MS = 5_000L
        private const val RESTART_TIMEOUT_MS = 15_000L

        private const val NOTIFICATION_TIMEOUT_MS = 90_000L
    }
}
