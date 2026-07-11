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
import com.android.build.api.dsl.ManagedVirtualDevice
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidTest)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.apps.adrcotfas.goodtime.appkilltest"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        minSdk = 28
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":androidApp"

    // Run the tests in their OWN process, not the app's. Without this the instrumentation
    // lives inside the app process, so killing the app (the whole point here) kills the
    // test runner too. Same mechanism macrobenchmark uses.
    experimentalProperties["android.experimental.self-instrumenting"] = true

    // Match the app's flavors; the tests exercise the (default) google flavor.
    flavorDimensions += "distribution"
    productFlavors {
        create("google") { dimension = "distribution" }
        create("fdroid") { dimension = "distribution" }
    }

    // Managed emulator so the suite can run headless in CI without a physical device.
    // AOSP image (has no Play services, but includes SystemUI for notification assertions).
    testOptions.managedDevices.allDevices {
        create<ManagedVirtualDevice>("pixel6Api35") {
            device = "Pixel 6"
            apiLevel = 35
            systemImageSource = "aosp"
        }
    }
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.runner)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.lifecycle.runtime.ktx)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
