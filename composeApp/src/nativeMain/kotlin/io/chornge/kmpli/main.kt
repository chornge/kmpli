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
    } catch (e: InvalidInputException) {
        // The user supplied something kmpli cannot accept. Point them at --help, which is only
        // appropriate because this type means a user mistake rather than an internal failure.
        println("Error: ${e.message}")
        println("Run 'kmpli --help' for usage.")
        exitProcess(1)
    } catch (e: Exception) {
        // Environment or internal failure: a missing extraction tool, a network problem, a
        // violated invariant. Still no stack trace at the user, but no --help hint either,
        // because nothing the user types would fix it.
        println("Error: ${e.message ?: e::class.simpleName}")
        exitProcess(1)
    }
}

private suspend fun runInteractiveMode() {
    val wizard = TuiWizard()
    val result = wizard.run()

    if (result == null) {
        // Cancelling is a choice, so it exits 0. Running out of input is not: the wizard never
        // had a usable stdin, so there was nothing to cancel. That happens with `docker run`
        // and no -it, or when kmpli is launched from a script with no arguments, and reporting
        // success there hides the fact that no project was created.
        if (wizard.wasInputExhausted) {
            println("Error: no input available to answer the wizard's prompts.")
            println("Pass flags instead, or attach a terminal (docker run -it ...).")
            println("Run 'kmpli --help' for usage.")
            exitProcess(1)
        }
        return
    }

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

        // Omitted at the default so the generated argument list, and the URL it produces,
        // stay unchanged for Gradle projects.
        if (result.buildSystem != "gradle") {
            argsList.add("--build-system=${result.buildSystem}")
        }
    }

    if (result.includeTests) {
        argsList.add("--include-tests")
    }

    return argsList.toTypedArray()
}
