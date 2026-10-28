package io.chornge.kmpli

import io.ktor.client.HttpClient
import io.ktor.client.engine.curl.Curl
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.encodeURLPath
import io.ktor.utils.io.toByteArray
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.*
import okio.FileSystem
import okio.Path.Companion.toPath

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class, kotlin.experimental.ExperimentalNativeApi::class)
actual fun Platform(): Platform = object : Platform {
    private val client = NetClient()

    override suspend fun httpGetBytes(url: String): ByteArray {
        printLine("Downloading from: $url")
        try {
            val response = client.get(url)

            // Validate HTTP status code
            val statusCode = response.status.value
            if (statusCode !in 200..299) {
                throw IllegalStateException("HTTP $statusCode: ${response.status.description}")
            }

            val contentType = response.headers["Content-Type"]
            printLine("Content-Type: $contentType")

            // Warn if we got HTML instead of a ZIP (likely TLS/redirect issue)
            if (contentType?.contains("html", ignoreCase = true) == true) {
                printLine("⚠ Warning: received HTML instead of ZIP (likely a TLS or redirect issue).")
            }

            val bytes = response.bodyAsChannel().toByteArray()
            printLine("Downloaded ${bytes.size} bytes")

            return bytes
        } catch (e: Exception) {
            val errorMsg = e.message ?: "Unknown error"
            printLine("❌ HTTP request failed: $errorMsg")

            // Provide helpful troubleshooting for SSL/TLS errors
            if (errorMsg.contains("TLS", ignoreCase = true) ||
                errorMsg.contains("SSL", ignoreCase = true) ||
                errorMsg.contains("certificate", ignoreCase = true)
            ) {

                printLine("")
                printLine("🔧 SSL/TLS Certificate Error Detected!")
                printLine("")
                printLine("The server's certificate could not be verified.")
                printLine("")
                printLine("On Linux this usually means the system trust store is missing:")
                printLine("  sudo apt-get install -y ca-certificates   # Debian, Ubuntu")
                printLine("  sudo dnf install -y ca-certificates       # Fedora, RHEL")
                printLine("")
                printLine("macOS and Windows read the system trust store directly, so a failure")
                printLine("there is more likely a proxy or antivirus intercepting TLS.")
                printLine("")
                printLine("To point kmpli at a specific CA bundle instead:")
                printLine("  export CURL_CA_BUNDLE=/path/to/cert.pem")
                printLine("")
            }

            throw e
        }
    }

    override fun extractZip(zipBytes: ByteArray, projectName: String): String {
        val tmpZip = "$projectName.zip"
        val targetPath = "./$projectName".toPath()

        FileSystem.SYSTEM.write(tmpZip.toPath()) { write(zipBytes) }
        printLine("Saved ZIP to $tmpZip")

        // Use platform-appropriate extraction command.
        // On Windows, "tar" on PATH is usually Git for Windows' GNU tar, which cannot read
        // zip archives. The Windows-bundled bsdtar can, so invoke it by absolute path rather
        // than letting PATH decide, and fall back to PowerShell if it is not present.
        // macOS/Linux use unzip.
        val isWindows = kotlin.native.Platform.osFamily == kotlin.native.OsFamily.WINDOWS
        val exitCode = if (isWindows) {
            // Unquoted on purpose: cmd.exe mangles a command line that begins with a
            // quote, and %SystemRoot%\System32 never contains spaces.
            val bsdTar = system("%SystemRoot%\\System32\\tar.exe -xf \"$tmpZip\"")
            if (bsdTar == 0) {
                bsdTar
            } else {
                system(
                    "powershell -NoProfile -Command " +
                        "\"Expand-Archive -LiteralPath '$tmpZip' -DestinationPath '.' -Force\""
                )
            }
        } else {
            // -q to match the Windows branch: bsdtar and Expand-Archive are both silent,
            // so without it Unix users get a 50-line file dump that Windows users do not.
            system("unzip -q -o \"$tmpZip\" -d .")
        }
        if (exitCode != 0) {
            FileSystem.SYSTEM.delete(tmpZip.toPath())
            error("Failed to extract $tmpZip (extractor exited with code $exitCode)")
        }

        FileSystem.SYSTEM.delete(tmpZip.toPath())

        // Two archive shapes arrive here. kmp.jetbrains.com returns a zip already rooted at the
        // project name, so there is nothing to rename and targetPath already exists. The template
        // repositories return "<repo>-<branch>", which has to be moved into place. Only the second
        // case needs a folder to locate, so look for one only when the target is absent. Treating
        // a missing folder as an error unconditionally breaks platform generation.
        if (!FileSystem.SYSTEM.exists(targetPath)) {
            val extractedFolder = FileSystem.SYSTEM.list("./".toPath()).firstOrNull {
                it.name != projectName && (
                    it.name.contains("KMP-App-Template") ||
                    it.name.contains("multiplatform-library-template")
                )
            } ?: error("Extraction produced no project folder for $projectName")

            FileSystem.SYSTEM.atomicMove(extractedFolder, targetPath)
            printLine("Renamed ${extractedFolder.name} -> ${targetPath.name}")
        }

        // Sanity check
        val entries = FileSystem.SYSTEM.list(targetPath)
        if (entries.isEmpty()) {
            error("Extraction produced an empty folder: $targetPath")
        }
        printLine("Extraction complete: $targetPath (${entries.size} items)")

        return targetPath.toString()
    }

    override fun replacePlaceholders(dirPath: String, name: String, pid: String, oldPid: String) {
        // First, replace file contents
        replaceFileContents(dirPath, name, pid, oldPid)
        // Then, rename package directories
        renamePackageDirectories(dirPath, pid, oldPid)
    }

    override fun printLine(message: String) {
        println(message)
    }

    override fun urlEncode(value: String): String {
        return value.encodeURLPath()
    }
}

