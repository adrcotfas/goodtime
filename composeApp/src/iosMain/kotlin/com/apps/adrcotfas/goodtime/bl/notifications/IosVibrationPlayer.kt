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
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import platform.CoreHaptics.CHHapticAdvancedPatternPlayerProtocol
import platform.CoreHaptics.CHHapticEngine
import platform.CoreHaptics.CHHapticEvent
import platform.CoreHaptics.CHHapticEventParameter
import platform.CoreHaptics.CHHapticEventParameterIDHapticIntensity
import platform.CoreHaptics.CHHapticEventParameterIDHapticSharpness
import platform.CoreHaptics.CHHapticEventTypeHapticContinuous
import platform.CoreHaptics.CHHapticEventTypeHapticTransient
import platform.CoreHaptics.CHHapticPattern
import platform.CoreHaptics.CHHapticTimeImmediate
import platform.Foundation.NSError

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosVibrationPlayer(
    private val settingsRepo: SettingsRepository,
    private val playerScope: CoroutineScope,
    ioScope: CoroutineScope,
    private val logger: Logger,
) : VibrationPlayer {
    private var data: VibrationData = VibrationData(3, false)
    private var job: Job? = null

    // Core Haptics Objects
    private var engine: CHHapticEngine? = null
    private var player: CHHapticAdvancedPatternPlayerProtocol? = null

    init {
        logger.i { "IosVibrationPlayer initializing..." }

        // Initialize Engine
        createEngine()

        // Observe Settings
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

    private fun createEngine() {
        logger.d { "createEngine() called" }

        // Check hardware support
        val capabilities = CHHapticEngine.capabilitiesForHardware()
        logger.d { "Haptics capabilities - supportsHaptics: ${capabilities.supportsHaptics}, supportsAudio: ${capabilities.supportsAudio}" }

        if (!capabilities.supportsHaptics) {
            logger.w { "Haptics not supported on this device" }
            return
        }

        memScoped {
            val errorPtr = alloc<ObjCObjectVar<NSError?>>()
            errorPtr.value = null

            logger.d { "Creating CHHapticEngine..." }

            // Create engine using the proper initializer with error pointer
            val createdEngine = CHHapticEngine(errorPtr.ptr)

            // Check if there was an error during creation
            val error = errorPtr.value
            if (error != null) {
                logger.e(Exception("CHHapticEngine error: ${error.localizedDescription}")) {
                    "Failed to create CHHapticEngine - code: ${error.code}, domain: ${error.domain}"
                }
                engine = null
                return
            }

            // If we get here, createdEngine should be valid
            engine = createdEngine
            logger.i { "CHHapticEngine instance created successfully" }

            // Handle Reset (e.g. app coming back from background)
            createdEngine.resetHandler = {
                logger.i { "Haptic engine reset" }
                memScoped {
                    val resetErrorPtr = alloc<ObjCObjectVar<NSError?>>()
                    resetErrorPtr.value = null
                    createdEngine.startAndReturnError(resetErrorPtr.ptr)

                    val resetError = resetErrorPtr.value
                    if (resetError != null) {
                        logger.e(Exception("Reset start error: ${resetError.localizedDescription}")) {
                            "Failed to restart haptic engine after reset"
                        }
                    }
                }
            }

            // Handle stopped handler
            createdEngine.stoppedHandler = { reason ->
                logger.w { "Haptic engine stopped: $reason" }
            }

            logger.i { "Haptic engine created and configured successfully" }
        }
    }

    override fun start() {
        start(data)
    }

    override fun stop() {
        playerScope.launch {
            job?.cancelAndJoin()
            job =
                playerScope.launch {
                    stopHaptics()
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
                playerScope.launch {
                    stopHaptics()

                    val (strength, loop) = data

                    if (strength == 0) {
                        logger.d { "Vibration strength is 0, skipping" }
                        return@launch
                    }

                    // Ensure engine is running
                    val currentEngine = engine
                    if (currentEngine == null) {
                        logger.e(Exception("Engine is null")) { "Haptic engine is null" }
                        return@launch
                    }

                    memScoped {
                        val startErrorPtr = alloc<ObjCObjectVar<NSError?>>()
                        startErrorPtr.value = null

                        logger.d { "Starting haptic engine..." }
                        currentEngine.startAndReturnError(startErrorPtr.ptr)

                        val startError = startErrorPtr.value
                        if (startError != null) {
                            logger.e(Exception("Engine start error: ${startError.localizedDescription}")) {
                                "Engine start failed - code: ${startError.code}, domain: ${startError.domain}"
                            }
                            return@launch
                        }
                        logger.d { "Haptic engine started" }
                    }

                    val pattern = getPatternForStrength(strength)
                    if (pattern.isEmpty()) {
                        logger.w { "Empty pattern for strength $strength" }
                        return@launch
                    }

                    logger.d { "Playing vibration pattern for strength $strength (loop: $loop)" }
                    logger.d { "Pattern: ${pattern.joinToString()}" }

                    // Artificial delay to match Android's "avoid ignore" logic
                    delay(100)

                    playPattern(pattern, loop)
                }
        }
    }

    private fun stopHaptics() {
        memScoped {
            try {
                val stopErrorPtr = alloc<ObjCObjectVar<NSError?>>()
                stopErrorPtr.value = null

                player?.stopAtTime(CHHapticTimeImmediate, stopErrorPtr.ptr)

                val stopError = stopErrorPtr.value
                if (stopError != null) {
                    logger.w(Exception("Stop error: ${stopError.localizedDescription}")) {
                        "Error stopping haptics - code: ${stopError.code}, domain: ${stopError.domain}"
                    }
                }

                player = null
                logger.d { "Haptics stopped" }
            } catch (e: Exception) {
                logger.w(e) { "Error stopping haptics" }
            }
        }
    }

    private fun playPattern(
        androidPattern: LongArray,
        loop: Boolean,
    ) {
        val currentEngine = engine
        if (currentEngine == null) {
            logger.e(Exception("Engine is null")) { "Engine is null in playPattern" }
            return
        }

        memScoped {
            try {
                logger.d { "Creating haptic pattern..." }

                // Convert Android millisecond pattern to iOS CHHapticPattern
                val hapticPattern = createHapticPattern(androidPattern)

                logger.d { "Creating advanced player..." }

                // Create Player with error handling
                val playerErrorPtr = alloc<ObjCObjectVar<NSError?>>()
                playerErrorPtr.value = null

                val newPlayer =
                    currentEngine.createAdvancedPlayerWithPattern(hapticPattern, playerErrorPtr.ptr)

                val playerError = playerErrorPtr.value
                if (playerError != null) {
                    logger.e(Exception("Player creation error: ${playerError.localizedDescription}")) {
                        "Failed to create player - code: ${playerError.code}, domain: ${playerError.domain}"
                    }
                    return
                }

                if (newPlayer == null) {
                    logger.e(Exception("Player is null")) { "Failed to create player (returned null)" }
                    return
                }

                newPlayer.loopEnabled = loop
                player = newPlayer

                logger.d { "Starting playback..." }

                // Start playback with error handling
                val startErrorPtr = alloc<ObjCObjectVar<NSError?>>()
                startErrorPtr.value = null
                newPlayer.startAtTime(CHHapticTimeImmediate, startErrorPtr.ptr)

                val startError = startErrorPtr.value
                if (startError != null) {
                    logger.e(Exception("Playback start error: ${startError.localizedDescription}")) {
                        "Failed to start playback - code: ${startError.code}, domain: ${startError.domain}"
                    }
                    return
                }

                logger.i { "Haptic playback started successfully" }
            } catch (e: Exception) {
                logger.e(e) { "Failed to play haptic pattern" }
            }
        }
    }

    /**
     * Converts Android LongArray [Wait, Vib, Wait, Vib...] (ms)
     * into CoreHaptics Events (seconds)
     */
    private fun createHapticPattern(timings: LongArray): CHHapticPattern {
        val events = mutableListOf<CHHapticEvent>()

        // Use max intensity for strong haptic feedback
        val intensityVal = 1.0f
        val sharpnessVal = 0.5f

        var currentTime = 0.0

        logger.d { "Converting pattern: ${timings.joinToString()}" }

        for (i in timings.indices) {
            val durationSeconds = timings[i] / 1000.0

            if (i % 2 == 0) {
                // Even index = WAIT (in Android: 0, 100, 200...)
                // We simply advance the time cursor
                currentTime += durationSeconds
            } else {
                // Odd index = VIBRATE
                val intensity =
                    CHHapticEventParameter(
                        parameterID = CHHapticEventParameterIDHapticIntensity,
                        value = intensityVal,
                    )
                val sharpness =
                    CHHapticEventParameter(
                        parameterID = CHHapticEventParameterIDHapticSharpness,
                        value = sharpnessVal,
                    )

                val event =
                    CHHapticEvent(
                        eventType = CHHapticEventTypeHapticContinuous,
                        parameters = listOf(intensity, sharpness),
                        relativeTime = currentTime,
                        duration = durationSeconds,
                    )
                events.add(event)
                currentTime += durationSeconds

                logger.d { "  Vibrate at ${currentTime}s for ${durationSeconds}s" }
            }
        }

        // Handle the trailing wait (e.g. the 1000ms at the end of your arrays).
        // iOS loop restarts immediately after the last event ends.
        // We add a silent, transient event at the very end to force the loop to wait.
        if (timings.isNotEmpty() && timings.last() > 0 && timings.size % 2 != 0) {
            // If array size is odd, the last element was a WAIT, which we processed above
            // by adding to currentTime. Now we place a marker there.
            val silentIntensity =
                CHHapticEventParameter(
                    parameterID = CHHapticEventParameterIDHapticIntensity,
                    value = 0.0f,
                )

            val silentEvent =
                CHHapticEvent(
                    eventType = CHHapticEventTypeHapticTransient,
                    parameters = listOf(silentIntensity),
                    relativeTime = currentTime,
                    duration = 0.001,
                )
            events.add(silentEvent)

            logger.d { "  Silent marker at ${currentTime}s" }
        }

        logger.d { "Pattern created with ${events.size} events" }

        // Create pattern with events
        val pattern =
            CHHapticPattern(
                events = events,
                parameters = emptyList<CHHapticEventParameter>(),
                error = null,
            )

        return pattern
    }

    private fun getPatternForStrength(strength: Int): LongArray =
        when (strength) {
            1 -> longArrayOf(0, 100, 2000)
            2 -> longArrayOf(0, 100, 50, 100, 1000)
            3 -> longArrayOf(0, 200, 50, 200, 1000)
            4 -> longArrayOf(0, 400, 100, 400, 1000)
            5 -> longArrayOf(0, 400, 100, 400, 100, 400, 1000)
            else -> longArrayOf()
        }
}
