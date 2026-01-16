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

import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.apps.adrcotfas.goodtime.bl.notifications.SoundPlayer
import com.apps.adrcotfas.goodtime.common.findActivity
import com.apps.adrcotfas.goodtime.common.getFileName
import com.apps.adrcotfas.goodtime.data.settings.SoundData
import com.apps.adrcotfas.goodtime.ui.PreferenceGroupTitle
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Plus
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.settings_add_custom_sound
import goodtime_productivity.composeapp.generated.resources.settings_your_sounds
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
actual fun NotificationSoundPickerDialog(
    title: String,
    selectedItem: SoundData,
    onSelected: (SoundData) -> Unit,
    onSave: (SoundData) -> Unit,
    onDismiss: () -> Unit,
) {
    val viewModel: SoundsViewModel = koinViewModel()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val activity = context.findActivity()
    val soundPlayer = koinInject<SoundPlayer>()

    val pickSoundLauncher =
        rememberLauncherForActivityResult(
            contract =
                object : ActivityResultContracts.OpenDocument() {
                    override fun createIntent(
                        context: Context,
                        input: Array<String>,
                    ): Intent =
                        super.createIntent(context, input).apply {
                            addFlags(FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
                            addCategory(Intent.CATEGORY_OPENABLE)
                            type = "audio/*"
                        }
                },
        ) { uri ->
            if (uri != null) {
                context.getFileName(uri)?.let {
                    val soundData = SoundData(name = it, uriString = uri.toString())
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION,
                    )
                    viewModel.saveUserSound(soundData)
                    onSelected(soundData)
                }
            }
        }

    activity?.let {
        LaunchedEffect(Unit) {
            viewModel.fetchNotificationSounds(activity)
        }
    }

    val items by viewModel.soundData.collectAsStateWithLifecycle()
    val userItems by viewModel.userSoundData.collectAsStateWithLifecycle()

    NotificationSoundPickerDialogContent(
        title = title,
        selectedItem = selectedItem,
        items = items,
        onSelected = {
            onSelected(it)
            coroutineScope.launch {
                soundPlayer.play(it, loop = false, forceSound = true)
            }
        },
        onSave = onSave,
        onDismiss = {
            coroutineScope.launch {
                soundPlayer.stop()
            }
            onDismiss()
        },
        platformSpecificContent = {
            item(key = "user sounds") {
                PreferenceGroupTitle(
                    modifier = Modifier.animateItem(),
                    text = stringResource(Res.string.settings_your_sounds),
                )
            }
            items(userItems.toList(), key = { "user" + it.uriString }) { item ->
                val isSelected = selectedItem == item
                NotificationSoundItem(
                    modifier = Modifier.animateItem(),
                    name = item.name,
                    isSelected = isSelected,
                    isCustomSound = true,
                    onRemove = { viewModel.removeUserSound(item) },
                ) {
                    onSelected(item)
                    coroutineScope.launch {
                        soundPlayer.play(item, loop = false, forceSound = true)
                    }
                }
            }
            item(key = "add custom sound") {
                AddCustomSoundButton(
                    modifier = Modifier.animateItem(),
                    onAddUserSound = {
                        coroutineScope.launch {
                            soundPlayer.stop()
                        }
                        pickSoundLauncher.launch(arrayOf("audio/*"))
                    },
                )
            }
        },
    )
}

@Composable
fun AddCustomSoundButton(
    modifier: Modifier = Modifier,
    onAddUserSound: () -> Unit,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable { onAddUserSound() }
                .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = EvaIcons.Outline.Plus,
            contentDescription = null,
            modifier = Modifier.padding(end = 16.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = stringResource(Res.string.settings_add_custom_sound),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Preview
@Composable
fun NotificationSoundPickerDialogPreview() {
    NotificationSoundPickerDialogContent(
        title = "Focus complete sound",
        selectedItem = SoundData("Mallet", "Mallet"),
        onSelected = {},
        onSave = {},
        onDismiss = {},
        items =
            setOf(
                SoundData("Coconuts", "Coconuts"),
                SoundData("Mallet", "Mallet"),
                SoundData("Music Box", "Music Box"),
            ),
        platformSpecificContent = null,
    )
}
