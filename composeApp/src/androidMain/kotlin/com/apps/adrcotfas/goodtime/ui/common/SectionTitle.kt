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
package com.apps.adrcotfas.goodtime.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PreferenceGroupTitle(
    modifier: Modifier = Modifier,
    text: String,
    paddingValues: PaddingValues =
        PaddingValues(
            horizontal = 16.dp,
            vertical = 24.dp,
        ),
) {
    Text(
        modifier = modifier.padding(paddingValues),
        text = text,
        style = MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary),
    )
}

@Composable
fun CompactPreferenceGroupTitle(
    modifier: Modifier = Modifier,
    text: String,
) {
    PreferenceGroupTitle(
        modifier = modifier,
        text = text,
        paddingValues =
            PaddingValues(
                top = 24.dp,
                bottom = 4.dp,
                start = 16.dp,
                end = 16.dp,
            ),
    )
}

@Composable
fun SubtleHorizontalDivider() {
    HorizontalDivider(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f))
}

@Composable
fun SubtleVerticalDivider() {
    VerticalDivider(
        modifier = Modifier.fillMaxHeight(),
        color = DividerDefaults.color.copy(alpha = 0.38f),
    )
}
