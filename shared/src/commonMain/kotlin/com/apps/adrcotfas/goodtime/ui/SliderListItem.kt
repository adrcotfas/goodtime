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

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFormat
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// M3 expressive slider, size M
private val trackHeight = 40.dp
private val thumbSize = DpSize(4.dp, 52.dp)
private val thumbTrackGap = 6.dp
private val trackOuterCorner = 12.dp
private val trackInnerCorner = 2.dp
private val stopIndicatorRadius = 2.dp

private val trackIconSize = 24.dp
private val trackIconEndPadding = 12.dp

/** How early the icon snaps to the handle, before the handle reaches it */
private val iconSnapMargin = 16.dp

/** Icon distance from the active track's edge while riding along the handle */
private val snappedIconGap = 8.dp

private val iconSpacing = 16.dp

private const val DISABLED_CONTENT_ALPHA = 0.38f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderListItem(
    modifier: Modifier = Modifier,
    title: String? = null,
    value: Int,
    icon: @Composable (() -> Unit)? = null,
    trackIcon: ImageVector? = null,
    min: Int = 1,
    max: Int,
    steps: Int = max - min - 1,
    // continuous drag; on release the handle animates to the nearest integer
    animateToNearestStep: Boolean = false,
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
        supportingContent = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                icon?.let {
                    it()
                    Spacer(modifier = Modifier.width(iconSpacing))
                }
                val interactionSource = remember { MutableInteractionSource() }
                val haptic = LocalHapticFeedback.current
                val scope = rememberCoroutineScope()
                val floatValue = remember { Animatable(value.toFloat()) }
                var rawValue by remember { mutableFloatStateOf(value.toFloat()) }
                var lastTicked by remember { mutableIntStateOf(value) }
                val stepSize = if (steps > 0) (max - min).toFloat() / (steps + 1) else 1f
                val nearestStep = { v: Float ->
                    (min + ((v - min) / stepSize).roundToInt() * stepSize).roundToInt().coerceIn(min, max)
                }
                if (animateToNearestStep) {
                    // follow external value changes; the targetValue check keeps the
                    // release animation (whose target already equals value) uninterrupted
                    LaunchedEffect(value) {
                        if (floatValue.targetValue != value.toFloat()) {
                            rawValue = value.toFloat()
                            floatValue.snapTo(value.toFloat())
                        }
                    }
                }
                Slider(
                    modifier = Modifier.weight(1f),
                    value = if (animateToNearestStep) floatValue.value else value.toFloat(),
                    onValueChange = {
                        val newValue = nearestStep(it)
                        if (newValue != lastTicked) {
                            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
                            lastTicked = newValue
                        }
                        if (animateToNearestStep) {
                            rawValue = it
                            scope.launch { floatValue.snapTo(it) }
                        } else {
                            onValueChange(newValue)
                        }
                    },
                    enabled = enabled,
                    onValueChangeFinished = {
                        if (animateToNearestStep) {
                            val target = nearestStep(rawValue)
                            rawValue = target.toFloat()
                            onValueChange(target)
                            scope.launch { floatValue.animateTo(target.toFloat()) }
                        }
                        onValueChangeFinished()
                    },
                    steps = if (animateToNearestStep) 0 else steps,
                    valueRange = min.toFloat()..max.toFloat(),
                    interactionSource = interactionSource,
                    thumb = {
                        SliderDefaults.Thumb(
                            interactionSource = interactionSource,
                            enabled = enabled,
                            thumbSize = thumbSize,
                        )
                    },
                    track = { ExpressiveTrack(sliderState = it, enabled = enabled, trackIcon = trackIcon) },
                )
                if (showValue) {
                    Spacer(modifier = Modifier.width(iconSpacing))
                    Text(
                        text = value.toString(),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpressiveTrack(
    sliderState: SliderState,
    enabled: Boolean,
    trackIcon: ImageVector?,
) {
    // neutral inactive track like the system volume sliders,
    // instead of the expressive default (secondaryContainer)
    val colors = SliderDefaults.colors(inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest)
    val activeColor = if (enabled) colors.activeTrackColor else colors.disabledActiveTrackColor
    val inactiveColor = if (enabled) colors.inactiveTrackColor else colors.disabledInactiveTrackColor
    // a lambda so state reads happen in the draw/placement phase, not composition
    val fraction = {
        (sliderState.value - sliderState.valueRange.start) /
            (sliderState.valueRange.endInclusive - sliderState.valueRange.start)
    }
    var trackWidth by remember { mutableIntStateOf(0) }
    Box(contentAlignment = Alignment.CenterStart) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(trackHeight)
                .onSizeChanged { trackWidth = it.width },
        ) {
            drawTrack(fraction(), activeColor, inactiveColor, drawStopDot = trackIcon == null)
        }
        if (trackIcon != null && trackWidth > 0) {
            val density = LocalDensity.current
            // derived so the tint only recomposes when the snap state flips
            val snapped by remember(trackWidth, density) {
                derivedStateOf { density.isIconSnapped(trackWidth, fraction()) }
            }
            val tint =
                if (snapped) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            Icon(
                imageVector = trackIcon,
                contentDescription = null,
                modifier =
                Modifier
                    .offset {
                        val x =
                            if (isIconSnapped(trackWidth, fraction())) {
                                // ride along just inside the active track, next to the handle
                                (activeTrackEnd(trackWidth, fraction()) - snappedIconGap.toPx() - trackIconSize.toPx())
                                    .coerceAtLeast(0f)
                            } else {
                                defaultIconX(trackWidth)
                            }
                        IntOffset(x.roundToInt(), 0)
                    }.size(trackIconSize),
                tint = tint.copy(alpha = if (enabled) 1f else DISABLED_CONTENT_ALPHA),
            )
        }
    }
}

/** Where the active track visually ends: handle position minus the gap */
private fun Density.activeTrackEnd(
    trackWidth: Int,
    fraction: Float,
) = fraction * trackWidth - thumbTrackGap.toPx()

/** The icon's resting position at the trailing edge of the track */
private fun Density.defaultIconX(trackWidth: Int) = trackWidth - trackIconEndPadding.toPx() - trackIconSize.toPx()

private fun Density.isIconSnapped(
    trackWidth: Int,
    fraction: Float,
) = activeTrackEnd(trackWidth, fraction) >= defaultIconX(trackWidth) - iconSnapMargin.toPx()

private fun DrawScope.drawTrack(
    fraction: Float,
    activeColor: Color,
    inactiveColor: Color,
    drawStopDot: Boolean,
) {
    val gap = thumbTrackGap.toPx()
    val outerCorner = trackOuterCorner.toPx()
    val thumbX = fraction * size.width
    scale(
        scaleX = if (layoutDirection == LayoutDirection.Rtl) -1f else 1f,
        scaleY = 1f,
    ) {
        val activeEnd = thumbX - gap
        if (activeEnd > 0f) {
            // outer corner shrinks as the segment collapses, like the system volume slider
            drawTrackSegment(0f, activeEnd, minOf(outerCorner, activeEnd / 2f), activeColor, outerOnLeft = true)
        }
        val inactiveStart = thumbX + gap
        if (inactiveStart < size.width) {
            drawTrackSegment(
                inactiveStart,
                size.width,
                minOf(outerCorner, (size.width - inactiveStart) / 2f),
                inactiveColor,
                outerOnLeft = false,
            )
        }
        if (drawStopDot) {
            val dotX = size.width - size.height / 2f
            if (thumbX + gap < dotX) {
                drawCircle(activeColor, radius = stopIndicatorRadius.toPx(), center = Offset(dotX, size.height / 2f))
            }
        }
    }
}

private fun DrawScope.drawTrackSegment(
    left: Float,
    right: Float,
    outerRadius: Float,
    color: Color,
    outerOnLeft: Boolean,
) {
    val outer = CornerRadius(outerRadius)
    val inner = CornerRadius(trackInnerCorner.toPx())
    drawPath(
        Path().apply {
            addRoundRect(
                RoundRect(
                    left = left,
                    top = 0f,
                    right = right,
                    bottom = size.height,
                    topLeftCornerRadius = if (outerOnLeft) outer else inner,
                    bottomLeftCornerRadius = if (outerOnLeft) outer else inner,
                    topRightCornerRadius = if (outerOnLeft) inner else outer,
                    bottomRightCornerRadius = if (outerOnLeft) inner else outer,
                ),
            )
        },
        color = color,
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
