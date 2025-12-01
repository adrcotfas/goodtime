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

import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController

/**
 * Extension function for NavController to safely pop the back stack.
 * Only pops if the current back stack entry is in RESUMED state.
 * This prevents crashes when trying to navigate while a screen is not fully resumed.
 *
 * @return true if the back stack was popped, false otherwise
 */
fun NavController.popBackStack2(): Boolean {
    if (this.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
        return this.popBackStack()
    }
    return false
}
