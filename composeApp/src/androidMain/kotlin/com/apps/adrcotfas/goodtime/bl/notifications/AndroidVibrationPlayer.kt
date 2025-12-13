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

import android.content.Context
import android.content.Context.VIBRATOR_SERVICE
import android.media.AudioAttributes
import android.os.Build
import android.os.VibrationAttributes
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AndroidVibrationPlayer(
    context: Context,
    private val settingsRepo: SettingsRepository,
    private val playerScope: CoroutineScope,
    ioScope: CoroutineScope,
) : VibrationPlayer {
    private var data: VibrationData = VibrationData(3, false)
    private var job: Job? = null

    init {
        ioScope.launch {
            settingsRepo.settings
                .map {
                    VibrationData(
                        it.vibrationStrength,
                        it.insistentNotification,
                    )
                }.collect {
                    data = it
                }
        }
    }

    private val vibrator =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

    override fun start() {
        start(data)
    }

    override fun stop() {
        playerScope.launch {
            job?.cancelAndJoin()
            job =
                playerScope.launch {
                    vibrator.cancel()
                }
        }
    }

    override fun start(strength: Int) {
        start(VibrationData(strength, false))
    }

    private fun start(data: VibrationData) {
        playerScope.launch {
            job?.cancelAndJoin()
            job =
                playerScope.launch innerLaunch@{
                    vibrator.cancel()
                    val (strength, loop) = data
                    val repeat = if (loop) 1 else -1
                    if (strength == 0 || !vibrator.hasVibrator()) {
                        return@innerLaunch
                    }
                    val pattern =
                        when (strength) {
                            1 -> {
                                longArrayOf(0, 100, 2000)
                            }

                            2 -> {
                                longArrayOf(0, 100, 50, 100, 1000)
                            }

                            3 -> {
                                longArrayOf(0, 200, 50, 200, 1000)
                            }

                            4 -> {
                                longArrayOf(0, 400, 100, 400, 1000)
                            }

                            5 -> {
                                longArrayOf(0, 400, 100, 400, 100, 400, 1000)
                            }

                            else -> longArrayOf()
                        }
                    // add a small delay to avoid vibration being ignored when exiting DnD mode
                    delay(100)

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val vibrationAttributes =
                            VibrationAttributes
                                .Builder()
                                .setUsage(VibrationAttributes.USAGE_ALARM)
                                .build()
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(pattern, repeat),
                            vibrationAttributes,
                        )
                    } else {
                        val audioAttributes =
                            AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(
                            VibrationEffect.createWaveform(pattern, repeat),
                            audioAttributes,
                        )
                    }
                }
        }
    }
}
