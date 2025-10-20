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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    isVisible: Boolean = true,
    title: String = "",
    onNavigateBack: (() -> Unit)? = null,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showSeparator: Boolean = false,
) {
    TopBar(
        isVisible = isVisible,
        title = { Text(text = title, maxLines = 1) },
        onNavigateBack = onNavigateBack,
        icon = icon,
        actions = actions,
        scrollBehavior = scrollBehavior,
        showSeparator = showSeparator,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    isVisible: Boolean = true,
    title: @Composable () -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    icon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    actions: @Composable RowScope.() -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    showSeparator: Boolean = false,
) {
    Column {
        CenterAlignedTopAppBar(
            modifier =
                Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .alpha(if (isVisible) 1f else 0f),
            title = title,
            navigationIcon = {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            icon,
                            contentDescription = "Navigate back",
                        )
                    }
                }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            actions = actions,
            scrollBehavior = scrollBehavior,
        )
        if (showSeparator) {
            SubtleHorizontalDivider()
        }
    }
}
