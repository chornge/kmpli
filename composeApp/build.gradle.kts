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
    macosArm64("macosArm64") {
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
    linuxArm64("linuxArm64") {
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
                implementation(libs.ktor.client.core)
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
                implementation(libs.ktor.client.cio)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.network.tls)
                implementation(libs.squareup.okio)
            }
        }

        val macosX64Main by getting {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }
        val macosArm64Main by getting {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.client.darwin)
            }
        }

        val linuxX64Main by getting {
            dependsOn(nativeMain)
            dependencies {
                //implementation(libs.ktor.client.curl)
            }
        }
        val linuxArm64Main by getting {
            dependsOn(nativeMain)
            dependencies {
                //implementation(libs.ktor.client.curl)
            }
        }

        val mingwX64Main by getting {
            dependsOn(nativeMain)
            dependencies {
                implementation(libs.ktor.client.winhttp)
            }
        }
    }
}

val nativeTargets = listOf("MacosX64", "MacosArm64", "LinuxX64", "LinuxArm64", "MingwX64")
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
