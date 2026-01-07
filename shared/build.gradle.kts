// shared/build.gradle.kts
plugins {
    alias(libs.plugins.androidLibrary)
    id("convention.kmp")
    id("convention.inject")
    alias(libs.plugins.kotlinxSerialization) // <--- THIS WAS THE FIX
}

android {
    namespace = "com.app.multiplatform.shared"
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)

            // Ktor Networking
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ktor.client.logging)
        }
    }
}