package io.chornge.kmpli

import kotlinx.coroutines.runBlocking
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = runBlocking {
    try {
        if (args.isEmpty()) {
            // No arguments: launch interactive TUI wizard
            runInteractiveMode()
        } else {
            // Arguments provided: use existing CLI behavior
            Kmpli().parse(args)
        }
    } catch (e: IllegalStateException) {
        // error(...) in Kmpli signals invalid user input. Report the message and exit 1
        // rather than letting it abort with a Kotlin/Native stack trace and SIGABRT.
        println("Error: ${e.message}")
        exitProcess(1)
    } catch (e: Exception) {
        // Unexpected failure. Still no stack trace at the user; name the type so a bug
        // report says something useful.
        println("Error: ${e::class.simpleName}: ${e.message}")
        exitProcess(1)
    }
}

private suspend fun runInteractiveMode() {
    val wizard = TuiWizard()
    val result = wizard.run() ?: return

    wizard.printProgress("Starting project generation...")

    // Build args array from wizard result and delegate to existing Kmpli logic
    val args = buildArgsFromWizardResult(result)
    Kmpli().parse(args)
}

internal fun buildArgsFromWizardResult(result: TuiWizard.WizardResult): Array<String> {
    val argsList = mutableListOf<String>()

    argsList.add("--name=${result.projectName}")
    argsList.add("--pid=${result.packageId}")

    if (result.templateId != null) {
        argsList.add("--template=${result.templateId}")
    } else if (result.platforms != null) {
        val platformsStr = result.platforms.joinToString(",") { config ->
            if (config.ui != null) "${config.name}(${config.ui})" else config.name
        }
        argsList.add("--platforms=$platformsStr")
    }

    if (result.includeTests) {
        argsList.add("--include-tests")
    }

    return argsList.toTypedArray()
}
