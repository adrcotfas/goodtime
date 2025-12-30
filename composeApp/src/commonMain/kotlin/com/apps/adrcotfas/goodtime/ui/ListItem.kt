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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.Lock
import org.jetbrains.compose.ui.tooling.preview.Preview
import kotlin.math.roundToInt

@Composable
fun ListItemDefaults.enabledColors(): ListItemColors {
    val secondaryColor = colors().headlineColor.copy(alpha = 0.75f)
    return colors(
        supportingColor = secondaryColor,
        trailingIconColor = secondaryColor,
    )
}

@Composable
fun ListItemDefaults.disabledColors(): ListItemColors {
    val disabledColor = colors().disabledHeadlineColor
    return colors(
        headlineColor = disabledColor,
        supportingColor = disabledColor,
        trailingIconColor = disabledColor,
    )
}

@Composable
fun ListItemDefaults.selectedColors(): ListItemColors = colors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))

@Composable
fun BetterListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    trailing: String,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    BetterListItem(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        trailing = {
            Text(
                trailing,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        onClick = onClick,
        enabled = enabled,
    )
}

@Composable
fun BetterListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    leading: @Composable (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val internalModifier = modifier.padding(vertical = 4.dp)
    ListItem(
        modifier =
            if (enabled && onClick != null) {
                Modifier
                    .clickable(onClick = onClick)
                    .then(internalModifier)
            } else {
                internalModifier
            },
        colors = if (enabled) ListItemDefaults.enabledColors() else ListItemDefaults.disabledColors(),
        leadingContent = leading,
        headlineContent = { Text(text = title) },
        supportingContent = {
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        trailingContent = trailing,
    )
}

@Composable
fun BetterListItem(
    title: String,
    supporting: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
) {
    val modifier = Modifier.padding(vertical = 4.dp)
    ListItem(
        modifier =
            if (enabled && onClick != null) {
                Modifier
                    .clickable(onClick = onClick)
                    .then(modifier)
            } else {
                modifier
            },
        colors = if (enabled) ListItemDefaults.enabledColors() else ListItemDefaults.disabledColors(),
        headlineContent = { Text(text = title) },
        supportingContent = supporting,
    )
}

@Composable
fun IconListItem(
    title: String,
    subtitle: String? = null,
    icon: @Composable () -> Unit,
    isSelected: Boolean = false,
    onClick: () -> Unit,
) {
    val modifier = Modifier.padding(vertical = 4.dp)
    val colors =
        if (isSelected) {
            ListItemDefaults.selectedColors()
        } else {
            ListItemDefaults.enabledColors()
        }

    ListItem(
        modifier =
            Modifier
                .clickable(onClick = onClick)
                .then(modifier),
        colors = colors,
        headlineContent = { Text(text = title) },
        supportingContent = {
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        leadingContent = icon,
    )
}

@Composable
fun SliderListItem(
    modifier: Modifier = Modifier,
    title: String? = null,
    value: Int,
    icon: @Composable (() -> Unit)? = null,
    min: Int = 1,
    max: Int,
    steps: Int = max - min - 1,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit = { },
    showValue: Boolean = false,
    enabled: Boolean = true,
) {
    ListItem(
        modifier = modifier,
        colors = if (enabled) ListItemDefaults.enabledColors() else ListItemDefaults.disabledColors(),
        headlineContent = {
            if (title != null) {
                Text(text = title)
            }
        },
        leadingContent = icon,
        supportingContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Slider(
                    modifier = Modifier.weight(1f),
                    value = value.toFloat(),
                    onValueChange = {
                        onValueChange(it.roundToInt())
                    },
                    enabled = enabled,
                    onValueChangeFinished = onValueChangeFinished,
                    steps = steps,
                    valueRange = min.toFloat()..max.toFloat(),
                )
                if (showValue) {
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
    )
}

@Composable
fun CheckboxListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val toggleableModifier =
        if (enabled) {
            modifier.toggleable(
                value = checked,
                onValueChange = { onCheckedChange(!checked) },
                role = Role.Checkbox,
            )
        } else {
            modifier
        }
    BetterListItem(
        modifier = toggleableModifier,
        title = title,
        subtitle = subtitle,
        trailing = {
            Checkbox(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
            )
        },
        enabled = enabled,
        onClick =
            if (enabled) {
                { onCheckedChange.invoke(!checked) }
            } else {
                null
            },
    )
}

@Composable
fun LockedCheckboxListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val toggleableModifier =
        if (enabled) {
            modifier.toggleable(
                value = checked,
                onValueChange = { onCheckedChange(!checked) },
                role = Role.Checkbox,
            )
        } else {
            modifier
        }
    val leading: @Composable (() -> Unit)? =
        if (!enabled) {
            {
                Icon(
                    imageVector = EvaIcons.Outline.Lock,
                    tint = ListItemDefaults.disabledColors().disabledHeadlineColor,
                    contentDescription = null,
                )
            }
        } else {
            null
        }

    BetterListItem(
        modifier = toggleableModifier,
        title = title,
        subtitle = subtitle,
        leading = leading,
        trailing = {
            Checkbox(
                checked = checked,
                enabled = enabled,
                onCheckedChange = null,
            )
        },
        enabled = enabled,
        onClick =
            if (enabled) {
                { onCheckedChange.invoke(!checked) }
            } else {
                null
            },
    )
}

@Composable
fun SwitchListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    enabled: Boolean = true,
    showProgress: Boolean = false,
    onCheckedChange: (Boolean) -> Unit,
) {
    val isEnabled = enabled && !showProgress
    BetterListItem(
        modifier =
            if (isEnabled) {
                modifier.toggleable(
                    value = checked,
                    onValueChange = { onCheckedChange(!checked) },
                    role = Role.Switch,
                )
            } else {
                modifier
            },
        title = title,
        subtitle = subtitle,
        trailing = {
            if (showProgress) {
                Box(modifier = Modifier.size(32.dp)) {
                    CircularProgressIndicator(modifier = Modifier.fillMaxSize())
                }
            } else {
                Switch(
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = null,
                )
            }
        },
        enabled = isEnabled,
        onClick = if (isEnabled) {
            { onCheckedChange(!checked) }
        } else {
            null
        },
    )
}

@Composable
fun DropdownMenuListItem(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String? = null,
    value: String,
    enabled: Boolean = true,
    dropdownMenuOptions: List<String>,
    onDropdownMenuItemSelected: (Int) -> Unit,
) {
    var dropdownMenuExpanded by remember { mutableStateOf(false) }
    BetterListItem(
        modifier = modifier,
        title = title,
        subtitle = subtitle,
        trailing = {
            Box {
                Text(text = value, style = MaterialTheme.typography.bodyMedium)
                BetterDropdownMenu(
                    expanded = dropdownMenuExpanded,
                    value = value,
                    onDismissRequest = { dropdownMenuExpanded = false },
                    dropdownMenuOptions = dropdownMenuOptions,
                    onDropdownMenuItemSelected = onDropdownMenuItemSelected,
                )
            }
        },
        onClick = { dropdownMenuExpanded = true },
        enabled = enabled,
    )
}

@Composable
fun CircularProgressListItem(
    title: String,
    subtitle: String? = null,
    showProgress: Boolean,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    BetterListItem(
        title = title,
        subtitle = subtitle,
        trailing = {
            Box(
                modifier = Modifier.size(32.dp),
            ) {
                if (showProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        },
        onClick = onClick,
        enabled = enabled,
    )
}

@Preview
@Composable
fun SliderListItemPreview() {
    SliderListItem(
        value = 0,
        icon = {
            Icon(Icons.Default.TextFormat, contentDescription = null)
        },
        min = 0,
        max = 5,
        showValue = false,
        onValueChange = {},
        onValueChangeFinished = {},
    )
}
