package io.chornge.kmpli

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.toByteArray
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import java.io.File
import java.net.URLEncoder
import java.util.zip.ZipInputStream

class Kmpli {
    fun parse(args: Array<String>) = runBlocking {
        val options = parseArgs(args)

        options.help?.let {
            println("Usage: ./kmpli --name=\"CMPProject\" --pid=\"org.cmp.project\" [--platforms=\"TARGETS\"] [--include-tests]")
            println()
            println("Options:")
            println("  --name           Project name")
            println("  --pid            Package identifier")
            println("  --platforms      Comma-separated list of targets with UI engine. default engine is compose")
            println("                   Supported targets: android, ios, ios(swiftui), desktop, web, web(react), server")
            println("                   Example: --platforms=\"android,ios(swiftui),desktop,web(react),server\"")
            println("  --template       Predefined project template to use in place of platforms")
            println("                   Available templates: ${ProjectTemplate.availableTemplates()}")
            println("                   Example: --template=\"shared-ui\"")
            println("  --include-tests  Include sample tests in the generated project")
            println("  --help, -h       Show this help message")
            return@runBlocking
        }

        // Template mode
        options.template?.let { templateName ->
            if (options.name == null) options.name = "CMPProject"
            // Replace non-alphanumeric characters
            if (options.pid == null) options.pid =
                "org.cmp.${options.name!!.lowercase().replace(Regex("[^a-z0-9]+"), "")}"
            require(options.platforms == null) {
                "--template and --platforms cannot be used together."
            }

            val selectedTemplate = ProjectTemplate.fromId(templateName)
                ?: error("Invalid template name: $templateName\nAvailable templates:\n${ProjectTemplate.availableTemplates()}")

            println("Generating template: ${selectedTemplate.id} → ${selectedTemplate.description}")
            val zipFile = downloadZip(selectedTemplate.url)
            val extractedDir = extractZip(zipFile, options.name!!)
            replacePlaceholders(
                dir = extractedDir,
                name = options.name!!,
                pid = options.pid!!,
                oldPid = "com.jetbrains.kmpapp"
            )

            if (zipFile.exists()) zipFile.delete()
            println("✅ Project generation complete!")
            return@runBlocking
        }

        // Platforms mode
        val parsedPlatforms = parsePlatforms(options.platforms)
        if (options.name == null) options.name = "CMPProject"
        if (options.pid == null) options.pid = "org.cmp.${options.name!!.lowercase()}"
        val url = buildUrl(
            name = options.name!!,
            id = options.pid!!,
            platforms = parsedPlatforms,
            tests = options.includeTests
        )

        println("Generating project for platform(s):")
        parsedPlatforms.forEach { platform ->
            val uiInfo = platform.ui?.let { " ($it)" } ?: ""
            println("• ${platform.name}$uiInfo")
        }
        if (options.includeTests) println("• including tests")

        val zipFile = downloadZip(url)
        val extractedDir = extractZip(zipFile, options.name!!)
        replacePlaceholders(
            dir = extractedDir,
            name = options.name!!,
            pid = options.pid!!,
            //oldPid = "io.example.test"
            oldPid = "org.example.project"
        )

        if (zipFile.exists()) zipFile.delete()
        println("✅ Project generation complete!")
    }

