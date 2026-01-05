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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.LayoutScopeMarker
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import compose.icons.EvaIcons
import compose.icons.evaicons.Outline
import compose.icons.evaicons.outline.ColorPalette
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.settings_user_interface
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun GroupedListItemContainer(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    middleCornerRadius: Dp = 4.dp,
    itemBackgroundColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    itemSpacing: Dp = 2.dp,
    content: @Composable GroupedListItemScope.() -> Unit,
) {
    val items = mutableListOf<@Composable () -> Unit>()
    var sectionTitle: String? = null
    val scope =
        object : GroupedListItemScope {
            @Composable
            override fun title(title: String) {
                sectionTitle = title
            }

            @Composable
            override fun item(content: @Composable () -> Unit) {
                items.add(content)
            }
        }

    scope.content()

    Column(modifier = modifier.fillMaxWidth()) {
        sectionTitle?.let {
            Text(
                sectionTitle,
                modifier = Modifier.padding(start = 8.dp, bottom = 10.dp),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        items.forEachIndexed { index, itemContent ->
            if (index > 0) {
                Spacer(modifier = Modifier.height(itemSpacing))
            }

            val shape =
                when {
                    items.size == 1 -> RoundedCornerShape(cornerRadius)
                    index == 0 ->
                        RoundedCornerShape(
                            topStart = cornerRadius,
                            topEnd = cornerRadius,
                            bottomStart = middleCornerRadius,
                            bottomEnd = middleCornerRadius,
                        )

                    index == items.size - 1 ->
                        RoundedCornerShape(
                            topStart = middleCornerRadius,
                            topEnd = middleCornerRadius,
                            bottomStart = cornerRadius,
                            bottomEnd = cornerRadius,
                        )

                    else -> RoundedCornerShape(middleCornerRadius)
                }

            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(itemBackgroundColor),
            ) {
                itemContent()
            }
        }
    }
}

@Suppress("ComposableNaming")
@LayoutScopeMarker
interface GroupedListItemScope {
    @Composable
    fun title(title: String)

    @Composable
    fun item(content: @Composable () -> Unit)
}

@Preview
@Composable
fun LanguageSettingsExample() {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column {
            GroupedListItemContainer {
                title("Language settings")
                item {
                    BetterListItem(title = "System Languages", subtitle = "English (United States)")
                }
            }
        }

        Column {
            GroupedListItemContainer {
                title("More language settings")
                item {
                    BetterListItem(
                        title = "App languages",
                        subtitle = "Choose the language for each app",
                        onClick = {
                        },
                    )
                }

                item {
                    SwitchListItem(
                        title = "Speech",
                        subtitle = "Control speech recognition and output",
                        checked = true,
                        onCheckedChange = {
                        },
                    )
                }
            }
        }

        GroupedListItemContainer {
            item {
                BetterListItem(
                    title = "App languages",
                    onClick = {
                    },
                )
            }

            item {
                IconListItem(
                    title = stringResource(Res.string.settings_user_interface),
                    icon = {
                        Icon(
                            imageVector = EvaIcons.Outline.ColorPalette,
                            contentDescription = stringResource(Res.string.settings_user_interface),
                        )
                    },
                    onClick = {},
                )
            }

            item {
                BetterListItem(
                    title = "Speech",
                    subtitle = "Control speech recognition and output",
                    onClick = {
                    },
                )
            }
            item {
                BetterListItem(
                    title = "System Languages",
                    subtitle = "English (United States)",
                    onClick = {
                    },
                )
            }
        }
    }
}
