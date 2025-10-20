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
package com.apps.adrcotfas.goodtime.stats

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.data.model.Label
import com.apps.adrcotfas.goodtime.ui.getLabelColor

@Composable
fun LabelChip(
    name: String,
    colorIndex: Long,
    selected: Boolean = true,
    showIcon: Boolean = false,
    onClick: () -> Unit,
) {
    val color = MaterialTheme.getLabelColor(colorIndex)
    LabelChip(name, color, selected, showIcon, onClick)
}

@Composable
fun LabelChip(
    name: String,
    color: Color,
    selected: Boolean = true,
    showIcon: Boolean = false,
    onClick: () -> Unit,
) {
    val defaultLabelName = stringResource(id = R.string.labels_default_label_name)
    val labelName = if (name == Label.DEFAULT_LABEL_NAME) defaultLabelName else name
    val transparentColor = color.copy(alpha = 0.15f)

    val chipColors =
        AssistChipDefaults.assistChipColors(
            containerColor = transparentColor,
            labelColor = color,
        )

    AssistChip(
        leadingIcon = {
            if (showIcon) {
                Icon(
                    imageVector = if (selected) Icons.AutoMirrored.Filled.Label else Icons.AutoMirrored.Outlined.Label,
                    contentDescription = null,
                    tint = color,
                )
            }
        },
        label = { Text(labelName) },
        onClick = onClick,
        colors = chipColors,
        shape = MaterialTheme.shapes.small,
        border =
            BorderStroke(
                width = 0.dp,
                color = Color.Transparent,
            ),
    )
}

@Composable
fun SmallLabelChip(
    name: String,
    colorIndex: Long,
) {
    val color = MaterialTheme.getLabelColor(colorIndex)
    SmallLabelChip(name, color)
}

@Composable
fun SmallLabelChip(
    name: String,
    color: Color,
) {
    val defaultLabelName = stringResource(id = R.string.labels_default_label_name)
    val labelName = if (name == Label.DEFAULT_LABEL_NAME) defaultLabelName else name

    Row(
        modifier =
            Modifier
                .wrapContentWidth()
                .widthIn(min = 32.dp)
                .clip(RoundedCornerShape(6.0.dp))
                .background(color.copy(alpha = 0.15f)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            text = labelName,
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

@Preview
@Composable
fun LabelChipPreview() {
    MaterialTheme {
        LabelChip("math", Color.Red, selected = false, showIcon = false, {})
    }
}

@Preview
@Composable
fun SmallLabelChipPreview() {
    MaterialTheme {
        SmallLabelChip(
            "math",
            Color.Red,
        )
    }
}
