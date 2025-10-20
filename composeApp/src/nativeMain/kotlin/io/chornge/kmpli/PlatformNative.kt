package io.chornge.kmpli

//import io.ktor.client.*
//import io.ktor.client.engine.curl.*
//import io.ktor.client.engine.darwin.*
import io.ktor.client.HttpClient
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

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
actual fun Platform(): Platform = object : Platform {
    private val client = HttpClient()

    override suspend fun httpGetBytes(url: String): ByteArray {
        printLine("Downloading from: $url")
        try {
            val response = client.get(url)
            val contentType = response.headers["Content-Type"]
            printLine("Content-Type: $contentType")

            val bytes = response.bodyAsChannel().toByteArray()
            printLine("Downloaded ${bytes.size} bytes")

            if (contentType?.contains("html", ignoreCase = true) == true) {
                printLine("⚠️ Warning: received HTML instead of ZIP — likely a TLS or redirect issue.")
            }

            return bytes
        } catch (e: Exception) {
            printLine("❌ HTTP request failed: ${e.message}")
            return ByteArray(0)
        }
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

        if (writtenSize != null && writtenSize < 100) {
            printLine("❌ ZIP file too small to be valid. Aborting extraction.")
            FileSystem.SYSTEM.delete(tmpZip.toPath())
            return targetPath.toString()
        }

        // Unzip to current directory instead of projectName
        val exitCode = system("unzip -o $tmpZip -d .")
        if (exitCode != 0) {
            printLine("❌ unzip failed with code $exitCode. Aborting extraction.")
            FileSystem.SYSTEM.delete(tmpZip.toPath())
            return targetPath.toString()
        }

        FileSystem.SYSTEM.delete(tmpZip.toPath())

        // If we end up with projectName/projectName, fix it
        val innerPath = "$projectName/$projectName".toPath()
        if (FileSystem.SYSTEM.exists(innerPath)) {
            printLine("🪄 Fixing nested directory structure...")
            val innerEntries = FileSystem.SYSTEM.list(innerPath)
            innerEntries.forEach { entry ->
                FileSystem.SYSTEM.atomicMove(entry, "$projectName/${entry.name}".toPath())
            }
            FileSystem.SYSTEM.deleteRecursively(innerPath)
        }

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

actual fun HttpClient(): HttpClient = when {
    osName().contains("Darwin", ignoreCase = true) -> {
        HttpClient(/*Darwin*/) {
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
            }
            expectSuccess = false
        }
    }

    else -> {
        HttpClient(/*Curl*/) {
            engine {
                /*configureCurl {
                    // disable SSL verify (via libcurl flag)
                    curl_easy_setopt(it, 64, 0) // peer verification
                    curl_easy_setopt(it, 81, 0) // host verification
                }*/
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 30_000
            }
            expectSuccess = false
        }
    }
}
