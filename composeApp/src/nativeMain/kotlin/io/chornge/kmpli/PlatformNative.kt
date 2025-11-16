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

@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
actual fun Platform(): Platform = object : Platform {
    private val client = NetClient()

    override suspend fun httpGetBytes(url: String): ByteArray {
        printLine("Downloading from: $url")
        try {
            val response = client.get(url)
            val contentType = response.headers["Content-Type"]
            printLine("Content-Type: $contentType")

            val bytes = response.bodyAsChannel().toByteArray()
            printLine("Downloaded ${bytes.size} bytes")

            if (contentType?.contains("html", ignoreCase = true) == true) {
                printLine("⚠ Warning: received HTML instead of ZIP — likely a TLS or redirect issue.")
            }

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

        // Unzip into current directory
        val exitCode = system("unzip -o $tmpZip -d .")
        if (exitCode != 0) {
            printLine("unzip failed with code $exitCode")
            FileSystem.SYSTEM.delete(tmpZip.toPath())
            return targetPath.toString()
        }

        FileSystem.SYSTEM.delete(tmpZip.toPath())

        // Detect the actual extracted folder (e.g. "KMP-App-Template-main")
        val extractedFolder = FileSystem.SYSTEM.list("./".toPath()).firstOrNull {
            it.name != projectName && it.name.contains("KMP-App-Template")
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
        return value.encodeURLPath()
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun NetClient(): HttpClient {
    return HttpClient(Curl) {
        engine {
            sslVerify = true

            // Set CA bundle path via environment variable if not already set
            // This helps curl find SSL certificates on macOS
            if (getenv("CURL_CA_BUNDLE") == null && getenv("SSL_CERT_FILE") == null) {
                val caBundlePaths = listOf(
                    "/etc/ssl/cert.pem",                        // macOS (from brew openssl)
                    "/opt/homebrew/etc/openssl/cert.pem",       // macOS Apple Silicon (brew)
                    "/usr/local/etc/openssl/cert.pem",          // macOS Intel (brew)
                    "/usr/local/etc/openssl@3/cert.pem",        // macOS (brew openssl@3)
                    "/etc/ssl/certs/ca-certificates.crt",       // Linux (Debian/Ubuntu)
                    "/etc/pki/tls/certs/ca-bundle.crt",         // Linux (CentOS/RHEL)
                    "/etc/ssl/ca-bundle.pem"                    // Linux (openSUSE)
                )

                // Find first existing CA bundle and set it
                val caBundle = caBundlePaths.firstOrNull { path ->
                    access(path, F_OK) == 0
                }

                if (caBundle != null) {
                    setenv("CURL_CA_BUNDLE", caBundle, 1)
                }
            }
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
        expectSuccess = false
    }
}
