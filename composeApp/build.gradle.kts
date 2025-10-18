import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.compose.hotReload)
}

kotlin {
    jvm()

    macosX64("macosX64") {
        binaries.executable {
            entryPoint = "io.chornge.kmpli.KmpliKt"
        }
    }
    macosArm64("macosArm64") {
        binaries.executable {
            entryPoint = "io.chornge.kmpli.KmpliKt"
        }
    }
    linuxX64("linuxX64") {
        binaries.executable {
            entryPoint = "io.chornge.kmpli.KmpliKt"
        }
    }
    linuxArm64("linuxArm64") {
        binaries.executable {
            entryPoint = "io.chornge.kmpli.KmpliKt"
        }
    }
    mingwX64("mingwX64") {
        binaries.executable {
            entryPoint = "io.chornge.kmpli.KmpliKt"
        }
    }

    // windowsArm64() // Limited support (experimental)

    sourceSets {
        commonMain.dependencies {

        }
        commonTest.dependencies {
            implementation(libs.junit)
            implementation(libs.kotlin.test)
        }
        jvmMain.dependencies {
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(compose.desktop.currentOs)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.runtime)
            implementation(compose.ui)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.junit.jupiter)
            implementation(libs.ktor.client.cio)
            implementation(libs.ktor.client.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.coroutines.swing)
            implementation(libs.kotlinx.serialization.json)
            runtimeOnly(libs.logback.classic)
        }

        // Ensure native targets are registered
        val macosX64Main by getting
        val macosArm64Main by getting
        val linuxX64Main by getting
        val mingwX64Main by getting
    }
}

// JVM builds only
compose.desktop {
    application {
        mainClass = "io.chornge.kmpli.KmpliKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "io.chornge.kmpli"
            packageVersion = libs.versions.appVersion.get()
        }
    }
}

val nativeTargets = listOf("MacosX64", "MacosArm64", "LinuxX64", "LinuxArm64", "MingwX64")

// Build all native binaries
tasks.register("buildAllNative") {
    group = "build"
    description = "Build all Kotlin/Native executables"
    dependsOn(nativeTargets.map { "linkReleaseExecutable$it" })
}

// Copy all binaries to `dist` folder
tasks.register<Copy>("copyBinariesToDist") {
    group = "distribution"
    description = "Copy all native binaries to the build/dist directory"

    val distDir = layout.buildDirectory.dir("dist")
    into(distDir)

    nativeTargets.forEach { target ->
        val binaryDir = layout.buildDirectory.dir("bin/$target/releaseExecutable")
        from(binaryDir) {
            include("*")
            rename { fileName ->
                if (target.lowercase().startsWith("mingw")) fileName
                else fileName.removeSuffix(".kexe")
            }
        }
    }
}

// Optional: Build all + Copy
tasks.register("prepareDist") {
    group = "distribution"
    description = "Build all native binaries and copy them to dist"
    dependsOn("buildAllNative", "copyBinariesToDist")
}
