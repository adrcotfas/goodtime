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
import com.apps.adrcotfas.goodtime.bl.TimerType
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import com.apps.adrcotfas.goodtime.data.settings.SoundData
import com.apps.adrcotfas.goodtime.settings.notifications.toSoundData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryAmbient
import platform.AVFAudio.AVAudioSessionCategoryOptionDuckOthers
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionPortBluetoothA2DP
import platform.AVFAudio.AVAudioSessionPortBluetoothHFP
import platform.AVFAudio.AVAudioSessionPortBluetoothLE
import platform.AVFAudio.AVAudioSessionPortHeadphones
import platform.AVFAudio.AVAudioSessionPortUSBAudio
import platform.AVFAudio.currentRoute
import platform.AVFAudio.setActive
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import kotlin.concurrent.Volatile

@OptIn(ExperimentalForeignApi::class)
class IosSoundPlayer(
    private val settingsRepo: SettingsRepository,
    private val logger: Logger,
    private val ioScope: CoroutineScope,
    private val playerScope: CoroutineScope,
) : SoundPlayer {
    override fun close() {
        playerScope.launch {
            job?.cancelAndJoin()
            stopInternal()
            focusMonitorJob?.cancel()
            focusMonitorJob = null
        }
    }

    private var job: Job? = null
    private val playbackMutex = Mutex()

    // iOS Media Player
    private var audioPlayer: AVAudioPlayer? = null

    // Monitoring job
    private var focusMonitorJob: Job? = null

    @Volatile
    private var state = SoundPlayerState()

    init {
        ioScope.launch {
            settingsRepo.settings.collect { settings ->
                state =
                    state.copy(
                        workRingTone = toSoundData(settings.workFinishedSound),
                        breakRingTone = toSoundData(settings.breakFinishedSound),
                        overrideSoundProfile = settings.overrideSoundProfile,
                        loop = settings.insistentNotification,
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
    override fun play(timerType: TimerType) {
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
    override fun play(
        soundData: SoundData,
        loop: Boolean,
        forceSound: Boolean,
    ) {
        logger.i { "▶️ play() called with soundData=${soundData.name}, uri=${soundData.uriString}, loop=$loop, forceSound=$forceSound" }
        playerScope.launch {
            job?.cancelAndJoin()
            job =
                playerScope.launch {
                    stopInternal() // Stop previous sound first
                    playInternal(soundData, loop, forceSound)
                }
        }
    }

    private suspend fun playInternal(
        soundData: SoundData,
        loop: Boolean,
        forceSound: Boolean,
    ) = playbackMutex.withLock {
        logger.i { "🎵 playInternal() called with soundData=${soundData.name}, uri=${soundData.uriString}" }

        if (soundData.isSilent) {
            logger.i { "🔇 Sound is silent, skipping playback" }
            return@withLock
        }

        // 1. Resolve Audio File URL
        // iOS Note: We cannot access system ringtones by path.
        // We assume `uriString` is a filename in the main bundle (e.g. "bell.mp3").
        // If empty, we try to find a default file named "default_sound".
        val fileName =
            soundData.uriString
                .takeUnless { it.isBlank() }
                ?: "positive_chime.wav"
        logger.d { "Resolved fileName: $fileName" }

        // Helper to split "sound.mp3" into "sound" and "mp3"
        val name = fileName.substringBeforeLast(".")
        val ext = if (fileName.contains(".")) fileName.substringAfterLast(".") else "mp3"
        logger.d { "Looking for resource: name=$name, ext=$ext" }

        val soundUrl: NSURL? =
            if (fileName.contains("/")) {
                val subdirectory = fileName.substringBeforeLast("/")
                val leafFileName = fileName.substringAfterLast("/")
                val leafName = leafFileName.substringBeforeLast(".")
                val leafExt =
                    if (leafFileName.contains(".")) {
                        leafFileName.substringAfterLast(".")
                    } else {
                        ext
                    }
                NSBundle.mainBundle.URLForResource(leafName, withExtension = leafExt, subdirectory = subdirectory)
            } else {
                NSBundle.mainBundle.URLForResource(name, withExtension = ext)
                    ?: NSBundle.mainBundle.URLForResource(name, withExtension = ext, subdirectory = "Sounds")
            }

        val resolvedSoundUrl =
            soundUrl
                ?: run {
                    // Stale/invalid uriString: fall back to bundled default.
                    val fallbackName = "positive_chime"
                    val fallbackExt = "wav"
                    logger.w { "Could not find sound file: $fileName in bundle. Falling back to $fallbackName.$fallbackExt" }
                    NSBundle.mainBundle.URLForResource(fallbackName, withExtension = fallbackExt)
                        ?: NSBundle.mainBundle.URLForResource(fallbackName, withExtension = fallbackExt, subdirectory = "Sounds")
                }

        if (resolvedSoundUrl == null) {
            logger.e(Exception("File not found")) { "Could not find fallback sound in bundle either" }
            return@withLock
        }
        logger.d { "Sound file found at: ${resolvedSoundUrl.path}" }

        // 2. Configure Audio Session (Audio Focus & Routing)
        val session = AVAudioSession.sharedInstance()

        try {
            // Determine Category
            // Playback: Plays even if silent switch is on (equivalent to USAGE_ALARM / Override Profile)
            // Ambient: Respects silent switch (equivalent to USAGE_NOTIFICATION)
            val category =
                if (state.overrideSoundProfile || forceSound || areHeadphonesPluggedIn(session)) {
                    logger.d { "Using AVAudioSessionCategoryPlayback (forceSound=$forceSound)" }
                    AVAudioSessionCategoryPlayback
                } else {
                    logger.d { "Using AVAudioSessionCategoryAmbient" }
                    AVAudioSessionCategoryAmbient
                }

            // Determine Options (Ducking)
            // If looping (insistent), we usually want to pause background audio (no DuckOthers).
            // If transient (ping), we want to duck (lower volume of) background audio.
            val options =
                if (loop) {
                    0.toULong() // No options = Interrupt background audio (Pause)
                } else {
                    AVAudioSessionCategoryOptionDuckOthers // Lower background audio volume
                }

            logger.d { "Setting audio session category and activating" }
            session.setCategory(category, withOptions = options, error = null)
            session.setActive(true, error = null)
            logger.d { "Audio session configured successfully" }
        } catch (e: Exception) {
            logger.e(e) { "Failed to configure AudioSession" }
        }

        // 3. Prepare and Play
        try {
            logger.d { "Creating AVAudioPlayer with URL: ${resolvedSoundUrl.path}" }
            val player = AVAudioPlayer(contentsOfURL = resolvedSoundUrl, error = null)
            logger.d { "Preparing to play..." }
            player.prepareToPlay()

            // Negative value means infinite loop
            player.numberOfLoops = if (loop) -1 else 0
            logger.d { "Number of loops: ${player.numberOfLoops}" }

            logger.d { "Starting playback..." }
            if (player.play()) {
                logger.d { "Playback started successfully" }
                audioPlayer = player

                // Monitor for completion if not looping (to deactivate session)
                if (!loop) {
                    startFocusMonitoring(player)
                }
            } else {
                logger.e(Exception("Play failed")) { "AVAudioPlayer failed to start" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Failed to initialize AVAudioPlayer" }
        }
    }

    /**
     * Stops any currently playing sound.
     * This method is safe to call even if no sound is currently playing.
     */
    override fun stop() {
        logger.d { "stop() called" }
        playerScope.launch {
            job?.cancelAndJoin()
            job =
                playerScope.launch {
                    stopInternal()
                }
        }
    }

    private fun stopInternal() {
        logger.d { "stopInternal() called" }
        focusMonitorJob?.cancel()
        focusMonitorJob = null

        audioPlayer?.let {
            if (it.isPlaying()) {
                logger.d { "Stopping currently playing audio" }
                it.stop()
            } else {
                logger.d { "Audio player exists but not playing" }
            }
        }
        audioPlayer = null

        abandonAudioFocusInternal()
    }

    /**
     * Monitors playback and deactivates audio session when finished.
     * Replaces Android's polling loop.
     */
    private fun startFocusMonitoring(player: AVAudioPlayer) {
        focusMonitorJob?.cancel()
        focusMonitorJob =
            playerScope.launch {
                try {
                    while (player.isPlaying()) {
                        delay(100)
                    }
                    abandonAudioFocusInternal()
                } catch (e: Exception) {
                    logger.e(e) { "Error monitoring playback completion" }
                }
            }
    }

    private fun abandonAudioFocusInternal() {
        try {
            // notifyOthersOnDeactivation: true allows background music to resume volume
            AVAudioSession.sharedInstance().setActive(false, withOptions = 1u, error = null)
        } catch (e: Exception) {
            logger.e(e) { "Failed to deactivate AudioSession" }
        }
    }

    private fun areHeadphonesPluggedIn(session: AVAudioSession): Boolean {
        val route = session.currentRoute
        val outputs = route.outputs

        // Check ports for headphones/bluetooth
        val headphoneTypes =
            setOf(
                AVAudioSessionPortHeadphones,
                AVAudioSessionPortBluetoothA2DP,
                AVAudioSessionPortBluetoothHFP,
                AVAudioSessionPortBluetoothLE,
                AVAudioSessionPortUSBAudio,
            )

        // Iterate through outputs using Kotlin collection operations
        return outputs.any { output ->
            val portDesc = output as? platform.AVFAudio.AVAudioSessionPortDescription
            portDesc != null && headphoneTypes.contains(portDesc.portType)
        }
    }
}