// ktor's Linux curl is linked against a static OpenSSL whose OPENSSLDIR is /etc/ssl, so the CA
// file it looks for by default is /etc/ssl/cert.pem. Debian and Ubuntu never create that file:
// ca-certificates installs /etc/ssl/certs/ca-certificates.crt and a hashed /etc/ssl/certs
// directory instead. That curl is also built without a CA fallback, so it does not consult
// OpenSSL's default verify paths either, and on a stock distribution every HTTPS request fails
// verification even with ca-certificates installed. So tell it where the trust store actually is.
//
// Ordered by how common they are. The last entry is OpenSSL's own default, which Alpine provides.

// Hoisted out of the Platform object so nativeTest can exercise them directly. They never
// referenced object state, so this is a move rather than a change: each already takes every
// value it needs as a parameter. Keeping them private inside an anonymous object meant the
// only way in was through the interface, which does network and process work, and that is why
// the file that corrupted every generated binary had no direct coverage.

/**
 * Rewrite placeholders in one file, leaving binary files alone.
 *
 * okio's readUtf8() is lossy: invalid byte sequences decode to U+FFFD rather than raising,
 * so reading a jar or a PNG through it and writing the result back rewrites that file as
 * mangled text. Nothing threw, so the previous catch never fired and every binary in every
 * generated project was corrupted, gradle-wrapper.jar included, which left `./gradlew`
 * failing with "Invalid or corrupt jarfile" in a freshly generated project.
 *
 * Decoding strictly is what that catch was always meant to do. The NUL check covers the
 * remainder, because a file can be valid UTF-8 and still not be text.
 */
