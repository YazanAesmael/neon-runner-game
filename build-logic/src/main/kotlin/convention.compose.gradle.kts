// build-logic/src/main/kotlin/convention.compose.gradle.kts

import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

val libs = the<VersionCatalogsExtension>().named("libs")

configure<KotlinMultiplatformExtension> {
    sourceSets.apply {
        commonMain.dependencies {
            implementation(libs.findLibrary("compose-runtime").get())
            implementation(libs.findLibrary("compose-foundation").get())
            implementation(libs.findLibrary("compose-material3").get())
            implementation(libs.findLibrary("compose-ui").get())
            implementation(libs.findLibrary("compose-components-resources").get())
            implementation(libs.findLibrary("compose-ui-tooling-preview").get())
            implementation(libs.findLibrary("androidx-lifecycle-viewmodel").get())
            implementation(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
        }

        val androidMain by getting {
            dependencies {
                implementation(libs.findLibrary("androidx-activity-compose").get())
            }
        }

        val jvmMain by getting {
            dependencies {
                // Skiko/Desktop Native is handled in the final app module now
                implementation(libs.findLibrary("kotlinx-coroutines-swing").get())
            }
        }
    }
}