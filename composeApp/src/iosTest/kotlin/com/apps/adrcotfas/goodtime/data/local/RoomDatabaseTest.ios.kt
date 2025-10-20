package com.apps.adrcotfas.goodtime.data.local

import androidx.room.RoomDatabase
import com.apps.adrcotfas.goodtime.RobolectricTest

actual abstract class RoomDatabaseTest actual constructor() :
    RobolectricTest() {
    actual fun getInMemoryDatabaseBuilder(): RoomDatabase.Builder<ProductivityDatabase> {
        TODO("Not yet implemented")
    }
}