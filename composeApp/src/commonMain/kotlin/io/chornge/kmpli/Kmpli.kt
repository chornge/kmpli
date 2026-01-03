package io.chornge.kmpli

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

class Kmpli(private val io: Platform = Platform()) {

    companion object {
        // Valid project name: Latin letters, digits, spaces, underscores, hyphens (1-50 chars)
        private val PROJECT_NAME_REGEX = Regex("^[a-zA-Z0-9][a-zA-Z0-9 _-]{0,49}$")
        // Valid package ID: each segment starts with lowercase letter, contains lowercase letters, digits, or underscores
        private val PACKAGE_ID_REGEX = Regex("^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*$")
        // Package ID length limit (reasonable max for Java packages)
        private const val MAX_PACKAGE_ID_LENGTH = 100
        // Valid platforms
        private val VALID_PLATFORMS = setOf("android", "ios", "desktop", "web", "server")
        // Valid UI frameworks per platform
        private val VALID_UI = mapOf(
            "android" to setOf("compose"),
            "ios" to setOf("compose", "swiftui"),
            "desktop" to setOf("compose"),
            "web" to setOf("compose", "react")
        )
    }

    private fun validateProjectName(name: String): String {
        if (!PROJECT_NAME_REGEX.matches(name)) {
            error("Invalid project name: '$name'. Only Latin characters, digits, spaces, '_' and '-' are allowed (1-50 chars)")
        }
        return name
    }

    private fun validatePackageId(pid: String): String {
        if (pid.length > MAX_PACKAGE_ID_LENGTH) {
            error("Package ID too long: ${pid.length} chars (max: $MAX_PACKAGE_ID_LENGTH)")
        }
        if (!PACKAGE_ID_REGEX.matches(pid)) {
            error("Invalid package ID: '$pid'. Must be a valid Java package name (e.g., org.example.app)")
        }
        return pid
    }

    fun parse(args: Array<String>) = runBlocking {
        val options = parseArgs(args)

        if (options.help == true) {
            io.printLine("Usage: kmpli --name=\"CMPProject\" --pid=\"org.cmp.project\" [--platforms=\"TARGETS\"] [--include-tests]")
            io.printLine("")
            io.printLine("Options:")
            io.printLine("  --name           Project name (alphanumeric, hyphens, underscores)")
            io.printLine("  --pid            Package identifier (e.g., org.example.app)")
            io.printLine("  --platforms      Comma-separated list of targets")
            io.printLine("  --template       Predefined project template")
            io.printLine("  --include-tests  Include sample tests")
            io.printLine("  --help, -h       Show this help message")
            io.printLine("")
            io.printLine("Platforms: android, ios, desktop, web, server")
            io.printLine("UI options: compose (default), swiftui (ios), react (web)")
            io.printLine("Templates: shared-ui, native-ui, library, shared-ui-amper, native-ui-amper")
            return@runBlocking
        }

        // Handle template
        options.template?.let { templateName ->
            if (options.name == null) options.name = "CMPProject"
            if (options.pid == null)
                options.pid = "org.cmp.${options.name!!.lowercase().replace(Regex("[^a-z0-9]+"), "")}"

            // Validate inputs
            val validatedName = validateProjectName(options.name!!)
            val validatedPid = validatePackageId(options.pid!!)

            val selectedTemplate = Template.fromId(templateName)
                ?: error("Invalid template name: $templateName\nAvailable: ${Template.availableTemplates()}")

            io.printLine("Generating template: ${selectedTemplate.id} -> ${selectedTemplate.description}")

            val zipBytes = io.httpGetBytes(selectedTemplate.url)
            val extractedDir = io.extractZip(zipBytes, validatedName)
            io.replacePlaceholders(extractedDir, validatedName, validatedPid, selectedTemplate.oldPackageId)

            io.printLine("Project generation complete!")
            return@runBlocking
        }

        // Handle platform-based generation
        val parsedPlatforms = parsePlatforms(options.platforms)
        if (options.name == null) options.name = "CMPProject"
        if (options.pid == null) options.pid = "org.cmp.${options.name!!.lowercase()}"

        // Validate inputs
        val validatedName = validateProjectName(options.name!!)
        val validatedPid = validatePackageId(options.pid!!)

        val url = buildUrl(validatedName, validatedPid, parsedPlatforms, options.includeTests)

        io.printLine("Generating project for platform(s):")
        parsedPlatforms.forEach { io.printLine("• ${it.name}${it.ui?.let { " ($it)" } ?: ""}") }
        if (options.includeTests) io.printLine("• including tests")

        val zipBytes = io.httpGetBytes(url)
        val extractedDir = io.extractZip(zipBytes, validatedName)
        io.replacePlaceholders(extractedDir, validatedName, validatedPid, "org.example.project")

        io.printLine("Project generation complete!")
    }

    private fun parseArgs(args: Array<String>): CliOptions {
        val options = CliOptions()
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            when {
                arg.startsWith("--name") -> options.name = arg.substringAfter("=", args.getOrNull(i + 1) ?: "")
                arg.startsWith("--pid") -> options.pid = arg.substringAfter("=", args.getOrNull(i + 1) ?: "")
                arg.startsWith("--template") -> options.template = arg.substringAfter("=", args.getOrNull(i + 1) ?: "")
                arg.startsWith("--platforms") -> options.platforms =
                    arg.substringAfter("=", args.getOrNull(i + 1) ?: "")

                arg == "--include-tests" -> options.includeTests = true
                arg == "--help" || arg == "-h" -> options.help = true
                else -> io.printLine("Unknown argument: $arg")
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

    data class PlatformConfig(val name: String, val ui: String? = null)

    fun parsePlatforms(input: String?): List<PlatformConfig> {
        val raw = input?.takeIf { it.isNotBlank() } ?: "android(compose),ios(compose)"
        return raw.split(",").map { part ->
            val match = Regex("""(\w+)(\(([^)]+)\))?""").find(part.trim())
            val name = match?.groupValues?.get(1)?.lowercase() ?: error("Invalid platform format: $part")
            val ui = match.groupValues[3].ifBlank { "compose" }.lowercase()

            // Validate platform name
            if (name !in VALID_PLATFORMS) {
                error("Invalid platform: '$name'. Valid platforms: ${VALID_PLATFORMS.joinToString(", ")}")
            }

            // Validate UI framework for platform
            val validUiForPlatform = VALID_UI[name]
            if (validUiForPlatform != null && ui !in validUiForPlatform) {
                error("Invalid UI '$ui' for platform '$name'. Valid options: ${validUiForPlatform.joinToString(", ")}")
            }

            PlatformConfig(name, ui)
        }
    }

    fun buildUrl(name: String, id: String, platforms: List<PlatformConfig>, tests: Boolean): String {
        val targets = buildJsonObject {
            platforms.forEach {
                when (it.name) {
                    "android", "ios", "desktop", "web" -> put(it.name, buildJsonObject {
                        put("ui", JsonArray(listOf(JsonPrimitive(it.ui ?: "compose"))))
                    })

                    "server" -> put("server", buildJsonObject {
                        put("engine", JsonArray(listOf(JsonPrimitive("ktor"))))
                    })
                }
            }
        }

        val spec = buildJsonObject {
            put("template_id", JsonPrimitive("kmt"))
            put("targets", targets)
            if (tests) put("include_tests", JsonPrimitive(true))
        }

        val encodedSpec = io.urlEncode(Json.encodeToString(spec))
        return "https://kmp.jetbrains.com/generateKmtProject?name=${io.urlEncode(name)}&id=${io.urlEncode(id)}&spec=$encodedSpec"
    }
}
