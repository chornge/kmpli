package io.chornge.kmpli

import com.github.ajalt.mordant.input.InputReceiver
import com.github.ajalt.mordant.input.enterRawMode
import com.github.ajalt.mordant.input.receiveKeyEvents
import com.github.ajalt.mordant.rendering.TextColors.*
import com.github.ajalt.mordant.rendering.TextStyles.*
import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.StringPrompt
import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.fflush
import platform.posix.stdout

/**
 * Interactive TUI wizard for Kmpli project generation.
 * Uses arrow-key navigation where supported, falls back to numbered prompts.
 * Supports back navigation throughout the wizard.
 */
class TuiWizard(
    private val terminal: Terminal = Terminal()
) {
    // Color palette
    private val pink = brightMagenta
    private val highlight = brightCyan
    private val subtle = gray
    private val success = brightGreen
    private val warning = brightYellow
    private val danger = brightRed
    private val muted = gray

    // Check if interactive (arrow-key) mode is available
    private val supportsInteractive: Boolean by lazy {
        try {
            terminal.enterRawMode().close()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Navigation signals
    private sealed class NavResult<out T> {
        data class Next<T>(val value: T) : NavResult<T>()
        object Back : NavResult<Nothing>()
        object Cancel : NavResult<Nothing>()
    }

    companion object {
        private val TEMPLATES = listOf(
            TemplateOption("shared-ui", "Shared Compose UI across all platforms"),
            TemplateOption("native-ui", "Compose + SwiftUI for native iOS feel"),
            TemplateOption("library", "Minimal multiplatform library setup"),
            TemplateOption("shared-ui-amper", "Shared UI with Amper build system"),
            TemplateOption("native-ui-amper", "Native UI with Amper build system")
        )

        private val PLATFORMS = listOf(
            PlatformOption("android", "Android", "compose"),
            PlatformOption("ios", "iOS", "compose"),
            PlatformOption("desktop", "Desktop", "compose"),
            PlatformOption("web", "Web", "compose"),
            PlatformOption("server", "Server", null)
        )

        private const val BACK_OPTION = "← Back"
        private const val DONE_OPTION = "✓ Done"
    }

    data class TemplateOption(val id: String, val description: String)
    data class PlatformOption(val id: String, val displayName: String, val defaultUi: String?)
    data class PlatformConfig(val name: String, val ui: String?)

    data class WizardResult(
        val projectName: String,
        val packageId: String,
        val templateId: String? = null,
        val platforms: List<PlatformConfig>? = null,
        val includeTests: Boolean
    )

    // Wizard state (persists across back navigation)
    private var useTemplate: Boolean = true
    private var templateId: String? = null
    private var selectedPlatforms: MutableList<PlatformConfig> = mutableListOf()
    private var projectName: String = ""  // Empty means use default hint
    private var packageId: String = ""    // Empty means use default hint
    private var includeTests: Boolean = true

    /**
     * Run the interactive wizard and return configuration result.
     * Returns null if user cancels.
     */
    fun run(): WizardResult? {
        printWelcome()

        var step = 1
        while (step <= 5) {
            when (step) {
                1 -> {
                    when (val result = promptTemplateOrCustom()) {
                        is NavResult.Next -> {
                            useTemplate = result.value
                            step = 2
                        }
                        is NavResult.Cancel -> return null
                        is NavResult.Back -> return null // Can't go back from first step
                    }
                }
                2 -> {
                    if (useTemplate) {
                        when (val result = promptSelectTemplate()) {
                            is NavResult.Next -> {
                                templateId = result.value
                                selectedPlatforms.clear()
                                step = 3
                            }
                            is NavResult.Back -> step = 1
                            is NavResult.Cancel -> return null
                        }
                    } else {
                        when (val result = promptSelectPlatforms()) {
                            is NavResult.Next -> {
                                selectedPlatforms = result.value.toMutableList()
                                templateId = null
                                if (selectedPlatforms.isEmpty()) {
                                    terminal.println(danger("  ✗ Please select at least one platform"))
                                    // Stay on step 2
                                } else {
                                    step = 3
                                }
                            }
                            is NavResult.Back -> step = 1
                            is NavResult.Cancel -> return null
                        }
                    }
                }
                3 -> {
                    when (val result = promptProjectName()) {
                        is NavResult.Next -> {
                            projectName = result.value
                            step = 4
                        }
                        is NavResult.Back -> step = 2
                        is NavResult.Cancel -> return null
                    }
                }
                4 -> {
                    when (val result = promptPackageId()) {
                        is NavResult.Next -> {
                            packageId = result.value
                            step = 5
                        }
                        is NavResult.Back -> step = 3
                        is NavResult.Cancel -> return null
                    }
                }
                5 -> {
                    when (val result = promptIncludeTests()) {
                        is NavResult.Next -> {
                            includeTests = result.value
                            // Show confirmation
                            val wizardResult = WizardResult(
                                projectName = projectName,
                                packageId = packageId,
                                templateId = templateId,
                                platforms = if (useTemplate) null else selectedPlatforms,
                                includeTests = includeTests
                            )
                            when (promptConfirmation(wizardResult)) {
                                is NavResult.Next -> return wizardResult
                                is NavResult.Back -> step = 5 // Stay to re-ask tests
                                is NavResult.Cancel -> {
                                    terminal.println()
                                    terminal.println(muted("  Cancelled."))
                                    terminal.println()
                                    return null
                                }
                            }
                        }
                        is NavResult.Back -> step = 4
                        is NavResult.Cancel -> return null
                    }
                }
            }
        }
        return null
    }

    private fun printWelcome() {
        terminal.println()
        terminal.println(subtle("  Create CMP/KMP projects in seconds."))
        terminal.println()
    }

    private fun promptTemplateOrCustom(): NavResult<Boolean> {
        terminal.println(bold("  How would you like to start?"))
        terminal.println()

        val options = listOf(
            "Use a template (recommended)",
            "Custom platform selection"
        )

        return when (val selected = selectOne(options)) {
            options[0] -> NavResult.Next(true)
            options[1] -> NavResult.Next(false)
            null -> NavResult.Cancel
            else -> NavResult.Cancel
        }
    }

    private fun promptSelectTemplate(): NavResult<String> {
        terminal.println()
        terminal.println(bold("  Pick a template"))
        terminal.println()

        val options = TEMPLATES.map { "${it.id} — ${it.description}" } + BACK_OPTION

        return when (val selected = selectOne(options)) {
            BACK_OPTION -> NavResult.Back
            null -> NavResult.Cancel
            else -> {
                val templateId = selected.substringBefore(" —")
                NavResult.Next(templateId)
            }
        }
    }

    private fun promptSelectPlatforms(): NavResult<List<PlatformConfig>> {
        terminal.println()
        terminal.println(bold("  Select platforms"))
        terminal.println(muted("  Toggle platforms on/off, then select Done"))
        terminal.println()

        val selected = mutableSetOf<String>().apply {
            // Default: Android and iOS
            add("android")
            add("ios")
        }
        val platformUis = mutableMapOf<String, String>()
        var lastCursor = 0 // Track cursor position

        while (true) {
            val options = PLATFORMS.map { platform ->
                val isSelected = platform.id in selected
                val check = if (isSelected) success("✓") else muted("○")
                val uiInfo = platformUis[platform.id]?.let { " ${muted("($it)")}" } ?: ""
                "$check ${platform.displayName}$uiInfo"
            } + listOf(DONE_OPTION, BACK_OPTION)

            val (choice, newCursor) = selectOneWithCursor(options, showHint = false, startIndex = lastCursor)
            lastCursor = newCursor

            when {
                choice == null -> return NavResult.Cancel
                choice == BACK_OPTION -> return NavResult.Back
                choice == DONE_OPTION -> {
                    val configs = selected.mapNotNull { id ->
                        val platform = PLATFORMS.find { it.id == id } ?: return@mapNotNull null
                        val ui = platformUis[id] ?: platform.defaultUi
                        PlatformConfig(platform.id, ui)
                    }
                    return NavResult.Next(configs)
                }
                else -> {
                    // Toggle platform
                    val platformName = choice.substringAfter(" ").substringBefore(" (").trim()
                    val platform = PLATFORMS.find { it.displayName == platformName }
                    if (platform != null) {
                        if (platform.id in selected) {
                            selected.remove(platform.id)
                            platformUis.remove(platform.id)
                        } else {
                            selected.add(platform.id)
                            // Prompt for UI if applicable
                            if (platform.id == "ios" || platform.id == "web") {
                                val ui = promptUiFramework(platform.id)
                                if (ui != null) {
                                    platformUis[platform.id] = ui
                                }
                            }
                        }
                    }
                    // Re-display the selection (loop continues)
                }
            }
        }
    }

    private fun promptUiFramework(platformId: String): String? {
        val (name, options) = when (platformId) {
            "ios" -> "iOS" to listOf(
                "Compose — shared UI across platforms",
                "SwiftUI — native iOS experience"
            )
            "web" -> "Web" to listOf(
                "Compose — shared UI across platforms",
                "React — TypeScript/React"
            )
            else -> return null
        }

        terminal.println()
        terminal.println(bold("  $name UI framework"))
        terminal.println()

        val choice = selectOne(options)
        return when {
            choice == null -> "compose"
            choice.startsWith("Compose") -> "compose"
            choice.startsWith("SwiftUI") -> "swiftui"
            choice.startsWith("React") -> "react"
            else -> "compose"
        }
    }

    private fun promptProjectName(): NavResult<String> {
        terminal.println()
        terminal.println(bold("  Configure your project"))
        terminal.println(muted("  Type 'back' to go back"))
        terminal.println()

        // Show current value or default hint
        val hint = projectName.ifEmpty { "CMPProject" }
        terminal.println(muted("  default: $hint"))
        terminal.println()

        val response = StringPrompt(
            prompt = "  ${pink("?")} Project name",
            terminal = terminal,
            default = hint
        ).ask() ?: return NavResult.Cancel

        return when (response.lowercase()) {
            "back", "b" -> NavResult.Back
            "" -> NavResult.Next(hint)
            else -> NavResult.Next(response)
        }
    }

    private fun promptPackageId(): NavResult<String> {
        // Use previously entered package ID, or derive from project name, or use default
        val hint = if (packageId.isNotEmpty()) {
            packageId
        } else {
            val safeName = projectName.lowercase().replace(Regex("[^a-z0-9]+"), "")
            val validSegment = if (safeName.isEmpty() || safeName[0].isDigit()) "app" else safeName
            "org.example.$validSegment"
        }

        // Show hint
        terminal.println(muted("  default: $hint"))
        terminal.println(muted("  Type 'back' to go back"))
        terminal.println()

        val response = StringPrompt(
            prompt = "  ${pink("?")} Package ID",
            terminal = terminal,
            default = hint
        ).ask() ?: return NavResult.Cancel

        return when (response.lowercase()) {
            "back", "b" -> NavResult.Back
            "" -> NavResult.Next(hint)
            else -> NavResult.Next(response)
        }
    }

    private fun promptIncludeTests(): NavResult<Boolean> {
        terminal.println()
        terminal.println(muted("  Type 'back' to go back"))

        val options = listOf(
            "Yes, include sample tests",
            "No tests",
            BACK_OPTION
        )

        terminal.println()

        return when (val choice = selectOne(options)) {
            options[0] -> NavResult.Next(true)
            options[1] -> NavResult.Next(false)
            BACK_OPTION, null -> NavResult.Back
            else -> NavResult.Next(true)
        }
    }

    private fun promptConfirmation(result: WizardResult): NavResult<Boolean> {
        terminal.println()
        terminal.println()
        terminal.println(bold("  Review your project"))
        terminal.println()

        terminal.println(muted("  ─────────────────────────────────────"))
        terminal.println()
        terminal.println("  ${muted("Name")}        ${highlight(result.projectName)}")
        terminal.println("  ${muted("Package")}     ${highlight(result.packageId)}")

        if (result.templateId != null) {
            terminal.println("  ${muted("Template")}    ${highlight(result.templateId)}")
        } else {
            val platformsStr = result.platforms
                ?.joinToString(muted(", ").toString()) { config ->
                    val uiSuffix = if (config.ui != null && config.ui != "compose") {
                        muted("(${config.ui})").toString()
                    } else ""
                    "${highlight(config.name)}$uiSuffix"
                }
                ?: "none"
            terminal.println("  ${muted("Platforms")}   $platformsStr")
        }

        val testsStr = if (result.includeTests) success("yes") else warning("no")
        terminal.println("  ${muted("Tests")}       $testsStr")
        terminal.println()
        terminal.println(muted("  ─────────────────────────────────────"))
        terminal.println()

        val options = listOf(
            "Create project",
            "Edit settings",
            "Cancel"
        )

        return when (selectOne(options)) {
            options[0] -> NavResult.Next(true)
            options[1] -> NavResult.Back
            else -> NavResult.Cancel
        }
    }

    /**
     * Single-select helper with circular navigation.
     * Uses arrow-key navigation when supported, falls back to numbered prompts.
     */
    private fun selectOne(options: List<String>, showHint: Boolean = true, startIndex: Int = 0): String? {
        return selectOneWithCursor(options, showHint, startIndex).first
    }

    /**
     * Single-select helper that also returns the final cursor position.
     * Useful for preserving cursor position in toggle loops.
     */
    private fun selectOneWithCursor(options: List<String>, showHint: Boolean = true, startIndex: Int = 0): Pair<String?, Int> {
        return if (supportsInteractive) {
            selectOneCircularWithCursor(options, startIndex)
        } else {
            if (showHint) {
                terminal.println(muted("  Use number to select"))
                terminal.println()
            }
            options.forEachIndexed { index, option ->
                val num = (index + 1).toString()
                terminal.println("    ${highlight(num)}  $option")
            }
            terminal.println()

            val response = StringPrompt(
                prompt = "  ${pink("?")} Choice",
                terminal = terminal,
                default = "1"
            ).ask() ?: return null to startIndex

            val index = response.toIntOrNull()?.minus(1) ?: return null to startIndex
            options.getOrNull(index) to index
        }
    }

    /**
     * Custom interactive select with circular navigation.
     * Arrow up/down wraps around, Enter selects, Escape/q cancels.
     * Returns both selection and final cursor position.
     */
    private fun selectOneCircularWithCursor(options: List<String>, startIndex: Int = 0): Pair<String?, Int> {
        if (options.isEmpty()) return null to 0

        var cursor = startIndex.coerceIn(0, options.lastIndex)

        // Render the list initially
        renderSelectList(options, cursor)

        return terminal.receiveKeyEvents<Pair<String?, Int>> { event ->
            // Handle Ctrl+C first (ctrl modifier + "c" key)
            if (event.ctrl && event.key == "c") {
                clearSelectList(options.size)
                return@receiveKeyEvents InputReceiver.Status.Finished(null to cursor)
            }

            when (event.key) {
                "ArrowUp", "Up" -> {
                    // Circular: go to last item if at first
                    cursor = if (cursor == 0) options.lastIndex else cursor - 1
                    clearAndRenderSelectList(options, cursor)
                    InputReceiver.Status.Continue
                }
                "ArrowDown", "Down" -> {
                    // Circular: go to first item if at last
                    cursor = if (cursor == options.lastIndex) 0 else cursor + 1
                    clearAndRenderSelectList(options, cursor)
                    InputReceiver.Status.Continue
                }
                "Enter" -> {
                    // Clear the list and return selection with cursor position
                    clearSelectList(options.size)
                    InputReceiver.Status.Finished(options[cursor] to cursor)
                }
                "Escape", "q" -> {
                    clearSelectList(options.size)
                    InputReceiver.Status.Finished(null to cursor)
                }
                "j" -> {
                    cursor = if (cursor == options.lastIndex) 0 else cursor + 1
                    clearAndRenderSelectList(options, cursor)
                    InputReceiver.Status.Continue
                }
                "k" -> {
                    cursor = if (cursor == 0) options.lastIndex else cursor - 1
                    clearAndRenderSelectList(options, cursor)
                    InputReceiver.Status.Continue
                }
                else -> InputReceiver.Status.Continue
            }
        }
    }

    private fun renderSelectList(options: List<String>, cursor: Int) {
        options.forEachIndexed { index, option ->
            val prefix = if (index == cursor) highlight("❯ ") else "  "
            val text = if (index == cursor) highlight(option) else option
            terminal.println("    $prefix$text")
        }
    }

    @OptIn(ExperimentalForeignApi::class)
    private fun clearSelectList(count: Int) {
        // Move cursor up and clear each line using raw output
        // Build the entire escape sequence string and print at once
        val escapeSequence = buildString {
            repeat(count) {
                append("\u001B[A")   // Move up one line
                append("\u001B[2K")  // Clear the entire line
            }
            append("\r") // Return to start of line
        }
        print(escapeSequence)
        fflush(stdout)
    }

    private fun clearAndRenderSelectList(options: List<String>, cursor: Int) {
        clearSelectList(options.size)
        renderSelectList(options, cursor)
    }

    fun printProgress(message: String) {
        terminal.println()
        terminal.println(pink("  ◐ ") + message)
    }

    fun printSuccess(message: String) {
        terminal.println(success("  ✓ ") + message)
    }

    fun printError(message: String) {
        terminal.println(danger("  ✗ ") + message)
    }
}
