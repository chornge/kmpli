import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.io.File
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipInputStream

class Kmpli : CliktCommand() {
    private val name: String? by option("-n", "--name", help = "Project name").default("KotlinProject")
    private val pid: String? by option("-p", "--pid", help = "Project ID").default("org.example.project")
    private val android: Boolean? by option("-a", "--android", help = "Include Android").flag()
    private val ios: Boolean? by option("-i", "--ios", help = "Include iOS").flag()
    private val iosui: String? by option("-iu", "--ios-ui", help = "iOS UI framework").default("compose")
    private val desktop: Boolean? by option("-d", "--desktop", help = "Include Desktop").flag()
    private val web: Boolean? by option("-w", "--web", help = "Include Web").flag()
    private val webui: String? by option("-wu", "--web-ui", help = "Web UI framework").default("compose")
    private val server: Boolean? by option("-s", "--server", help = "Include Server").flag()
    private val tests: Boolean by option("-t", "--tests", help = "Include Tests").flag(default = false)

    override fun run() = runBlocking {
        val platformFlags = listOf(android, ios, desktop, web, server)
        val anyPlatformSpecified = platformFlags.any { it != null }

        // Apply default if none were specified
        val useAndroid = android ?: !anyPlatformSpecified
        val useIos = ios ?: !anyPlatformSpecified
        val useDesktop = desktop ?: false
        val useWeb = web ?: false
        val useServer = server ?: false

        val url = buildUrl(
            name = name ?: "KotlinProject",
            id = pid ?: "org.example.project",
            android = useAndroid,
            ios = useIos,
            iosui = iosui,
            desktop = useDesktop,
            web = useWeb,
            webui = webui,
            server = useServer,
            tests = tests
        )
        val zipFile = downloadZip(url)
        val extractedDir = extractZip(zipFile, name ?: "KotlinProject")
        replacePlaceholders(extractedDir, name ?: "KotlinProject", pid ?: "org.example.project")

        if (zipFile.exists()) {
            zipFile.delete()
        }
    }

    fun buildUrl(
        name: String,
        id: String,
        android: Boolean,
        ios: Boolean,
        iosui: String?,
        desktop: Boolean,
        web: Boolean,
        webui: String?,
        server: Boolean,
        tests: Boolean
    ): String {
        val targets = buildJsonObject {
            if (android) {
                put("android", buildJsonObject {
                    put("ui", JsonArray(listOf(JsonPrimitive("compose"))))
                })
            }
            if (ios) {
                put("ios", buildJsonObject {
                    put("ui", JsonArray(listOf(JsonPrimitive(iosui))))
                })
            }
            if (desktop) {
                put("desktop", buildJsonObject {
                    put("ui", JsonArray(listOf(JsonPrimitive("compose"))))
                })
            }
            if (web) {
                put("web", buildJsonObject {
                    put("ui", JsonArray(listOf(JsonPrimitive(webui))))
                })
            }
            if (server) {
                put("server", buildJsonObject {
                    put("engine", JsonArray(listOf(JsonPrimitive("ktor"))))
                })
            }
        }

        val spec = buildJsonObject {
            put("template_id", JsonPrimitive("kmt"))
            put("targets", targets)
            if (tests) {
                put("include_tests", JsonPrimitive(true))
            }
        }

        val encodedId = URLEncoder.encode(id, "UTF-8")
        val encodedName = URLEncoder.encode(name, "UTF-8")
        val encodedSpec = URLEncoder.encode(Json.encodeToString(spec), "UTF-8")

        return "https://kmp.jetbrains.com/generateKmtProject?name=$encodedName&id=$encodedId&spec=$encodedSpec"
    }

    private suspend fun downloadZip(url: String): File {
        val client = HttpClient()
        val response: ByteArray = client.get(url).readBytes()
        val zipFile = File("cmpproject.zip")
        zipFile.writeBytes(response)
        return zipFile
    }

    fun extractZip(zipFile: File, projectName: String): File {
        val sanitizedProjectName = projectName.replace(" ", "_")
        val targetDir = File(sanitizedProjectName)
        targetDir.mkdirs()

        ZipInputStream(zipFile.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                // Strip top-level folder (e.g., "KotlinProject/...")
                val nameParts = entry.name.split("/", limit = 2)
                val relativePath = if (nameParts.size > 1) nameParts[1] else nameParts[0]

                if (relativePath.isNotBlank()) {
                    val newFile = File(targetDir, relativePath)
                    if (entry.isDirectory) {
                        newFile.mkdirs()
                    } else {
                        newFile.parentFile.mkdirs()
                        Files.copy(zip, newFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                    }
                }

                entry = zip.nextEntry
            }
        }

        return targetDir
    }

    fun replacePlaceholders(dir: File, name: String, pid: String) {
        val oldPid = "org.example.project"
        val oldDirPath = "org/example/project"

        // Rename the directory if it exists
        val oldDir = File(dir, oldDirPath)
        if (oldDir.exists()) {
            // Convert new PID to a directory path
            val newDir = File(dir, pid.replace(".", "/"))
            oldDir.renameTo(newDir)
        }

        dir.walk().forEach { file ->
            if (file.isFile) {
                var content = file.readText()
                content = content.replace("KotlinProject", name)
                content = content.replace(oldPid, pid)
                file.writeText(content)
            }
        }
    }
}

fun main(args: Array<String>) = Kmpli().main(args)