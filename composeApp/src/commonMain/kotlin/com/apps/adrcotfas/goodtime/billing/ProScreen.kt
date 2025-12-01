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
package com.apps.adrcotfas.goodtime.billing

import androidx.compose.runtime.Composable

/**
 * Pro/billing screen composable.
 * Has different implementations for different flavors:
 * - Google Play: Full billing implementation with Play Billing Library
 * - F-Droid: Simplified version or message about how to support
 * - iOS: App Store billing (future)
 */
@Composable
expect fun ProScreen(onNavigateBack: () -> Unit)
