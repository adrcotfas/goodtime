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
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
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
            implementation(libs.koin.androidx.workmanager)

            implementation(libs.androidx.documentfile)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.media)

            implementation(libs.androidchart)
            implementation(libs.acra.mail)
            implementation(libs.acra.notification)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.work.runtime.ktx)
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.devsrsouza.compose.icons.eva)
            implementation(libs.navigation.compose)
            implementation(libs.compottie)
            implementation(libs.compottie.dot)
            implementation(libs.ui.backhandler)
            implementation(libs.mikepenz.aboutlibraries.core)
            implementation(libs.mikepenz.aboutlibraries.compose)
            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.compose.viewmodel)
            api(libs.coroutines.core)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.paging)
            implementation(libs.androidx.datastore.preferences.core)
            api(libs.okio)
            api(libs.kotlinx.serialization)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.datetime.names)
            implementation(libs.androidx.lifecycle.viewmodel)
            api(libs.touchlab.kermit)
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
            implementation(libs.androidx.paging.runtime)
            implementation(libs.androidx.paging.compose)
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
            api(libs.touchlab.kermit.simple)
            api(libs.androidx.sqlite.bundled)
            implementation(libs.purchases.core)
            implementation(libs.purchases.ui)
        }

        iosTest.dependencies {
            implementation(libs.androidx.room.testing)
        }

        named { it.lowercase().startsWith("ios") }.configureEach {
            languageSettings {
                optIn("kotlinx.cinterop.ExperimentalForeignApi")
            }
        }
    }

    @OptIn(ExperimentalKotlinGradlePluginApi::class)
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
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
                buildConfigField("boolean", "IS_FDROID", "false")
                // Debug/test vs release/prod keys (same for now; replace as needed).
                buildConfigField("String", "REVENUECAT_API_KEY_DEBUG", "\"test_mIjxZUKugfUbtAZNjpvxaWynKcD\"")
                buildConfigField("String", "REVENUECAT_API_KEY_RELEASE", "\"goog_WJACaArOgxIPytSUVHDOgwjTZjN\"")
            }
            create("fdroid") {
                dimension = "distribution"
                buildConfigField("boolean", "IS_FDROID", "true")
                buildConfigField("String", "REVENUECAT_API_KEY_DEBUG", "\"\"")
                buildConfigField("String", "REVENUECAT_API_KEY_RELEASE", "\"\"")
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
            assets.srcDirs(files("$projectDir/schemas"))
        }
    }

    androidResources {
        @Suppress("UnstableApiUsage")
        generateLocaleConfig = true
    }

    aboutLibraries {
        collect.configPath = file("config")
        export {
            outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
            prettyPrint = true
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // for the google flavor
    add("googleImplementation", libs.app.update.ktx)
    add("googleImplementation", libs.review.ktx)
    add("googleImplementation", libs.purchases.core)
    add("googleImplementation", libs.purchases.ui)
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosX64", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

tasks.register("testClasses") { }

tasks.named("exportLibraryDefinitions") {
    dependsOn("copyNonXmlValueResourcesForCommonMain")
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

// Workaround for androidx.paging alpha version not having full iOS support
configurations.all {
    if (name.contains("ios", ignoreCase = true)) {
        resolutionStrategy {
            eachDependency {
                // Replace ktx libraries with their base counterparts for iOS compatibility
                when {
                    requested.group == "androidx.paging" && requested.name == "paging-common-ktx" -> {
                        useTarget("${requested.group}:paging-common:${requested.version}")
                        because("paging-common-ktx doesn't support iOS, using paging-common instead")
                    }

                    requested.group == "org.jetbrains.kotlinx" && requested.name == "kotlinx-coroutines-android" -> {
                        useTarget("org.jetbrains.kotlinx:kotlinx-coroutines-core:${requested.version}")
                        because("kotlinx-coroutines-android doesn't support iOS, using core instead")
                    }

                    requested.group == "androidx.lifecycle" && requested.name == "lifecycle-runtime-ktx" -> {
                        useTarget("${requested.group}:lifecycle-runtime:${requested.version}")
                        because("lifecycle-runtime-ktx doesn't support iOS, using lifecycle-runtime instead")
                    }

                    requested.group == "androidx.lifecycle" && requested.name == "lifecycle-livedata-ktx" -> {
                        useTarget("${requested.group}:lifecycle-livedata:${requested.version}")
                        because("lifecycle-livedata-ktx doesn't support iOS, using lifecycle-livedata instead")
                    }

                    requested.group == "androidx.lifecycle" && requested.name == "lifecycle-livedata-core-ktx" -> {
                        useTarget("${requested.group}:lifecycle-livedata-core:${requested.version}")
                        because("lifecycle-livedata-core-ktx doesn't support iOS, using lifecycle-livedata-core instead")
                    }

                    requested.group == "androidx.lifecycle" && requested.name == "lifecycle-viewmodel-ktx" -> {
                        useTarget("${requested.group}:lifecycle-viewmodel:${requested.version}")
                        because("lifecycle-viewmodel-ktx doesn't support iOS, using lifecycle-viewmodel instead")
                    }
                }
            }
        }
    }
}

// TODO: re-enable after we're out of TestFlight
// tasks.register("syncIosVersion") {
//    val configFile = file("$rootDir/iosApp/Configuration/Config.xcconfig")
//
//    val versionName = libs.versions.appVersionName.get()
//    val versionCode = libs.versions.appVersionCode.get()
//
//    inputs.property("versionName", versionName)
//    inputs.property("versionCode", versionCode)
//    outputs.file(configFile)
//
//    doLast {
//        if (!configFile.exists()) {
//            error("Config.xcconfig not found at: ${configFile.path}")
//        }
//
//        val updatedContent =
//            configFile
//                .readText()
//                .replace(Regex("""CURRENT_PROJECT_VERSION\s*=\s*\S+"""), "CURRENT_PROJECT_VERSION=$versionCode")
//                .replace(Regex("""MARKETING_VERSION\s*=\s*\S+"""), "MARKETING_VERSION=$versionName")
//
//        configFile.writeText(updatedContent)
//
//        println("✓ Updated Config.xcconfig: $versionName ($versionCode)")
//    }
// }
//
// tasks.configureEach {
//    if (name == "checkCanSyncComposeResourcesForIos") {
//        dependsOn("syncIosVersion")
//    }
// }
