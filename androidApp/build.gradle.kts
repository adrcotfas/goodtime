import com.android.build.api.variant.BuildConfigField
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

android {
    namespace = "com.apps.adrcotfas.goodtime.app"
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()

    defaultConfig {
        applicationId = "com.apps.adrcotfas.goodtime"
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode =
            libs.versions.appVersionCode
                .get()
                .toInt()
        versionName = libs.versions.appVersionName.get()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        flavorDimensions += "distribution"
        productFlavors {
            create("google") {
                dimension = "distribution"
            }
            create("fdroid") {
                dimension = "distribution"
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Google Drive API dependencies have conflicting META-INF files
            excludes += "/META-INF/INDEX.LIST"
            excludes += "/META-INF/DEPENDENCIES"
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug")
        }
        getByName("debug") {
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    sourceSets {
        named("androidTest") {
            assets.srcDirs(files("$rootDir/shared/schemas"))
        }
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }
}

androidComponents {
    onVariants { variant ->
        val isFdroid = variant.flavorName == "fdroid"
        // Debug/test vs release/prod RevenueCat keys (same for now; replace as needed).
        val revenueCatKey = if (isFdroid) "" else "goog_WJACaArOgxIPytSUVHDOgwjTZjN"
        variant.buildConfigFields?.apply {
            put("IS_FDROID", BuildConfigField("boolean", isFdroid.toString(), null))
            put("REVENUECAT_API_KEY_DEBUG", BuildConfigField("String", "\"$revenueCatKey\"", null))
            put("REVENUECAT_API_KEY_RELEASE", BuildConfigField("String", "\"$revenueCatKey\"", null))
        }
    }
}

dependencies {
    implementation(projects.shared)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.work.runtime.ktx)
    implementation(libs.acra.mail)
    implementation(libs.acra.notification)

    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // Google Play distribution
    add("googleImplementation", libs.app.update.ktx)
    add("googleImplementation", libs.review.ktx)
    add("googleImplementation", libs.purchases.core)
    add("googleImplementation", libs.purchases.ui)
    add("googleImplementation", libs.google.play.auth)
    add("googleImplementation", libs.google.play.auth.credentials)
    add("googleImplementation", libs.google.api.client)
    add("googleImplementation", libs.google.drive)
    add("googleImplementation", libs.google.id)
    add("googleImplementation", libs.coroutines.play.services)

    androidTestImplementation(libs.bundles.shared.androidTest)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// remove unused RevenueCat modules
configurations.configureEach {
    exclude(group = "com.revenuecat.purchases", module = "purchases-store-amazon")
    exclude(group = "com.amazon.device", module = "amazon-appstore-sdk")
}
