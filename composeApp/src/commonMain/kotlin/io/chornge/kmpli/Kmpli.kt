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
        internal const val MAX_PACKAGE_ID_LENGTH = 100
        // Flags that take a value, either as --flag=value or --flag value
        private val VALUE_FLAGS = setOf("--name", "--pid", "--template", "--platforms")
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
            error(
                "Invalid project name: '$name'. Use 1-50 characters of Latin letters, digits, " +
                    "spaces, '_' and '-', starting with a letter or digit"
            )
        }
        return name
    }

    private fun validatePackageId(pid: String): String {
        if (pid.length > MAX_PACKAGE_ID_LENGTH) {
            error("Package ID too long: ${pid.length} chars (max: $MAX_PACKAGE_ID_LENGTH)")
        }
        if (!PACKAGE_ID_REGEX.matches(pid)) {
            error(
                "Invalid package ID: '$pid'. Use dot-separated lowercase segments of letters, " +
                    "digits and '_', each starting with a letter (e.g. org.example.app)"
            )
        }
        return pid
    }

    fun parse(args: Array<String>) = runBlocking {
        val options = parseArgs(args)

        if (options.version) {
            io.printLine("kmpli $KMPLI_VERSION")
            return@runBlocking
        }

        if (options.help == true) {
            io.printLine("Usage: kmpli --name=\"CMPProject\" --pid=\"org.cmp.project\" [--platforms=\"TARGETS\"] [--include-tests]")
            io.printLine("")
            io.printLine("Options:")
            io.printLine("  --name           Project name: letters, digits, spaces, '_' and '-'")
            io.printLine("                   (1-50 chars, must start with a letter or digit)")
            io.printLine("  --pid            Package identifier, e.g. org.example.app (max 100 chars)")
            io.printLine("  --platforms      Comma-separated list of targets")
            io.printLine("  --template       Predefined project template")
            io.printLine("  --include-tests  Include sample tests")
            io.printLine("  --help, -h       Show this help message")
            io.printLine("  --version, -v    Show the installed kmpli version")
            io.printLine("")
            io.printLine("Platforms: android, ios, desktop, web, server")
            io.printLine("UI options: compose (default), swiftui (ios), react (web)")
            io.printLine("Templates: shared-ui, native-ui, library, shared-ui-amper, native-ui-amper")
            return@runBlocking
        }

        // Handle template
        options.template?.let { templateName ->
            if (options.name == null) options.name = "CMPProject"
            if (options.pid == null) {
                val safeName = options.name!!.lowercase().replace(Regex("[^a-z0-9]+"), "")
                // Package segments must start with a letter, not a digit
                val validSegment = if (safeName.isEmpty() || safeName[0].isDigit()) "project" else safeName
                options.pid = "org.cmp.$validSegment"
            }

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
        if (options.pid == null) {
            val safeName = options.name!!.lowercase().replace(Regex("[^a-z0-9]+"), "")
            // Package segments must start with a letter, not a digit
            val validSegment = if (safeName.isEmpty() || safeName[0].isDigit()) "project" else safeName
            options.pid = "org.cmp.$validSegment"
        }

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

    internal fun parseArgs(args: Array<String>): CliOptions {
        val options = CliOptions()
        var i = 0
        while (i < args.size) {
            val arg = args[i]
            // Flags are matched exactly, so a typo like --nameXYZ is reported rather than
            // silently accepted as --name.
            val flag = arg.substringBefore("=")
            val inlineValue = if (arg.contains("=")) arg.substringAfter("=") else null
            // A value may instead follow as the next argument. Only consume it when it cannot
            // itself be a flag, so "--name --help" still honours --help. No valid name, package
            // id, template or platform list starts with "-".
            val nextValue = args.getOrNull(i + 1)?.takeUnless { it.startsWith("-") }
            // Advance past a separately supplied value so it is not re-read as a stray argument.
            val consumed = if (flag in VALUE_FLAGS && inlineValue == null && nextValue != null) 2 else 1
            val value = inlineValue ?: nextValue ?: ""

            when (flag) {
                "--name" -> options.name = value
                "--pid" -> options.pid = value
                "--template" -> options.template = value
                "--platforms" -> options.platforms = value
                "--include-tests" -> options.includeTests = true
                "--help", "-h" -> options.help = true
                "--version", "-v" -> options.version = true
                else -> io.printLine("Unknown argument: $arg")
            }
            i += consumed
        }
        return options
    }

    data class CliOptions(
        var name: String? = null,
        var pid: String? = null,
        var template: String? = null,
        var platforms: String? = null,
        var includeTests: Boolean = false,
        var help: Boolean? = null,
        var version: Boolean = false
    )

    data class PlatformConfig(val name: String, val ui: String? = null)

    fun parsePlatforms(input: String?): List<PlatformConfig> {
        val raw = input?.takeIf { it.isNotBlank() } ?: "android(compose),ios(compose)"
        return raw.split(",").map { part ->
            val entry = part.trim()
            // matchEntire, not find: the whole entry must be consumed, so trailing junk and an
            // unbalanced parenthesis are rejected instead of silently dropped (which previously
            // turned "ios(swiftui" into ios with the default UI). \s* keeps benign spacing such
            // as "ios (swiftui)" working. Group 1 is the name, group 3 the UI.
            val match = Regex("""(\w+)\s*(\(\s*([^)\s]+)\s*\))?""").matchEntire(entry)
            // An unmatchable entry is reported with the offending text quoted, so an empty
            // one (from a stray comma) reads as a missing entry rather than a blank message.
            val name = match?.groupValues?.get(1)?.lowercase() ?: error(
                if (entry.isEmpty()) {
                    "Empty platform entry in '$raw'. Expected a comma-separated list, " +
                        "e.g. \"android,ios(swiftui)\"."
                } else {
                    "Invalid platform entry: '$entry'. Expected NAME or NAME(UI), " +
                        "e.g. \"android\" or \"ios(swiftui)\"."
                }
            )
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
