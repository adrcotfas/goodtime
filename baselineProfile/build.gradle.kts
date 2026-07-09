import com.android.build.api.dsl.ManagedVirtualDevice
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidTest)
    alias(libs.plugins.androidxBaselineProfile)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.apps.adrcotfas.goodtime.baselineprofile"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // Baseline profile generation requires API 28+.
        minSdk = 28
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    targetProjectPath = ":androidApp"

    // Match the app's flavors so the plugin knows which variant to profile.
    flavorDimensions += "distribution"
    productFlavors {
        create("google") { dimension = "distribution" }
        create("fdroid") { dimension = "distribution" }
    }

    // A managed emulator so `generateBaselineProfile` can run without a physical device.
    testOptions.managedDevices.allDevices {
        create<ManagedVirtualDevice>("pixel6Api34") {
            device = "Pixel 6"
            apiLevel = 34
            systemImageSource = "aosp"
        }
    }
}

baselineProfile {
    managedDevices += "pixel6Api34"
    useConnectedDevices = false
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
