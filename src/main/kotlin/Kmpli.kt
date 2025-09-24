import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import kotlinx.coroutines.runBlocking
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
    private val name: String? by option("--name", help = "Project name").default("KotlinProject")
    private val pid: String? by option("--pid", help = "Project ID").default("org.example.project")

    private val platforms: String? by option(
        "--platforms", help = "Comma-separated platforms (android,ios(swiftui),web(react),desktop,server)"
    )
    private val includeTests: Boolean by option(
        "--include-tests",
        help = "Include Tests (false if a platform is specified)"
    ).flag(default = false)

    override fun run() = runBlocking {
        val parsedPlatforms = parsePlatforms(platforms)

        val url = buildUrl(
            name = name ?: "KotlinProject",
            id = pid ?: "org.example.project",
            platforms = parsedPlatforms,
            tests = includeTests
        )

        val zipFile = downloadZip(url)
        val extractedDir = extractZip(zipFile, name ?: "KotlinProject")
        replacePlaceholders(extractedDir, name ?: "KotlinProject", pid ?: "org.example.project")

        if (zipFile.exists()) {
            zipFile.delete()
        }
    }

    data class PlatformConfig(val name: String, val ui: String? = null)

    fun parsePlatforms(input: String?): List<PlatformConfig> {
        val raw = input?.takeIf { it.isNotBlank() } ?: "android,ios(compose)" // Default if not provided

        return raw.split(",").map { part ->
            val trimmed = part.trim()
            val match = Regex("""(\w+)(\(([^)]+)\))?""").find(trimmed)
            val name = match?.groups?.get(1)?.value ?: error("Invalid platform syntax: $part")
            val ui = match.groups[3]?.value // Optional UI inside parentheses
            PlatformConfig(name.lowercase(), ui?.lowercase())
        }
    }

    fun buildUrl(
        name: String, id: String, platforms: List<PlatformConfig>, tests: Boolean
    ): String {
        val targets = buildJsonObject {
            platforms.forEach {
                when (it.name) {
                    "android" -> put("android", buildJsonObject {
                        put("ui", JsonArray(listOf(JsonPrimitive(it.ui ?: "compose"))))
                    })

                    "ios" -> put("ios", buildJsonObject {
                        put("ui", JsonArray(listOf(JsonPrimitive(it.ui ?: "compose"))))
                    })

                    "desktop" -> put("desktop", buildJsonObject {
                        put("ui", JsonArray(listOf(JsonPrimitive(it.ui ?: "compose"))))
                    })

                    "web" -> put("web", buildJsonObject {
                        put("ui", JsonArray(listOf(JsonPrimitive(it.ui ?: "compose"))))
                    })

                    "server" -> put("server", buildJsonObject {
                        put("engine", JsonArray(listOf(JsonPrimitive("ktor"))))
                    })

                    else -> error("Unsupported platform: ${it.name}")
                }
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
        val response: ByteArray = client.get(url).readRawBytes()
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

        val oldDir = File(dir, oldDirPath)
        if (oldDir.exists()) {
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