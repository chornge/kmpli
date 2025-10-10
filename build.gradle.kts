plugins {
    application
    kotlin("jvm") version libs.versions.kotlinJvm.get()
}

application {
    mainClass.set("KmpliKt")
}

group = "io.chornge.kmpli"
version = libs.versions.appVersion.get()

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation(libs.clikt)
    testImplementation(libs.junitJupiter)
    implementation(libs.kotlinxSerializationJson)
    implementation(libs.ktorClientCio)
    implementation(libs.ktorClientCore)
    implementation(libs.ktorClientSerialization)
    implementation(libs.slf4jApi)
    runtimeOnly(libs.logbackClassic)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
    sourceSets.all {
        languageSettings.optIn("kotlinx.serialization.ExperimentalSerializationApi")
    }
}