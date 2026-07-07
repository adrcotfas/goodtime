import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKotlinMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.room)
    alias(libs.plugins.ksp)
    alias(libs.plugins.mikepenz.aboutlibraries)
}

kotlin {
    androidLibrary {
        namespace = "com.apps.adrcotfas.goodtime"
        compileSdk =
            libs.versions.android.compileSdk
                .get()
                .toInt()
        minSdk =
            libs.versions.android.minSdk
                .get()
                .toInt()

        androidResources.enable = true

        withHostTestBuilder {
        }.configure {
            isIncludeAndroidResources = true
        }

        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(JvmTarget.JVM_17)
                }
            }
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
            export(libs.touchlab.kermit.simple)
        }
    }

    // The RevenueCat klib embeds a Swift library search path from its publisher's machine;
    // point the simulator test binary at the local toolchain so swiftCompatibility* resolve.
    iosSimulatorArm64().binaries.configureEach {
        if (this is org.jetbrains.kotlin.gradle.plugin.mpp.TestExecutable) {
            val developerDir = System.getenv("DEVELOPER_DIR") ?: "/Applications/Xcode.app/Contents/Developer"
            linkerOpts("-L$developerDir/Toolchains/XcodeDefault.xctoolchain/usr/lib/swift/iphonesimulator")
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
            api(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.koin.androidx.workmanager)

            implementation(libs.androidx.documentfile)
            implementation(libs.androidx.lifecycle.runtime.ktx)
            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.media)

            implementation(libs.androidchart)
            implementation(libs.androidx.core.splashscreen)
            implementation(libs.work.runtime.ktx)
        }

        commonMain.dependencies {
            // api: the androidApp entry module compiles against these through this library
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.materialIconsExtended)
            api(compose.ui)
            api(compose.components.resources)
            api(compose.components.uiToolingPreview)
            implementation(libs.devsrsouza.compose.icons.eva)
            implementation(libs.navigation.compose)
            implementation(libs.compottie)
            implementation(libs.compottie.dot)
            implementation(libs.ui.backhandler)
            implementation(libs.mikepenz.aboutlibraries.core)
            implementation(libs.mikepenz.aboutlibraries.compose)
            api(libs.koin.core)
            api(libs.koin.compose)
            api(libs.koin.compose.viewmodel)
            api(libs.coroutines.core)
            implementation(libs.androidx.room.runtime)
            implementation(libs.androidx.room.paging)
            implementation(libs.androidx.datastore.preferences.core)
            api(libs.okio)
            api(libs.kotlinx.serialization)
            implementation(libs.kotlinx.collections.immutable)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.datetime.names)
            api(libs.androidx.lifecycle.viewmodel)
            api(libs.touchlab.kermit)
            implementation(libs.vico.compose)
            implementation(libs.vico.compose.m3)
            implementation(libs.androidx.paging.runtime)
            implementation(libs.androidx.paging.compose)
        }

        commonTest.dependencies {
            implementation(libs.bundles.shared.commonTest)
        }

        getByName("androidHostTest").dependencies {
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

compose.resources {
    // the androidApp entry module references generated resource accessors
    publicResClass = true
}

aboutLibraries {
    collect.configPath = file("config")
    export {
        outputFile = file("src/commonMain/composeResources/files/aboutlibraries.json")
        prettyPrint = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    add("kspAndroid", libs.androidx.room.compiler)
    add("kspIosArm64", libs.androidx.room.compiler)
    add("kspIosSimulatorArm64", libs.androidx.room.compiler)
}

tasks.named("exportLibraryDefinitions") {
    dependsOn("copyNonXmlValueResourcesForCommonMain")
}

// aboutlibraries.json is gitignored, so it must be (re)generated before packaging resources
tasks.matching { it.name == "preBuild" || it.name == "prepareComposeResourcesTaskForCommonMain" }.configureEach {
    dependsOn("exportLibraryDefinitions")
}

java {
    toolchain {
        // 21: Robolectric's SDK 36 sandbox requires Java 21; bytecode still targets 17 (jvmTarget)
        languageVersion = JavaLanguageVersion.of(21)
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
