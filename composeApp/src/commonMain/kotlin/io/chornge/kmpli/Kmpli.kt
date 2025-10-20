package io.chornge.kmpli

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

class Kmpli(private val io: Platform = Platform()) {

    fun parse(args: Array<String>) = runBlocking {
        val options = parseArgs(args)

        if (options.help == true) {
            io.printLine("Usage: ./kmpli --name=\"CMPProject\" --pid=\"org.cmp.project\" [--platforms=\"TARGETS\"] [--include-tests]")
            io.printLine("")
            io.printLine("Options:")
            io.printLine("  --name           Project name")
            io.printLine("  --pid            Package identifier")
            io.printLine("  --platforms      Comma-separated list of targets")
            io.printLine("  --template       Predefined project template (e.g., shared-ui)")
            io.printLine("  --include-tests  Include sample tests")
            io.printLine("  --help, -h       Show this help message")
            return@runBlocking
        }

        // Handle template
        options.template?.let { templateName ->
            if (options.name == null) options.name = "CMPProject"
            if (options.pid == null)
                options.pid = "org.cmp.${options.name!!.lowercase().replace(Regex("[^a-z0-9]+"), "")}"

            val selectedTemplate = ProjectTemplate.fromId(templateName)
                ?: error("Invalid template name: $templateName\nAvailable: ${ProjectTemplate.availableTemplates()}")

            io.printLine("Generating template: ${selectedTemplate.id} -> ${selectedTemplate.description}")

            val zipBytes = io.httpGetBytes(selectedTemplate.url)
            val extractedDir = io.extractZip(zipBytes, options.name!!)
            io.replacePlaceholders(extractedDir, options.name!!, options.pid!!, "com.jetbrains.kmpapp")

            io.printLine("✅ Project generation complete!")
            return@runBlocking
        }

        // Handle platform-based generation
        val parsedPlatforms = parsePlatforms(options.platforms)
        if (options.name == null) options.name = "CMPProject"
        if (options.pid == null) options.pid = "org.cmp.${options.name!!.lowercase()}"

        val url = buildUrl(options.name!!, options.pid!!, parsedPlatforms, options.includeTests)

        io.printLine("Generating project for platform(s):")
        parsedPlatforms.forEach { io.printLine("• ${it.name}${it.ui?.let { " ($it)" } ?: ""}") }
        if (options.includeTests) io.printLine("• including tests")

        val zipBytes = io.httpGetBytes(url)
        val extractedDir = io.extractZip(zipBytes, options.name!!)
        io.replacePlaceholders(extractedDir, options.name!!, options.pid!!, "org.example.project")

        io.printLine("✅ Project generation complete!")
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
            val name = match?.groupValues?.get(1)?.lowercase() ?: error("Invalid: $part")
            val ui = match.groupValues[3].ifBlank { "compose" }
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
