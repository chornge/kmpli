plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    macosX64("macosX64") {
        binaries.executable {
            entryPoint = "io.chornge.kmpli.main"
            baseName = "kmpli"
        }
    }
    linuxX64("linuxX64") {
        binaries.executable {
            entryPoint = "io.chornge.kmpli.main"
            baseName = "kmpli"
        }
    }
    mingwX64("mingwX64") {
        binaries.executable {
            entryPoint = "io.chornge.kmpli.main"
            baseName = "kmpli"
        }
    }
    /*windowsArm64("windowsArm64") {
        binaries.executable {
            entryPoint = "io.chornge.kmpli.main"
            baseName = "kmpli"
        }
    }*/

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.content.negotiation)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(libs.junit)
                implementation(libs.kotlin.test)
            }
        }

        val nativeMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.squareup.okio)
            }
        }

        // Connect builds to nativeMain
        val macosX64Main by getting {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val linuxX64Main by getting {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
        val mingwX64Main by getting {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.client.curl)
            }
        }
    }
}

val nativeTargets = listOf("MacosX64", "LinuxX64", "MingwX64")

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

    nativeTargets.forEach { target ->
        val binaryDir = layout.buildDirectory.dir("bin/$target/releaseExecutable")
        from(binaryDir) {
            include("*")
            rename { fileName ->
                if (target.lowercase().startsWith("mingw")) fileName
                else fileName.removeSuffix(".kexe")
            }
        }
        into(layout.buildDirectory.dir("dist/$target"))
    }
}
