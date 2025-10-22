import org.jetbrains.kotlin.konan.target.KonanTarget

val host = org.jetbrains.kotlin.konan.target.HostManager.host

plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    when (host) {
        KonanTarget.MACOS_X64 -> macosX64("macosX64") {
            binaries.executable {
                entryPoint = "io.chornge.kmpli.main"
                baseName = "kmpli"
            }
        }

        KonanTarget.MACOS_ARM64 -> macosArm64("macosArm64") {
            binaries.executable {
                entryPoint = "io.chornge.kmpli.main"
                baseName = "kmpli"
            }
        }

        KonanTarget.LINUX_X64 -> linuxX64("linuxX64") {
            binaries.executable {
                entryPoint = "io.chornge.kmpli.main"
                baseName = "kmpli"
            }
        }

        KonanTarget.LINUX_ARM64 -> linuxArm64("linuxArm64") {
            binaries.executable {
                entryPoint = "io.chornge.kmpli.main"
                baseName = "kmpli"
            }
        }

        KonanTarget.MINGW_X64 -> mingwX64("mingwX64") {
            binaries.executable {
                entryPoint = "io.chornge.kmpli.main"
                baseName = "kmpli"
            }
        }

        else -> throw GradleException("Unsupported host: $host")
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.ktor.client.core)
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(libs.kotlin.test)
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.ktor.client.curl)
                implementation(libs.squareup.okio)
            }
        }

        when (host) {
            KonanTarget.MACOS_X64 -> sourceSets.getByName("macosX64Main").dependsOn(nativeMain)
            KonanTarget.MACOS_ARM64 -> sourceSets.getByName("macosArm64Main").dependsOn(nativeMain)
            KonanTarget.LINUX_X64 -> sourceSets.getByName("linuxX64Main").dependsOn(nativeMain)
            KonanTarget.LINUX_ARM64 -> sourceSets.getByName("linuxArm64Main").dependsOn(nativeMain)
            KonanTarget.MINGW_X64 -> sourceSets.getByName("mingwX64Main").dependsOn(nativeMain)
            else -> {} // Unsupported host
        }
    }
}

val nativeTargets = listOf(
    "MacosX64",
    "MacosArm64",
    "LinuxX64",
    "LinuxArm64",
    "MingwX64"
)

tasks.register<Copy>("copyBinariesToDist") {
    group = "distribution"
    description = "Copy native binaries to build/dist"

    nativeTargets.forEach { target ->
        val binaryDir = layout.buildDirectory.dir("bin/$target/releaseExecutable")
        from(binaryDir) {
            include("*")
            rename { fileName ->
                if (target.lowercase().startsWith("mingw")) fileName else fileName.removeSuffix(".kexe")
            }
        }
        into(layout.buildDirectory.dir("dist/$target"))
    }
}
