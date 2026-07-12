import com.diffplug.gradle.spotless.SpotlessTask

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidKotlinMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlinx.serialization) apply false
    alias(libs.plugins.mikepenz.aboutlibraries) apply false
    alias(libs.plugins.androidTest) apply false
    alias(libs.plugins.androidxBaselineProfile) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.kmp.app.version)
}

subprojects {
    apply(plugin = rootProject.libs.plugins.spotless.get().pluginId)

    configure<com.diffplug.gradle.spotless.SpotlessExtension> {
        kotlin {
            target("**/*.kt")
            targetExclude("${layout.buildDirectory}/**/*.kt")
            ktlint().editorConfigOverride(
                mapOf(
                    // the GPL license header is a toplevel KDoc in every file
                    "ktlint_standard_kdoc" to "disabled",
                ),
            )
            licenseHeaderFile(rootProject.file(".spotless/license.kt"))
            trimTrailingWhitespace()
            endWithNewline()
        }
        format("xml") {
            target("**/*.xml")
            targetExclude(listOf("**/build/**/*.xml", "google/**/*.xml"))
            licenseHeaderFile(rootProject.file(".spotless/license.xml"), "(<[^!?])")
            trimTrailingWhitespace()
            endWithNewline()
        }
        kotlinGradle {
            target("*.gradle.kts")
            ktlint()
        }
        tasks.withType<SpotlessTask>().configureEach {
            notCompatibleWithConfigurationCache("https://github.com/diffplug/spotless/issues/987")
        }
    }
}