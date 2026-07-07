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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVCaptureDeviceDiscoverySession
import platform.AVFoundation.AVCaptureDevicePositionBack
import platform.AVFoundation.AVCaptureDeviceTypeBuiltInWideAngleCamera
import platform.AVFoundation.AVCaptureTorchModeOff
import platform.AVFoundation.AVCaptureTorchModeOn
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.hasTorch
import platform.AVFoundation.torchMode
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/**
 * Uses the camera flash, if available, to notify the user.
 */
@OptIn(ExperimentalForeignApi::class)
class IosTorchManager(
    ioScope: CoroutineScope,
    private val playerScope: CoroutineScope,
    private val settingsRepo: SettingsRepository,
    private val logger: Logger,
) : TorchManager {
    private var data: TorchManagerData = TorchManagerData()
    private var job: Job? = null

    // AVCaptureDevice is the iOS equivalent of CameraManager/Camera
    private val torchDevice: AVCaptureDevice? = findTorchDevice()

    init {
        ioScope.launch {
            settingsRepo.settings
                .map {
                    TorchManagerData(
                        enabled = it.enableTorch,
                        loop = it.insistentNotification,
                    )
                }.collect {
                    data = it
                }
        }
    }

    // Finds the first available back camera with a torch (which is typically the main camera)
    private fun findTorchDevice(): AVCaptureDevice? {
        val discoverySession =
            AVCaptureDeviceDiscoverySession.discoverySessionWithDeviceTypes(
                deviceTypes = listOf(AVCaptureDeviceTypeBuiltInWideAngleCamera),
                mediaType = AVMediaTypeVideo,
                position = AVCaptureDevicePositionBack,
            )

        // Find the first device that supports torch mode using Kotlin collection operations
        return discoverySession.devices.firstOrNull { device ->
            val captureDevice = device as? AVCaptureDevice
            captureDevice != null && captureDevice.hasTorch
        } as? AVCaptureDevice
    }

    override fun isTorchAvailable() = torchDevice?.hasTorch == true

    override fun start() {
        if (!data.enabled || torchDevice == null) return

        // Cancel any previous job
        job?.cancel()

        job =
            playerScope.launch {
                val device = torchDevice

                // The pattern: 100ms ON, 50ms OFF, 100ms ON
                val pattern = listOf(100L, 50L, 100L)

                try {
                    if (data.loop) {
                        while (isActive) {
                            device.lightUp(pattern, logger, playerScope)
                            delay(1000) // 1000ms pause between patterns
                        }
                    } else {
                        device.lightUp(pattern, logger, playerScope)
                    }
                } catch (e: Exception) {
                    logger.e(e) { "Failed to control the torch" }
                } finally {
                    // Ensure torch is off when job is cancelled/finishes
                    setTorchModeInternal(device, false)
                }
            }
    }

    override fun stop() {
        if (!data.enabled || torchDevice == null) return
        job?.cancel()
        setTorchModeInternal(torchDevice, false)
    }
}

/**
 * Extension function on AVCaptureDevice to control the torch mode.
 * Note: Must be executed on the main thread for AVCaptureDevice lock.
 */
@OptIn(ExperimentalForeignApi::class)
private fun setTorchModeInternal(
    device: AVCaptureDevice,
    on: Boolean,
) {
    // AVCaptureDevice manipulation must be done on the main thread
    dispatch_async(dispatch_get_main_queue()) {
        if (device.lockForConfiguration(null)) {
            try {
                device.torchMode = if (on) AVCaptureTorchModeOn else AVCaptureTorchModeOff
            } finally {
                device.unlockForConfiguration()
            }
        }
    }
}

/**
 * Runs the light sequence asynchronously.
 */
@OptIn(ExperimentalForeignApi::class)
private suspend fun AVCaptureDevice.lightUp(
    pattern: List<Long>,
    @Suppress("UNUSED_PARAMETER") logger: Logger,
    playerScope: CoroutineScope,
) {
    pattern.forEachIndexed { index, duration ->
        if (!playerScope.isActive) return // Check coroutine state

        val isOn = index % 2 == 0 // Even indices (0, 2, 4...) are ON

        setTorchModeInternal(this, isOn)

        // Delay for the specified duration
        delay(duration)
    }
    // Ensure it is turned off at the end of the sequence
    setTorchModeInternal(this, false)
}
