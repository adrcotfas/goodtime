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
package com.apps.adrcotfas.goodtime.onboarding

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.apps.adrcotfas.goodtime.R
import com.apps.adrcotfas.goodtime.R as Mr

data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val description1: Int,
    @StringRes val description2: Int,
    @DrawableRes val image: Int,
) {
    companion object {
        val pages =
            listOf(
                OnboardingPage(
                    title = Mr.string.intro1_title,
                    description1 = Mr.string.intro1_desc1,
                    description2 = Mr.string.intro1_desc2,
                    image = R.drawable.intro1,
                ),
                OnboardingPage(
                    title = Mr.string.intro2_title,
                    description1 = Mr.string.intro2_desc1,
                    description2 = Mr.string.intro2_desc2,
                    image = R.drawable.intro2,
                ),
                OnboardingPage(
                    title = Mr.string.intro3_title,
                    description1 = Mr.string.intro3_desc1,
                    description2 = Mr.string.intro3_desc2,
                    image = R.drawable.intro3,
                ),
            )
    }
}