    private fun parseArgs(args: Array<String>): CliOptions {
        val options = CliOptions()
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            when {
                arg.startsWith("--name") -> options.name =
                    arg.substringAfter("=", args.getOrNull(i + 1) ?: "")

                arg.startsWith("--pid") -> options.pid =
                    arg.substringAfter("=", args.getOrNull(i + 1) ?: "")

                arg.startsWith("--template") -> options.template =
                    arg.substringAfter("=", args.getOrNull(i + 1) ?: "")

                arg.startsWith("--platforms") -> options.platforms =
                    arg.substringAfter("=", args.getOrNull(i + 1) ?: "")

                arg == "--include-tests" -> options.includeTests = true

                arg == "--help" || arg == "-h" -> options.help = true

                else -> println("Unknown argument: $arg")
            }
            i += 1
        }
        return options
    }

    data class CliOptions(
        var name: String? = null,
        var pid: String? = null,
        var template: String? = null,
        var platforms: String? = null,
        var includeTests: Boolean = false,
        var help: Boolean? = null
    )

    data class PlatformConfig(
        val name: String,
        val ui: String? = null
    )

    fun parsePlatforms(input: String?): List<PlatformConfig> {
        val raw = input?.takeIf { it.isNotBlank() } ?: "android,ios(compose)"
        return raw.split(",").map { part ->
            val trimmed = part.trim()
            val match = Regex("""(\w+)(\(([^)]+)\))?""").find(trimmed)
            val name = match?.groups?.get(1)?.value ?: error("Invalid platform syntax: $part")
            val ui = match.groups[3]?.value
            PlatformConfig(name.lowercase(), ui?.lowercase())
        }
    }

    fun buildUrl(
        name: String,
        id: String,
        platforms: List<PlatformConfig>,
        tests: Boolean
    ): String {
        val targets = buildJsonObject {
            platforms.forEach {
                when (it.name) {
                    "android", "ios", "desktop", "web" -> put(it.name, buildJsonObject {
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
            if (tests) put("include_tests", JsonPrimitive(true))
        }

        val encodedId = URLEncoder.encode(id, "UTF-8")
        val encodedName = URLEncoder.encode(name, "UTF-8")
        val encodedSpec = URLEncoder.encode(Json.encodeToString(spec), "UTF-8")
        return "https://kmp.jetbrains.com/generateKmtProject?name=$encodedName&id=$encodedId&spec=$encodedSpec"
    }

    private suspend fun downloadZip(url: String): File {
        val client = HttpClient(CIO)
        val response: ByteArray = client.get(url).bodyAsChannel().toByteArray()
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
                val relativePath = entry.name.substringAfter("/", entry.name)
                if (relativePath.isNotBlank()) {
                    val newFile = File(targetDir, relativePath)
                    if (entry.isDirectory) newFile.mkdirs()
                    else {
                        newFile.parentFile.mkdirs()
                        newFile.outputStream().use { output -> zip.copyTo(output) }
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

        // Move files first
        dir.walkTopDown().forEach { file ->
            val relativePath = file.relativeToOrNull(dir)?.path ?: return@forEach
            if (relativePath.contains(oldDirPath) && file.isFile) {
                val newRelativePath = relativePath.replace(oldDirPath, newDirPath)
                val targetFile = File(dir, newRelativePath)
                targetFile.parentFile.mkdirs()
                if (!file.renameTo(targetFile)) {
                    file.copyTo(targetFile, overwrite = true)
                    file.delete()
                }
            }
        }

        // Delete all directories containing the old package path
        dir.walkBottomUp().filter { it.isDirectory && it.relativeTo(dir).path.contains(oldDirPath) }
            .forEach { it.deleteRecursively() }

        // Replace placeholders in text files
        dir.walkTopDown().forEach { file ->
            if (file.isFile && !isBinaryFile(file)) {
                var content = file.readText()
                content = content
                    .replace(oldPid, pid)
                    .replace("KotlinProject", name)
                    .replace("KMP-App-Template", name)
                file.writeText(content)
            }
        }
    }

    private fun isBinaryFile(file: File): Boolean {
        file.inputStream().use { input ->
            val buffer = ByteArray(8000)
            val read = input.read(buffer)
            for (i in 0 until read) {
                val b = buffer[i]
                if (b < 0x09 || (b in 0x0E..0x1F) || b == 0x7F.toByte()) return true
            }
        }
        return false
    }
}

sealed class ProjectTemplate(
    val id: String,
    val description: String,
    val url: String
) {
    object SharedUi : ProjectTemplate(
        "shared-ui", "Shared UI App (Compose)",
        "https://github.com/Kotlin/KMP-App-Template/archive/refs/heads/main.zip"
    )

    object NativeUi : ProjectTemplate(
        "native-ui", "Native UI App (Compose + SwiftUI)",
        "https://github.com/Kotlin/KMP-App-Template-Native/archive/refs/heads/main.zip"
    )

    object Library : ProjectTemplate(
        id = "library",
        description = "Bare-bones Multiplatform Library",
        url = "https://github.com/Kotlin/multiplatform-library-template/archive/refs/heads/main.zip"
    )

    object SharedUiAmper : ProjectTemplate(
        id = "shared-ui-amper",
        description = "Shared UI App (configured with Amper)",
        url = "https://github.com/Kotlin/KMP-App-Template/archive/refs/heads/amper.zip"
    )

    object NativeUiAmper : ProjectTemplate(
        id = "native-ui-amper",
        description = "Native UI App (configured with Amper)",
        url = "https://github.com/Kotlin/KMP-App-Template-Native/archive/refs/heads/amper.zip"
    )

    companion object {
        private val allTemplates = listOf(SharedUi, NativeUi, Library, SharedUiAmper, NativeUiAmper)
        fun fromId(id: String) = allTemplates.find { it.id.equals(id, ignoreCase = true) }
        fun availableTemplates() = allTemplates.joinToString(", ") { it.id }
    }
}

fun main(args: Array<String>) {
    Kmpli().parse(args)
}