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
package com.apps.adrcotfas.goodtime.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.room.RoomDatabase
import com.apps.adrcotfas.goodtime.data.local.ProductivityDatabase
import com.apps.adrcotfas.goodtime.data.local.getDatabaseBuilder
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.module.Module
import org.koin.core.qualifier.named
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalForeignApi::class)
actual val platformModule: Module =
    module {
        single<RoomDatabase.Builder<ProductivityDatabase>> { getDatabaseBuilder() }

        single<DataStore<Preferences>>(named(SETTINGS_NAME)) {
            getDataStore(
                producePath = {
                    val documentDirectory: NSURL? =
                        NSFileManager.defaultManager.URLForDirectory(
                            directory = NSDocumentDirectory,
                            inDomain = NSUserDomainMask,
                            appropriateForURL = null,
                            create = false,
                            error = null,
                        )
                    requireNotNull(documentDirectory).path + "/$SETTINGS_FILE_NAME"
                },
            )
        }
    }

@OptIn(ExperimentalNativeApi::class)
actual fun isDebug(): Boolean = Platform.isDebugBinary
