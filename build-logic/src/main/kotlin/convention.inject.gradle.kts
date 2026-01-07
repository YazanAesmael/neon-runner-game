// build-logic/src/main/kotlin/convention.inject.gradle.kts
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("com.google.devtools.ksp")
}

val libs = the<VersionCatalogsExtension>().named("libs")

configure<KotlinMultiplatformExtension> {
    sourceSets.apply {
        commonMain.dependencies {
            implementation(libs.findLibrary("kotlin-inject-runtime").get())
        }
    }
}

dependencies {
    val kspDependency = libs.findLibrary("kotlin-inject-compiler-ksp").get()

    add("kspCommonMainMetadata", kspDependency)
    add("kspAndroid", kspDependency)
    add("kspIosArm64", kspDependency)
    add("kspIosSimulatorArm64", kspDependency)
    add("kspJvm", kspDependency)
    add("kspJs", kspDependency)
    add("kspWasmJs", kspDependency)
}