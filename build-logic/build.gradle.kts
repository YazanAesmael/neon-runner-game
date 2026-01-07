// build-logic/build.gradle.kts

plugins {
    `kotlin-dsl`
}

dependencies {
    implementation(libs.agp.gradle)
    implementation(libs.kotlin.gradle)
    implementation(libs.compose.gradle)
    implementation(libs.ksp.gradle)
    implementation(libs.kotlin.compose.compiler.plugin)
}