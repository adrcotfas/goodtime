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
package com.apps.adrcotfas.goodtime.testutil

import kotlinx.coroutines.delay

/**
 * Generic retry utility for flaky tests
 * @param maxRetries Maximum number of retry attempts (default: 3)
 * @param delayBetweenRetries Delay in milliseconds between retry attempts (default: 100ms)
 * @param testBlock The test logic to execute
 */
suspend fun retryTest(
    maxRetries: Int = 3,
    delayBetweenRetries: Long = 100,
    testBlock: suspend () -> Unit,
) {
    var lastException: Throwable? = null

    repeat(maxRetries) { attempt ->
        try {
            testBlock()
            return // Test passed, exit early
        } catch (e: Throwable) {
            lastException = e
            if (attempt < maxRetries - 1) {
                println("Test failed on attempt ${attempt + 1}/$maxRetries. Retrying...")
                if (delayBetweenRetries > 0) {
                    delay(delayBetweenRetries)
                }
            }
        }
    }

    // If we get here, all retries failed
    throw lastException ?: AssertionError("Test failed after $maxRetries attempts")
}