internal fun rewritePlaceholders(path: String, name: String, pid: String, oldPid: String) {
    try {
        val bytes = FileSystem.SYSTEM.read(path.toPath()) { readByteArray() }
        if (bytes.any { it == 0.toByte() }) return

        val text = try {
            bytes.decodeToString(throwOnInvalidSequence = true)
        } catch (_: CharacterCodingException) {
            return
        }

        val updated = text
            .replace(oldPid, pid)
            .replace("KotlinProject", name)
            .replace("KMP-App-Template", name)

        // Only touch files that actually changed.
        if (updated != text) {
            FileSystem.SYSTEM.write(path.toPath()) { writeUtf8(updated) }
        }
    } catch (_: okio.IOException) {
        // Unreadable or unwritable: leave the file as found.
    }
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal fun replaceFileContents(dirPath: String, name: String, pid: String, oldPid: String) {
    val dir = opendir(dirPath)
    if (dir != null) {
        var entry = readdir(dir)
        while (entry != null) {
            val fname = entry.pointed.d_name.toKString()
            if (fname != "." && fname != "..") {
                val fullPath = "$dirPath/$fname"
                val statBuf = nativeHeap.alloc<stat>()
                try {
                    stat(fullPath, statBuf.ptr)
                    if ((statBuf.st_mode.toUInt() and S_IFDIR.toUInt()) != 0u) {
                        replaceFileContents(fullPath, name, pid, oldPid)
                    } else {
                        rewritePlaceholders(fullPath, name, pid, oldPid)
                    }
                } finally {
                    nativeHeap.free(statBuf.ptr)
                }
            }
            entry = readdir(dir)
        }
        closedir(dir)
    }
}

internal fun renamePackageDirectories(dirPath: String, pid: String, oldPid: String) {
    // Convert package IDs to path format (e.g., "com.jetbrains.kmpapp" -> "com/jetbrains/kmpapp")
    val oldPath = oldPid.replace('.', '/')
    val newPath = pid.replace('.', '/')

    if (oldPath == newPath) return

    // Find and rename all occurrences of the old package path
    findAndRenamePackageDirs(dirPath, oldPath, newPath)
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal fun findAndRenamePackageDirs(basePath: String, oldPkgPath: String, newPkgPath: String) {
    val dir = opendir(basePath)
    if (dir != null) {
        val entries = mutableListOf<String>()
        var entry = readdir(dir)
        while (entry != null) {
            val fname = entry.pointed.d_name.toKString()
            if (fname != "." && fname != "..") {
                entries.add(fname)
            }
            entry = readdir(dir)
        }
        closedir(dir)

        for (fname in entries) {
            val fullPath = "$basePath/$fname"
            val statBuf = nativeHeap.alloc<stat>()
            val isDir: Boolean
            try {
                stat(fullPath, statBuf.ptr)
                isDir = (statBuf.st_mode.toUInt() and S_IFDIR.toUInt()) != 0u
            } finally {
                nativeHeap.free(statBuf.ptr)
            }

            if (isDir) {
                // Check if this directory path ends with the old package path
                val relativePath = fullPath.removePrefix("./")
                if (relativePath.endsWith(oldPkgPath)) {
                    // Found the old package directory, rename it
                    val newFullPath = fullPath.replace(oldPkgPath, newPkgPath)
                    createParentDirs(newFullPath)
                    moveDirectoryContents(fullPath, newFullPath)
                    removeEmptyParentDirs(fullPath, oldPkgPath)
                } else {
                    // Continue searching recursively
                    findAndRenamePackageDirs(fullPath, oldPkgPath, newPkgPath)
                }
            }
        }
    }
}

internal fun createParentDirs(path: String) {
    val parentPath = path.substringBeforeLast('/')
    if (parentPath.isNotEmpty() && parentPath != path) {
        FileSystem.SYSTEM.createDirectories(parentPath.toPath())
    }
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal fun moveDirectoryContents(srcDir: String, destDir: String) {
    FileSystem.SYSTEM.createDirectories(destDir.toPath())

    val dir = opendir(srcDir)
    if (dir != null) {
        val entries = mutableListOf<String>()
        var entry = readdir(dir)
        while (entry != null) {
            val fname = entry.pointed.d_name.toKString()
            if (fname != "." && fname != "..") {
                entries.add(fname)
            }
            entry = readdir(dir)
        }
        closedir(dir)

        for (fname in entries) {
            val srcPath = "$srcDir/$fname".toPath()
            val destPath = "$destDir/$fname".toPath()
            FileSystem.SYSTEM.atomicMove(srcPath, destPath)
        }
    }
}

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
internal fun removeEmptyParentDirs(path: String, oldPkgPath: String) {
    // Remove empty directories up to the first segment of the old package path
    val segments = oldPkgPath.split('/')
    var currentPath = path

    for (i in segments.indices.reversed()) {
        val statBuf = nativeHeap.alloc<stat>()
        val exists: Boolean
        try {
            exists = stat(currentPath, statBuf.ptr) == 0
        } finally {
            nativeHeap.free(statBuf.ptr)
        }

        if (exists) {
            val dir = opendir(currentPath)
            if (dir != null) {
                var isEmpty = true
                var entry = readdir(dir)
                while (entry != null) {
                    val fname = entry.pointed.d_name.toKString()
                    if (fname != "." && fname != "..") {
                        isEmpty = false
                        break
                    }
                    entry = readdir(dir)
                }
                closedir(dir)

                if (isEmpty) {
                    rmdir(currentPath)
                    currentPath = currentPath.substringBeforeLast('/')
                } else {
                    break
                }
            }
        } else {
            break
        }
    }
}

internal val LINUX_CA_FILES = listOf(
    "/etc/ssl/certs/ca-certificates.crt", // Debian, Ubuntu, Arch
    "/etc/pki/tls/certs/ca-bundle.crt",   // Fedora, RHEL, CentOS
    "/etc/ssl/ca-bundle.pem",             // openSUSE
    "/etc/pki/tls/cacert.pem",            // older RHEL
    "/etc/ssl/cert.pem"                   // OpenSSL's compiled-in default
)

// A hashed certificate directory is enough on its own, and is the fallback when no single
// bundle file exists.
private const val LINUX_CA_DIR = "/etc/ssl/certs"

// Linux only. macOS verifies through Security.framework and the Windows build uses Schannel;
// both read the OS trust store without being told where it lives, and passing a path there
// would override a working default with a guess.
@OptIn(ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
internal fun isLinux() = kotlin.native.Platform.osFamily == kotlin.native.OsFamily.LINUX

/**
 * Resolve the CA bundle to hand to curl.
 *
 * Everything the logic depends on is a parameter so a test can drive it against temp files on
 * any host. `onLinux` in particular: the probe is Linux-only behaviour, but the tests run on
 * whichever machine builds, so the gate has to be injectable or the whole function is dead code
 * off Linux and can never be exercised.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun linuxCaFile(
    candidates: List<String> = LINUX_CA_FILES,
    env: (String) -> String? = { getenv(it)?.toKString() },
    onLinux: Boolean = isLinux()
): String? {
    if (!onLinux) return null

    // An explicit setting wins, so the CURL_CA_BUNDLE advice under Troubleshooting still works.
    val fromEnv = listOf("CURL_CA_BUNDLE", "SSL_CERT_FILE")
        .mapNotNull { env(it) }
        .firstOrNull { it.isNotBlank() }

    return fromEnv ?: candidates.firstOrNull { FileSystem.SYSTEM.exists(it.toPath()) }
}

private fun linuxCaDir(): String? =
    if (isLinux() && FileSystem.SYSTEM.exists(LINUX_CA_DIR.toPath())) LINUX_CA_DIR else null

actual fun NetClient(): HttpClient {
    val caFile = linuxCaFile()
    val caDir = if (caFile == null) linuxCaDir() else null

    return HttpClient(Curl) {
        engine {
            sslVerify = true

            // Left unset on platforms that already resolve trust themselves, and when nothing
            // was found: curl's own "peer certificate was not OK" is the right error then, and
            // the handler for it already tells the user to install ca-certificates.
            caFile?.let { caInfo = it }
            caDir?.let { caPath = it }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
        expectSuccess = false
    }
}
