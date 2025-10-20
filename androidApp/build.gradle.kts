import com.mikepenz.aboutlibraries.plugin.DuplicateMode

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.mikepenz.aboutlibraries)
}

android {
    val packageName = "com.apps.adrcotfas.goodtime"
    namespace = packageName
    compileSdk =
        libs.versions.android.compileSdk
            .get()
            .toInt()
    defaultConfig {
        applicationId = packageName
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()
        targetSdk =
            libs.versions.android.targetSdk
                .get()
                .toInt()
        versionCode = 345
        versionName = "3.0.14"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    flavorDimensions += "distribution"
    productFlavors {
        create("google") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_FDROID", "false")
        }
        create("fdroid") {
            dimension = "distribution"
            buildConfigField("boolean", "IS_FDROID", "true")
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
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.add("-Xannotation-default-target=param-property")
        }
    }

    java {
        toolchain {
            languageVersion = JavaLanguageVersion.of(17)
        }
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    aboutLibraries {
        collect.configPath = file("config")
        library.duplicationMode = DuplicateMode.MERGE
    }
}

dependencies {
    implementation(libs.androidx.documentfile)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(projects.shared)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.kotlinx.datetime)
    implementation(libs.androidx.foundation)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)
    implementation(libs.androidx.media)
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.mikepenz.aboutlibraries.core)
    implementation(libs.mikepenz.aboutlibraries.compose)
    implementation(libs.devsrsouza.compose.icons.eva)

    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.androidchart)

    implementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.ui.tooling.preview)

    implementation(libs.acra.mail)
    implementation(libs.acra.notification)

    implementation(libs.lottie.compose)
    implementation(libs.androidx.core.splashscreen)

    implementation(libs.work.runtime.ktx)

    "googleImplementation"(libs.billing.ktx)
    "googleImplementation"(libs.app.update.ktx)
    "googleImplementation"(libs.review.ktx)
}
