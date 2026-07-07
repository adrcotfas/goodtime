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
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE
import android.hardware.camera2.CameraManager
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Uses the camera flash, if available, to notify the user.
 */
class AndroidTorchManager(
    context: Context,
    ioScope: CoroutineScope,
    private val playerScope: CoroutineScope,
    private val settingsRepo: SettingsRepository,
    private val logger: Logger,
) : TorchManager {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private var cameraId: String? = null
    private var data: TorchManagerData = TorchManagerData()

    private var job: Job? = null

    init {
        for (id in cameraManager.cameraIdList) {
            val flashAvailable =
                cameraManager.getCameraCharacteristics(id).get(FLASH_INFO_AVAILABLE)
            if (flashAvailable == true) {
                cameraId = id
                break
            }
        }
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

    override fun isTorchAvailable() = cameraId != null

    override fun start() {
        if (!data.enabled) return
        job =
            playerScope.launch {
                cameraId?.let {
                    try {
                        val pattern = listOf(100L, 50L, 100L)
                        if (data.loop) {
                            while (isActive) {
                                cameraManager.lightUp(pattern, it)
                                delay(1000)
                            }
                        } else {
                            cameraManager.lightUp(pattern, it)
                        }
                    } catch (e: CameraAccessException) {
                        logger.e(e) { "Failed to access the camera" }
                    }
                }
            }
    }

    override fun stop() {
        if (!data.enabled) return
        job?.cancel()
        cameraId?.let {
            try {
                cameraManager.setTorchMode(it, false)
            } catch (e: CameraAccessException) {
                logger.e(e) { "Failed to access the camera" }
            }
        }
    }
}

private suspend fun CameraManager.lightUp(
    listOf: List<Long>,
    cameraId: String,
) {
    listOf.forEachIndexed { index, i ->
        if (index % 2 == 0) {
            setTorchMode(cameraId, true)
        } else {
            setTorchMode(cameraId, false)
        }
        delay(i)
    }
    setTorchMode(cameraId, false)
}
