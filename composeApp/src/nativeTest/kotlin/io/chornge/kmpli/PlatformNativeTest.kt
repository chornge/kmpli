package io.chornge.kmpli

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.posix.getenv
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the filesystem half of the native platform directly, against a real temporary
 * directory rather than a mock.
 *
 * A fake filesystem would not work here even if one were wired in: the directory walk uses
 * posix opendir/readdir/stat rather than okio, so only a real tree exercises it. These run on
 * whichever host builds, which is why the Linux-only CA probe takes its gate as a parameter.
 */
@OptIn(ExperimentalForeignApi::class)
class PlatformNativeTest {

    private fun tempDir(name: String): String {
        val root = getenv("TMPDIR")?.toKString()?.trimEnd('/') ?: "/tmp"
        val dir = "$root/kmpli-native-test/$name"
        FileSystem.SYSTEM.deleteRecursively(dir.toPath(), mustExist = false)
        FileSystem.SYSTEM.createDirectories(dir.toPath())
        return dir
    }

    private fun write(path: String, bytes: ByteArray) {
        FileSystem.SYSTEM.createDirectories(path.substringBeforeLast('/').toPath())
        FileSystem.SYSTEM.write(path.toPath()) { write(bytes) }
    }

    private fun read(path: String): ByteArray =
        FileSystem.SYSTEM.read(path.toPath()) { readByteArray() }

    // The regression this file exists for. Reading a jar or a PNG as UTF-8 decoded lossily and
    // wrote the damage back, so every generated project shipped a corrupt gradle-wrapper.jar.
    @Test
    fun binaryFilesAreLeftByteIdentical() {
        val dir = tempDir("binary")
        // A PK zip header, a placeholder string, then bytes that are not valid UTF-8. The
        // placeholder matters: without one, a file is spared by the write-only-if-changed
        // guard rather than by the strict decoding, and the test would pass against the very
        // bug it exists to catch.
        val jar = byteArrayOf(0x50, 0x4B, 0x03, 0x04) +
            "KotlinProject".encodeToByteArray() +
            byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0xC3.toByte())
        write("$dir/gradle/wrapper/gradle-wrapper.jar", jar)

        replaceFileContents(dir, "MyApp", "com.acme.myapp", "com.jetbrains.kmpapp")

