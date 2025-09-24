import com.github.ajalt.clikt.core.CliktCommand
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
    private val name: String? by option("--name", help = "Project name")
    private val pid: String? by option("--pid", help = "Project ID")

    private val template: String? by option(
        "--template", help = "Project template name:\n" + ProjectTemplate.helpText()
    )

    private val platforms: String? by option(
        "--platforms", help = "Comma-separated platforms (android,ios(swiftui),web(react),desktop,server)"
    )

    private val includeTests: Boolean by option(
        "--include-tests", help = "Include Tests (false if a platform is specified)"
    ).flag(default = false)

    override fun run(): Unit = runBlocking {
        // Template mode
        template?.let { templateName ->
            require(platforms == null) {
                "--template and --platforms cannot be used together."
            }

            val selectedTemplate = ProjectTemplate.fromId(templateName)
                ?: error("Invalid template name: $templateName\n\nAvailable templates:\n${ProjectTemplate.helpText()}")

            echo("Using template: ${selectedTemplate.id} → ${selectedTemplate.description}")
            val zipFile = downloadZip(selectedTemplate.url)
            val extractedDir = extractZip(zipFile, name ?: "KMP-App-Template")

            replacePlaceholders(
                dir = extractedDir,
                name = name ?: "KMP-App-Template",
                pid = pid ?: "com.jetbrains.kmpapp",
                oldPid = "com.jetbrains.kmpapp"
            )

            if (zipFile.exists()) zipFile.delete()
            return@runBlocking
        }

        // Platforms mode
        val parsedPlatforms = parsePlatforms(platforms)
        val url = buildUrl(
            name = name ?: "KotlinProject",
            id = pid ?: "org.example.project",
            platforms = parsedPlatforms,
            tests = includeTests
        )
        val zipFile = downloadZip(url)
        val extractedDir = extractZip(zipFile, name ?: "KotlinProject")

        replacePlaceholders(
            dir = extractedDir,
            name = name ?: "KotlinProject",
            pid = pid ?: "org.example.project",
            oldPid = "org.example.project"
        )

        if (zipFile.exists()) zipFile.delete()
    }

    data class PlatformConfig(val name: String, val ui: String? = null)

    fun parsePlatforms(input: String?): List<PlatformConfig> {
        val raw = input?.takeIf { it.isNotBlank() } ?: "android,ios(compose)" // Default platforms

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

    fun replacePlaceholders(dir: File, name: String, pid: String, oldPid: String) {
        val oldDirPath = oldPid.replace(".", "/")
        val newDirPath = pid.replace(".", "/")

        // Move all files from old package path to new package path
        dir.walkTopDown().forEach { file ->
            val relativePath = file.relativeToOrNull(dir)?.path ?: return@forEach

            if (relativePath.contains(oldDirPath) && file.isFile) {
                val newRelativePath = relativePath.replace(oldDirPath, newDirPath)
                val targetFile = File(dir, newRelativePath)

                targetFile.parentFile.mkdirs()

                file.renameTo(targetFile)
                // Fallback: renameTo() only works across the same filesystem
                /* if (!file.renameTo(targetFile)) {
                     file.copyTo(targetFile, overwrite = true)
                     file.delete()
                 }*/
            }
        }

        // Safely delete the old package directory
        val oldPackageDir = File(dir, oldDirPath)
        if (oldPackageDir.exists()) {
            oldPackageDir.deleteRecursively()
        }

        // Replace placeholders in all files
        dir.walk().forEach { file ->
            if (file.isFile) {
                var content = file.readText()
                content = content.replace("KotlinProject", name)
                content = content.replace("KMP-App-Template", name)
                content = content.replace(oldPid, pid)
                file.writeText(content)
            }
        }
    }
}

sealed class ProjectTemplate(
    val id: String, val description: String, val url: String
) {
    object SharedUi : ProjectTemplate(
        id = "shared-ui",
        description = "Shared UI Multiplatform App using Compose Multiplatform",
        url = "https://github.com/Kotlin/KMP-App-Template/archive/refs/heads/main.zip"
    )

    object NativeUi : ProjectTemplate(
        id = "native-ui",
        description = "Native UI Multiplatform App using Jetpack Compose + SwiftUI",
        url = "https://github.com/Kotlin/KMP-App-Template-Native/archive/refs/heads/main.zip"
    )

    object Library : ProjectTemplate(
        id = "library",
        description = "Bare-bones Multiplatform Library",
        url = "https://github.com/Kotlin/multiplatform-library-template/archive/refs/heads/main.zip"
    )

    object SharedUiAmper : ProjectTemplate(
        id = "shared-ui-amper",
        description = "Shared UI Multiplatform App configured with Amper",
        url = "https://github.com/Kotlin/KMP-App-Template/archive/refs/heads/amper.zip"
    )

    object NativeUiAmper : ProjectTemplate(
        id = "native-ui-amper",
        description = "Native UI Multiplatform App configured with Amper",
        url = "https://github.com/Kotlin/KMP-App-Template-Native/archive/refs/heads/amper.zip"
    )

    companion object {
        private val allTemplates: List<ProjectTemplate> = listOf(
            SharedUi, NativeUi, Library, SharedUiAmper, NativeUiAmper
        )

        fun fromId(id: String): ProjectTemplate? = allTemplates.find { it.id.equals(id, ignoreCase = true) }

        fun listTemplateIds(): List<String> = allTemplates.map { it.id }

        fun helpText(): String = allTemplates.joinToString("\n") { "  ${it.id.padEnd(20)} → ${it.description}" }
    }
}

fun main(args: Array<String>) = Kmpli().main(args)