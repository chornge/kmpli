package io.chornge.kmpli

//import io.ktor.client.engine.darwin.*
import io.ktor.client.HttpClient
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

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
actual fun Platform(): Platform = object : Platform {
    private val client = HttpClient()

    override suspend fun httpGetBytes(url: String): ByteArray {
        printLine("Downloading from: $url")
        val bytes = try {
            client.get(url).bodyAsChannel().toByteArray()
        } catch (e: Exception) {
            printLine("❌ HTTP request failed: ${e.message}")
            return ByteArray(0)
        }

        printLine("Downloaded ${bytes.size} bytes")
        if (bytes.isEmpty()) {
            printLine("❌ Download returned empty content")
        }

        return bytes
    }

    override fun extractZip(zipBytes: ByteArray, projectName: String): String {
        val targetPath = "./$projectName".toPath()

        // Make sure target folder exists
        FileSystem.SYSTEM.createDirectories(targetPath)

        // Temporary zip file
        val tmpZip = "$projectName.zip"
        FileSystem.SYSTEM.write(tmpZip.toPath()) { write(zipBytes) }

        val writtenSize = FileSystem.SYSTEM.metadata(tmpZip.toPath()).size
        printLine("Saved ZIP to $tmpZip, size=$writtenSize bytes")

        if (writtenSize != null) {
            if (writtenSize < 100) {
                printLine("❌ ZIP file too small to be valid. Aborting extraction.")
                FileSystem.SYSTEM.delete(tmpZip.toPath())
                return targetPath.toString()
            }
        }

        // Use unzip command safely
        val exitCode = system("unzip -o $tmpZip -d $projectName")
        if (exitCode != 0) {
            printLine("❌ unzip failed with code $exitCode. Aborting extraction.")
            FileSystem.SYSTEM.delete(tmpZip.toPath())
            return targetPath.toString()
        }

        // Remove temp zip after extraction
        FileSystem.SYSTEM.delete(tmpZip.toPath())

        // Check extracted folder contents
        val entries = FileSystem.SYSTEM.list(targetPath)
        if (entries.isEmpty()) {
            printLine("❌ Extraction produced empty folder: $targetPath")
        } else {
            printLine("✅ Extraction complete: $targetPath")
        }

        return targetPath.toString()
    }

    override fun replacePlaceholders(dirPath: String, name: String, pid: String, oldPid: String) {
        // Simple POSIX-based traversal (use okio)
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
                        replacePlaceholders(fullPath, name, pid, oldPid)
                    } else {
                        val content = FileSystem.SYSTEM.read(fullPath.toPath()) { readUtf8() }
                            .replace(oldPid, pid)
                            .replace("KotlinProject", name)
                            .replace("KMP-App-Template", name)
                        FileSystem.SYSTEM.write(fullPath.toPath()) { writeUtf8(content) }
                    }
                    nativeHeap.free(statBuf.ptr)
                }
                entry = readdir(dir)
            }
            closedir(dir)
        }
    }

    override fun printLine(message: String) {
        println(message)
    }

    override fun urlEncode(value: String): String {
        // Simple percent-encoding
        return value.encodeURLPath()
    }
}

actual fun HttpClient(): HttpClient =
    HttpClient(/*Darwin*/) {
        engine {
            // optional config, e.g., timeout
        }
    }