        assertContentEquals(jar, read("$dir/gradle/wrapper/gradle-wrapper.jar"))
    }

    @Test
    fun filesContainingNulBytesAreLeftAlone() {
        val dir = tempDir("nul")
        // Valid UTF-8, but a NUL means it is not text.
        val blob = "KotlinProject".encodeToByteArray() + byteArrayOf(0) + "tail".encodeToByteArray()
        write("$dir/icon.bin", blob)

        replaceFileContents(dir, "MyApp", "com.acme.myapp", "com.jetbrains.kmpapp")

        assertContentEquals(blob, read("$dir/icon.bin"))
    }

    @Test
    fun textFilesHavePlaceholdersReplaced() {
        val dir = tempDir("text")
        write("$dir/src/App.kt", "package com.jetbrains.kmpapp\n// KotlinProject\n".encodeToByteArray())

        replaceFileContents(dir, "MyApp", "com.acme.myapp", "com.jetbrains.kmpapp")

        val out = read("$dir/src/App.kt").decodeToString()
        assertTrue(out.contains("package com.acme.myapp"), "package id should be rewritten")
        assertTrue(out.contains("// MyApp"), "project name should be rewritten")
        assertFalse(out.contains("kmpapp"), "no old package id should survive")
    }

    @Test
    fun filesWithoutPlaceholdersAreNotRewritten() {
        val dir = tempDir("untouched")
        val original = "nothing to replace here\n".encodeToByteArray()
        write("$dir/README.md", original)

        replaceFileContents(dir, "MyApp", "com.acme.myapp", "com.jetbrains.kmpapp")

        assertContentEquals(original, read("$dir/README.md"))
    }

    @Test
    fun nestedDirectoriesAreWalked() {
        val dir = tempDir("nested")
        write("$dir/a/b/c/Deep.kt", "com.jetbrains.kmpapp".encodeToByteArray())

        replaceFileContents(dir, "MyApp", "com.acme.myapp", "com.jetbrains.kmpapp")

        assertEquals("com.acme.myapp", read("$dir/a/b/c/Deep.kt").decodeToString())
    }

    @Test
    fun packageDirectoriesAreRenamed() {
        val dir = tempDir("pkgdirs")
        write("$dir/src/commonMain/kotlin/com/jetbrains/kmpapp/App.kt", "x".encodeToByteArray())

        renamePackageDirectories(dir, "com.acme.myapp", "com.jetbrains.kmpapp")

        assertTrue(
            FileSystem.SYSTEM.exists("$dir/src/commonMain/kotlin/com/acme/myapp/App.kt".toPath()),
            "the file should have moved to the new package path"
        )
        assertFalse(
            FileSystem.SYSTEM.exists("$dir/src/commonMain/kotlin/com/jetbrains/kmpapp".toPath()),
            "the old package directory should be gone"
        )
    }

    @Test
    fun identicalPackageIdsAreANoOp() {
        val dir = tempDir("samepkg")
        write("$dir/src/com/acme/myapp/App.kt", "x".encodeToByteArray())

        renamePackageDirectories(dir, "com.acme.myapp", "com.acme.myapp")

        assertTrue(FileSystem.SYSTEM.exists("$dir/src/com/acme/myapp/App.kt".toPath()))
    }

    @Test
    fun caProbeTakesTheFirstCandidateThatExists() {
        val dir = tempDir("ca-order")
        val missing = "$dir/absent.pem"
        val present = "$dir/present.pem"
        val alsoPresent = "$dir/also.pem"
        write(present, "cert".encodeToByteArray())
        write(alsoPresent, "cert".encodeToByteArray())

        val found = linuxCaFile(
            candidates = listOf(missing, present, alsoPresent),
            env = { null },
            onLinux = true
        )

        assertEquals(present, found, "the earliest existing candidate wins")
    }

    @Test
    fun caProbePrefersTheEnvironmentOverAnyCandidate() {
        val dir = tempDir("ca-env")
        val onDisk = "$dir/ondisk.pem"
        write(onDisk, "cert".encodeToByteArray())

        val found = linuxCaFile(
            candidates = listOf(onDisk),
            env = { if (it == "CURL_CA_BUNDLE") "/custom/bundle.pem" else null },
            onLinux = true
        )

        assertEquals("/custom/bundle.pem", found, "CURL_CA_BUNDLE should win")
    }

    @Test
    fun caProbeFallsBackToSslCertFile() {
        val found = linuxCaFile(
            candidates = emptyList(),
            env = { if (it == "SSL_CERT_FILE") "/ssl/cert.pem" else null },
            onLinux = true
        )

        assertEquals("/ssl/cert.pem", found)
    }

    @Test
    fun caProbeIgnoresBlankEnvironmentValues() {
        val dir = tempDir("ca-blank")
        val onDisk = "$dir/ondisk.pem"
        write(onDisk, "cert".encodeToByteArray())

        val found = linuxCaFile(candidates = listOf(onDisk), env = { "" }, onLinux = true)

        assertEquals(onDisk, found, "a blank variable should not shadow a real bundle")
    }

    @Test
    fun caProbeReturnsNothingWhenNoCandidateExists() {
        assertNull(linuxCaFile(candidates = listOf("/nope/absent.pem"), env = { null }, onLinux = true))
    }

    @Test
    fun caProbeIsSkippedOffLinux() {
        val dir = tempDir("ca-nonlinux")
        val onDisk = "$dir/ondisk.pem"
        write(onDisk, "cert".encodeToByteArray())

        assertNull(
            linuxCaFile(candidates = listOf(onDisk), env = { null }, onLinux = false),
            "macOS and Windows resolve trust through the OS, so nothing should be returned"
        )
    }
}
