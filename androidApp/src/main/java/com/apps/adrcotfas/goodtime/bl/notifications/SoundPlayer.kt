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
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.SoundData
import com.apps.adrcotfas.goodtime.settings.notifications.toSoundData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.Closeable
import java.lang.reflect.Method

/**
 * Represents the configuration state of the sound player.
 */
private data class SoundPlayerState(
    /** Sound configuration for work/focus timer completion */
    val workRingTone: SoundData = SoundData(),
    /** Sound configuration for break timer completion */
    val breakRingTone: SoundData = SoundData(),
    /** Whether sounds should loop until manually stopped */
    val loop: Boolean = false,
    /** Whether to override system sound profile settings */
    val overrideSoundProfile: Boolean = false,
)

class SoundPlayer(
    private val context: Context,
    ioScope: CoroutineScope,
    private val playerScope: CoroutineScope,
    private val settingsRepo: SettingsRepository,
    private val logger: Logger,
) : Closeable {
    companion object {
        private const val SET_LOOPING_METHOD_NAME = "setLooping"
    }

    private var job: Job? = null
    private val playbackMutex = Mutex()

    @Volatile
    private var state = SoundPlayerState()

    @Volatile
    private var currentRingtone: Ringtone? = null

    private lateinit var setLoopingMethod: Method

    init {
        try {
            setLoopingMethod =
                Ringtone::class.java.getDeclaredMethod(
                    SET_LOOPING_METHOD_NAME,
                    Boolean::class.javaPrimitiveType,
                )
        } catch (e: NoSuchMethodException) {
            logger.e(e) { "Failed to get method $SET_LOOPING_METHOD_NAME" }
        }
        ioScope.launch {
            settingsRepo.settings.collect { settings ->
                state =
                    state.copy(
                        workRingTone = toSoundData(settings.workFinishedSound),
                        breakRingTone = toSoundData(settings.breakFinishedSound),
                        overrideSoundProfile = settings.overrideSoundProfile,
                        loop = settings.insistentNotification
                    )
            }
        }
    }

    /**
     * Plays the appropriate sound for the given timer type.
     * Uses the configured sound settings for work vs break timers.
     *
     * @param timerType The type of timer that finished (FOCUS, BREAK, or LONG_BREAK)
     */
    fun play(timerType: TimerType) {
        val soundData =
            when (timerType) {
                TimerType.FOCUS -> state.workRingTone
                TimerType.BREAK, TimerType.LONG_BREAK -> state.breakRingTone
            }
        play(soundData, state.loop)
    }

    /**
     * Plays a specific sound with custom configuration.
     *
     * @param soundData The sound configuration to play
     * @param loop Whether the sound should loop until manually stopped
     * @param forceSound Whether to force sound playback regardless of system sound profile
     */
    fun play(
        soundData: SoundData,
        loop: Boolean = false,
        forceSound: Boolean = false,
    ) {
        playerScope.launch {
            job?.cancelAndJoin()
            job =
                playerScope.launch {
                    stopInternal()
                    playInternal(soundData, loop, forceSound)
                }
        }
    }

    private suspend fun playInternal(
        soundData: SoundData,
        loop: Boolean,
        forceSound: Boolean,
    ) = playbackMutex.withLock {
        if (soundData.isSilent) return@withLock
        val uri =
            soundData.uriString.let {
                if (it.isEmpty()) {
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                } else {
                    it.toUri()
                }
            }

        val audioManager = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager)

        val usage =
            if (areHeadphonesPluggedIn(audioManager)) {
                AudioAttributes.USAGE_MEDIA
            } else if (state.overrideSoundProfile || forceSound) {
                AudioAttributes.USAGE_ALARM
            } else {
                AudioAttributes.USAGE_NOTIFICATION
            }

        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.audioAttributes =
            AudioAttributes
                .Builder()
                .setUsage(usage)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

        // Update currentRingtone
        currentRingtone = ringtone

        try {
            if (loop) {
                setLoopingMethod.invoke(ringtone, true)
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to set looping" }
        }
        try {
            ringtone?.play()
        } catch (e: Exception) {
            logger.e(e) { "Failed to play ringtone" }
        }
    }

    /**
     * Stops any currently playing sound.
     * This method is safe to call even if no sound is currently playing.
     */
    fun stop() {
        playerScope.launch {
            job?.cancelAndJoin()
            job =
                playerScope.launch {
                    stopInternal()
                }
        }
    }

    private fun stopInternal() {
        currentRingtone?.let {
            if (it.isPlaying) {
                try {
                    it.stop()
                } catch (e: Exception) {
                    logger.e(e) { "Failed to stop ringtone" }
                }
            }
        }
        currentRingtone = null
    }

    private fun areHeadphonesPluggedIn(audioManager: AudioManager): Boolean {
        val audioDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val list =
            mutableListOf(
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
                AudioDeviceInfo.TYPE_WIRED_HEADSET,
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_BLUETOOTH_SCO,
            )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            list.add(AudioDeviceInfo.TYPE_BLE_SPEAKER)
        }
        return audioDevices.any { deviceInfo ->
            list.contains(deviceInfo.type)
        }
    }

    override fun close() {
        playerScope.launch {
            job?.cancelAndJoin()
            stopInternal()
            currentRingtone = null
        }
    }
}
