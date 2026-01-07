// composeApp/build.gradle.kts

plugins {
    alias(libs.plugins.androidApplication)
    id("convention.kmp")
    id("convention.compose")
    id("convention.inject")
     id("org.jetbrains.compose")
}

android {
    namespace = "com.app.multiplatform"

    defaultConfig {
        applicationId = "com.app.multiplatform"
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared)
        }

        val jvmMain by getting {
            dependencies {
                // This downloads the native graphics engine (Skiko) for the specific OS
                implementation(compose.desktop.currentOs)
            }
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

// Configuration for Desktop Application
compose.desktop {
    application {
        mainClass = "com.app.multiplatform.MainKt"
        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )
            packageName = "com.app.multiplatform"
            packageVersion = "1.0.0"
        }
    }
}