import com.mikepenz.aboutlibraries.plugin.DuplicateMode
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.mikepenz.aboutlibraries)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = false
            export(libs.touchlab.kermit.simple)
        }
    }

    sourceSets {
        all {
            languageSettings.apply {
                optIn("kotlin.RequiresOptIn")
                optIn("kotlinx.coroutines.ExperimentalCoroutinesApi")
                optIn("kotlin.time.ExperimentalTime")
            }
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
            implementation(libs.koin.androidx.workmanager)

            // Android-specific libraries
            implementation(libs.androidx.documentfile)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.paging.runtime)
            implementation(libs.androidx.paging.compose)
            implementation(libs.androidx.media)
            implementation(libs.mikepenz.aboutlibraries.core)
            implementation(libs.mikepenz.aboutlibraries.compose)
            implementation(libs.devsrsouza.compose.icons.eva)
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
            implementation(libs.androidchart)
            implementation(libs.acra.mail)
            implementation(libs.acra.notification)
            implementation(libs.lottie.compose)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.work.runtime.ktx)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            // Shared business logic dependencies
            implementation(libs.koin.core)
            api(libs.coroutines.core)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.paging)
            implementation(libs.androidx.datastore.preferences.core)
            api(libs.okio)
            api(libs.kotlinx.serialization)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.datetime)
            implementation(libs.androidx.lifecycle.viewmodel)
            api(libs.touchlab.kermit)
        }

        commonTest.dependencies {
            implementation(libs.bundles.shared.commonTest)
        }

        androidUnitTest.dependencies {
            implementation(libs.bundles.shared.androidTest)
        }

        androidInstrumentedTest.dependencies {
            implementation(libs.bundles.shared.androidTest)
        }

        iosMain.dependencies {
        }
    }

    // https://kotlinlang.org/docs/multiplatform-expect-actual.html#expected-and-actual-classes
    // To suppress this warning about usage of expected and actual classes
    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

android {
    val packageName = "com.apps.adrcotfas.goodtime"
    namespace = packageName
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = packageName
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 345
        versionName = "3.0.14"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
                "proguard-rules.pro"
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

    sourceSets {
        named("androidTest") {
            assets.srcDirs(files("$projectDir/schemas"))
        }
    }

    aboutLibraries {
        collect.configPath = file("config")
        library.duplicationMode = DuplicateMode.MERGE
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(platform(libs.androidx.compose.bom))

    // Google-specific (flavor-specific dependencies)
    add("googleImplementation", libs.billing.ktx)
    add("googleImplementation", libs.app.update.ktx)
    add("googleImplementation", libs.review.ktx)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

tasks.register("testClasses") { }

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}
