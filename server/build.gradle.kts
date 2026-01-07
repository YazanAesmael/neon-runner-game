// server/build.gradle.kts

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
}

group = "com.app.multiplatform"
version = "1.0.0"

application {
    mainClass.set("com.app.multiplatform.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=true")
}

dependencies {
    // The server needs the shared logic too!
    implementation(projects.shared)

    // error here
    implementation(libs.logback)
    implementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.core)
}