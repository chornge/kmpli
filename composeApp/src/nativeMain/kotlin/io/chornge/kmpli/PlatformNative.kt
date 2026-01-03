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
                printLine("⚠ Warning: received HTML instead of ZIP — likely a TLS or redirect issue.")
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
                printLine("This usually means your system is missing SSL certificates.")
                printLine("Please install OpenSSL:")
                printLine("")
                printLine("  macOS:   brew install openssl curl")
                printLine("  Linux:   sudo apt-get install -y ca-certificates libcurl4-openssl-dev libssl-dev")
                printLine("  Windows: choco install -y curl openssl.light")
                printLine("")
                printLine("After installing, try running kmpli again.")
                printLine("")
                printLine("If the issue persists, you can manually set the CA bundle path:")
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

        // Use platform-appropriate extraction command
        // Windows 10+ has built-in tar that supports zip files
        // macOS/Linux use unzip
        val isWindows = kotlin.native.Platform.osFamily == kotlin.native.OsFamily.WINDOWS
        val exitCode = if (isWindows) {
            system("tar -xf $tmpZip")
        } else {
            system("unzip -o $tmpZip -d .")
        }
        if (exitCode != 0) {
            val cmd = if (isWindows) "tar" else "unzip"
            printLine("$cmd failed with code $exitCode")
            FileSystem.SYSTEM.delete(tmpZip.toPath())
            return targetPath.toString()
        }

        FileSystem.SYSTEM.delete(tmpZip.toPath())

        // Detect the actual extracted folder (e.g. "KMP-App-Template-main", "multiplatform-library-template-main")
        val extractedFolder = FileSystem.SYSTEM.list("./".toPath()).firstOrNull {
            it.name != projectName && (
                it.name.contains("KMP-App-Template") ||
                it.name.contains("multiplatform-library-template")
            )
        }

        if (extractedFolder == null) {
            return targetPath.toString()
        }

        // Rename/move to intended project name
        if (!FileSystem.SYSTEM.exists(targetPath)) {
            FileSystem.SYSTEM.atomicMove(extractedFolder, targetPath)
            printLine("Renamed ${extractedFolder.name} -> ${targetPath.name}")
        }

        // Sanity check
        val entries = FileSystem.SYSTEM.list(targetPath)
        if (entries.isEmpty()) {
            printLine("Extraction produced empty folder: $targetPath")
        } else {
            printLine("Extraction complete: $targetPath (${entries.size} items)")
        }

        return targetPath.toString()
    }

    override fun replacePlaceholders(dirPath: String, name: String, pid: String, oldPid: String) {
        // First, replace file contents
        replaceFileContents(dirPath, name, pid, oldPid)
        // Then, rename package directories
        renamePackageDirectories(dirPath, pid, oldPid)
    }

    private fun replaceFileContents(dirPath: String, name: String, pid: String, oldPid: String) {
        val dir = opendir(dirPath)
        if (dir != null) {
            var entry = readdir(dir)
            while (entry != null) {
                val fname = entry.pointed.d_name.toKString()
                if (fname != "." && fname != "..") {
                    val fullPath = "$dirPath/$fname"
                    val statBuf = nativeHeap.alloc<stat>()
                    stat(fullPath, statBuf.ptr)
                    if ((statBuf.st_mode.toUInt() and S_IFDIR.toUInt()) != 0u) {
                        replaceFileContents(fullPath, name, pid, oldPid)
                    } else {
                        try {
                            val content = FileSystem.SYSTEM.read(fullPath.toPath()) { readUtf8() }
                                .replace(oldPid, pid)
                                .replace("KotlinProject", name)
                                .replace("KMP-App-Template", name)
                            FileSystem.SYSTEM.write(fullPath.toPath()) { writeUtf8(content) }
                        } catch (_: Exception) {
                            // Skip binary files that can't be read as UTF-8
                        }
                    }
                    nativeHeap.free(statBuf.ptr)
                }
                entry = readdir(dir)
            }
            closedir(dir)
        }
    }

    private fun renamePackageDirectories(dirPath: String, pid: String, oldPid: String) {
        // Convert package IDs to path format (e.g., "com.jetbrains.kmpapp" -> "com/jetbrains/kmpapp")
        val oldPath = oldPid.replace('.', '/')
        val newPath = pid.replace('.', '/')

        if (oldPath == newPath) return

        // Find and rename all occurrences of the old package path
        findAndRenamePackageDirs(dirPath, oldPath, newPath)
    }

    private fun findAndRenamePackageDirs(basePath: String, oldPkgPath: String, newPkgPath: String) {
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
                stat(fullPath, statBuf.ptr)
                val isDir = (statBuf.st_mode.toUInt() and S_IFDIR.toUInt()) != 0u
                nativeHeap.free(statBuf.ptr)

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

    private fun createParentDirs(path: String) {
        val parentPath = path.substringBeforeLast('/')
        if (parentPath.isNotEmpty() && parentPath != path) {
            FileSystem.SYSTEM.createDirectories(parentPath.toPath())
        }
    }

    private fun moveDirectoryContents(srcDir: String, destDir: String) {
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

    private fun removeEmptyParentDirs(path: String, oldPkgPath: String) {
        // Remove empty directories up to the first segment of the old package path
        val segments = oldPkgPath.split('/')
        var currentPath = path

        for (i in segments.indices.reversed()) {
            val statBuf = nativeHeap.alloc<stat>()
            val exists = stat(currentPath, statBuf.ptr) == 0
            nativeHeap.free(statBuf.ptr)

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

    override fun printLine(message: String) {
        println(message)
    }

    override fun urlEncode(value: String): String {
        return value.encodeURLPath()
    }
}

actual fun NetClient(): HttpClient {
    return HttpClient(Curl) {
        engine {
            sslVerify = true
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
        expectSuccess = false
    }
}
