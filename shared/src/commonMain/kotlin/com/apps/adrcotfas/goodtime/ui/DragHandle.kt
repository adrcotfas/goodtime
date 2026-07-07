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
package com.apps.adrcotfas.goodtime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DragHandle(
    startButton: @Composable () -> Unit,
    endButton: @Composable () -> Unit,
) {
    Box(
        contentAlignment = Alignment.TopCenter,
    ) {
        BottomSheetDefaults.DragHandle()
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            startButton()
            endButton()
        }
    }
}

@Composable
fun DragHandle(
    onClose: () -> Unit,
    buttonText: String,
    buttonIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
    isEnabled: Boolean,
) {
    DragHandle(
        startButton = {
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, contentDescription = null)
            }
        },
        endButton = {
            Button(onClick = onClick, enabled = isEnabled) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start), verticalAlignment = Alignment.CenterVertically) {
                    buttonIcon?.let { it() }
                    Text(text = buttonText)
                }
            }
        },
    )
}

@Preview
@Composable
fun DragHandlePreview() {
    DragHandle({}, "Save", null, {}, true)
}
