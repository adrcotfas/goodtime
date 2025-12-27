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
package com.apps.adrcotfas.goodtime.settings.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import co.touchlab.kermit.Logger
import com.apps.adrcotfas.goodtime.bl.notifications.SoundPlayer
import com.apps.adrcotfas.goodtime.data.settings.SoundData
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
actual fun NotificationSoundPickerDialog(
    title: String,
    selectedItem: SoundData,
    onSelected: (SoundData) -> Unit,
    onSave: (SoundData) -> Unit,
    onDismiss: () -> Unit,
) {
    val soundPlayer = koinInject<SoundPlayer>()
    val coroutineScope = rememberCoroutineScope()
    val logger = remember { Logger.withTag("SoundPickerDialog") }

    val systemSounds =
        linkedSetOf(
            SoundData("Positive chime", "positive_chime.wav"),
            SoundData("Marimba", "marimba.wav"),
            SoundData("Harp chime", "harp_chime.wav"),
            SoundData("Doorbell", "doorbell.wav"),
            SoundData("Digital tone", "digital_tone.wav"),
            SoundData("Bubble pop", "bubble_pop.wav"),
        )

    NotificationSoundPickerDialogContent(
        title = title,
        selectedItem = selectedItem,
        items = systemSounds,
        onSelected = {
            logger.i { "Sound selected: name=${it.name}, uri=${it.uriString}" }
            onSelected(it)
            coroutineScope.launch {
                logger.i { "Playing sound: ${it.uriString}" }
                soundPlayer.play(it, loop = false, forceSound = true)
            }
        },
        onSave = onSave,
        onDismiss = {
            logger.i { "Dismissing dialog, stopping sound" }
            coroutineScope.launch {
                soundPlayer.stop()
            }
            onDismiss()
        },
        platformSpecificContent = null,
    )
}
