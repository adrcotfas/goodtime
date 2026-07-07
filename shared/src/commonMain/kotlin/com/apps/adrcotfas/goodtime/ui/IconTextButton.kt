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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Heart
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun IconTextButton(
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    isFilled: Boolean = false,
    centered: Boolean = false,
    onClick: () -> Unit,
) {
    val content: @Composable () -> Unit =
        {
            val alignment = if (centered) Alignment.CenterHorizontally else Alignment.Start
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(if (isFilled) 42.dp else 48.dp)
                        .then(if (isFilled) Modifier else Modifier.padding(horizontal = 12.dp)),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp, alignment),
            ) {
                icon()
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    )
                    subtitle?.let {
                        Text(text = it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

    if (isFilled) {
        val colors =
            ButtonDefaults
                .buttonColors()
                .copy(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                )
        Button(colors = colors, onClick = onClick) {
            content()
        }
    } else {
        val colors =
            ButtonDefaults
                .textButtonColors()
                .copy(contentColor = MaterialTheme.colorScheme.onSurface)
        TextButton(colors = colors, onClick = onClick) {
            content()
        }
    }
}

@Preview
@Composable
fun IconTextButtonFilledPreview() {
    IconTextButton(
        title = "Some title",
        subtitle = "Some subtitle",
        icon = {
            Icon(
                imageVector = EvaIcons.Outline.Heart,
                contentDescription = "Upgrade to Pro",
            )
        },
        isFilled = true,
        centered = true,
    ) { }
}

@Preview
@Composable
fun IconTextButtonPreview() {
    Surface {
        IconTextButton(
            title = "Some title",
            subtitle = "Some subtitle",
            icon = {
                Icon(
                    imageVector = EvaIcons.Outline.Heart,
                    contentDescription = null,
                )
            },
            centered = false,
        ) {}
    }
}
