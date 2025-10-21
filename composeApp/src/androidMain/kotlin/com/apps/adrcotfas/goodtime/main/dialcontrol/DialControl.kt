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
package com.apps.adrcotfas.goodtime.main.dialcontrol

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitDragOrCancellation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.apps.adrcotfas.goodtime.main.dialcontrol.DialControlState.Companion.calculateStartAngle
import compose.icons.EvaIcons
import compose.icons.evaicons.Fill
import compose.icons.evaicons.fill.ArrowIosBack
import compose.icons.evaicons.fill.ArrowIosForward
import compose.icons.evaicons.fill.Plus
import goodtime_productivity.composeapp.generated.resources.Res
import goodtime_productivity.composeapp.generated.resources.main_plus_1_min
import goodtime_productivity.composeapp.generated.resources.main_skip
import goodtime_productivity.composeapp.generated.resources.main_stop
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import kotlin.math.cos
import kotlin.math.sin

data class DialControlColors(
    val dialColor: Color,
    val indicatorColor: Color,
    val selectionColor: Color,
)

object DialControlDefaults {
    @Composable
    @ReadOnlyComposable
    fun colors(
        dialColor: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
        indicatorColor: Color = MaterialTheme.colorScheme.primary,
        selectionColor: Color = MaterialTheme.colorScheme.primary,
    ): DialControlColors =
        DialControlColors(
            dialColor = dialColor,
            indicatorColor = indicatorColor,
            selectionColor = selectionColor,
        )

    @Composable
    fun Indicator(
        colors: DialControlColors,
        state: DialControlState<*>,
    ) {
        Box(
            modifier =
                Modifier
                    .graphicsLayer {
                        state.indicatorOffset.value.let {
                            translationX = it.x
                            translationY = it.y
                        }
                    }.size(state.config.indicatorSize)
                    .background(color = colors.indicatorColor, shape = CircleShape),
        )
    }
}

data class DialConfig(
    val size: Dp = 176.dp,
    val indicatorSize: Dp = 24.dp,
    val cutoffFraction: Float = 0.4f,
    val enableHaptics: Boolean = true,
    val dialAlignment: Alignment = Alignment.TopStart,
)

@Composable
fun <T> DialControl(
    state: DialControlState<T>,
    dialContent: @Composable (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: DialControlColors = DialControlDefaults.colors(),
    indicator: @Composable (DialControlState<T>) -> Unit = {
        DialControlDefaults.Indicator(colors, state)
    },
) {
    if (state.config.enableHaptics) {
        val hapticFeedback = LocalHapticFeedback.current
        LaunchedEffect(state) {
            val selection = snapshotFlow { state.selectedOption }
            selection
                .zip(selection.drop(1)) { previous, current ->
                    if (previous != current && current != null) {
                        HapticFeedbackType.LongPress
                    } else {
                        null
                    }
                }.filterNotNull()
                .collect {
                    hapticFeedback.performHapticFeedback(it)
                }
        }
    }

    val gestureModifier =
        if (enabled) {
            Modifier
                .pointerInput(state) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        state.onDown(position = down.position)
                        var change =
                            awaitTouchSlopOrCancellation(pointerId = down.id) { change, _ ->
                                change.consume()
                            }
                        while (change != null && change.pressed) {
                            change =
                                awaitDragOrCancellation(change.id)?.also {
                                    if (it.pressed) {
                                        state.onDrag(dragAmount = it.positionChange())
                                    }
                                }
                        }
                        state.onRelease()
                    }
                }
        } else {
            Modifier
        }
    Box {
        AnimatedVisibility(
            visible = state.isDragging,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier =
                Modifier // containerModifier
                    .padding(16.dp)
                    .padding(state.config.indicatorSize)
                    .size(state.config.size)
                    .align(state.config.dialAlignment),
        ) {
            CircleDial(
                modifier =
                    modifier
                        .then(gestureModifier),
                state = state,
                optionContent = dialContent,
                colors = colors,
                indicator = { indicator(state) },
            )
        }
    }
}

@Composable
private fun <T> CircleDial(
    modifier: Modifier = Modifier,
    state: DialControlState<T>,
    optionContent: @Composable (T) -> Unit,
    colors: DialControlColors,
    indicator: @Composable () -> Unit,
) {
    val alphas =
        remember(state.options) {
            state.options.associateWith { Animatable(initialValue = 0f, Float.VectorConverter) }.toMap()
        }

    LaunchedEffect(state.selectedOption, state.options) {
        state.options.forEach { option ->
            launch {
                alphas[option]?.animateTo(
                    if (option == state.selectedOption) 1f else 0f,
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow,
                        ),
                )
            }
        }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        val sweep = 360f / state.options.size
        Box(
            modifier =
                Modifier
                    .fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier =
                    Modifier
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }.fillMaxSize()
                        .background(color = colors.dialColor, shape = CircleShape),
            ) {
                state.options.forEachIndexed { index, option ->
                    val alpha = alphas[option]!!.value
                    val startAngle = calculateStartAngle(index = index, count = state.options.size)
                    drawArc(
                        color = colors.selectionColor,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        alpha = alpha,
                        useCenter = true,
                    )
                }

                val radius = size.minDimension / 2
                state.options.indices.forEach { index ->
                    val startAngle = calculateStartAngle(index = index, count = state.options.size)
                    val radian = startAngle * Math.PI / 180
                    val x = center.x + radius * cos(radian)
                    val y = center.y + radius * sin(radian)
                    drawLine(
                        color = Color.Black,
                        start = center,
                        end = Offset(x.toFloat(), y.toFloat()),
                        strokeWidth = 4.dp.toPx(),
                        cap = StrokeCap.Round,
                        blendMode = BlendMode.DstOut,
                    )
                }

                scale(scale = state.config.cutoffFraction) {
                    drawCircle(color = Color.Black, blendMode = BlendMode.DstOut)
                }
            }

            indicator()
        }

        state.options.forEachIndexed { index, option ->
            key(option) {
                Box(
                    modifier =
                        Modifier
                            .graphicsLayer {
                                val optionAlpha = 1f
                                val startAngle =
                                    calculateStartAngle(index = index, count = state.options.size)
                                val radians = (startAngle + sweep / 2) * Math.PI / 180
                                val radius =
                                    (state.config.size.toPx() / 2) * (state.config.cutoffFraction + (1f - state.config.cutoffFraction) / 2)
                                translationX = (radius * cos(radians)).toFloat()
                                translationY = (radius * sin(radians)).toFloat()
                                alpha = optionAlpha
                            },
                ) {
                    optionContent(option)
                }
            }
        }
    }
}

enum class DialRegion(
    val icon: ImageVector,
    val labelId: StringResource,
) {
    TOP(icon = EvaIcons.Fill.Plus, labelId = Res.string.main_plus_1_min),
    RIGHT(icon = EvaIcons.Fill.ArrowIosForward, labelId = Res.string.main_skip),
    BOTTOM(icon = Icons.Filled.Close, Res.string.main_stop),
    LEFT(icon = EvaIcons.Fill.ArrowIosBack, labelId = Res.string.main_skip),
}
