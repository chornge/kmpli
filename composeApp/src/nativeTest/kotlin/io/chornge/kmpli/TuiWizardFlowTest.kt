package io.chornge.kmpli

import com.github.ajalt.mordant.terminal.Terminal
import com.github.ajalt.mordant.terminal.TerminalRecorder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Drives the wizard end to end over its numbered-prompt path, which is what users get on any
 * terminal without raw mode.
 *
 * The arrow-key path cannot be exercised here: a [TerminalRecorder] reports raw mode as
 * supported and then never emits a key event, so the wizard would block forever. Hence
 * `interactive = false`.
 */
class TuiWizardFlowTest {

    private fun run(vararg input: String): Pair<TuiWizard.WizardResult?, String> {
        val recorder = TerminalRecorder(width = 88, height = 30)
        recorder.inputLines += input.toList()
        val wizard = TuiWizard(Terminal(terminalInterface = recorder), interactive = false)
        return wizard.run() to recorder.output()
    }

    // Step order: template-or-custom, template, name, package id, tests, confirm.
    private fun templateRun(
        choice: String = "3",
        name: String = "MyApp",
        pid: String = "com.acme.app",
        tests: String = "1",
        confirm: String = "1"
    ) = run("1", choice, name, pid, tests, confirm)

    @Test
    fun templatePath_producesTheSelectedTemplate() {
        val (result, _) = templateRun()
        assertEquals("library", result?.templateId)
        assertEquals("MyApp", result?.projectName)
        assertEquals("com.acme.app", result?.packageId)
        assertTrue(result?.includeTests == true)
        assertNull(result?.platforms, "a template run must not also carry platforms")
    }

    @Test
    fun templatePath_firstOptionIsSharedUi() {
        assertEquals("shared-ui", templateRun(choice = "1").first?.templateId)
    }

    @Test
    fun templatePath_toolchainTemplateIsReachable() {
        assertEquals("shared-ui-toolchain", templateRun(choice = "4").first?.templateId)
    }

    @Test
    fun declining_testsIsCarriedThrough() {
        assertEquals(false, templateRun(tests = "2").first?.includeTests)
    }

    @Test
    fun cancellingAtConfirmation_returnsNothing() {
        assertNull(templateRun(confirm = "3").first)
    }

    @Test
    fun emptyAnswers_fallBackToTheDefaults() {
        val (result, _) = run("1", "3", "", "", "1", "1")
        assertEquals("CMPProject", result?.projectName)
        assertEquals("org.example.cmpproject", result?.packageId)
    }

    @Test
    fun typingBack_returnsToTheTemplateChoice() {
        // "back" at the name step re-asks for a template, so a second choice is consumed.
        val (result, output) = run("1", "3", "back", "1", "MyApp", "com.acme.app", "1", "1")
        assertEquals("shared-ui", result?.templateId, "the second template choice should win")
        assertTrue(output.split("Pick a template").size > 2, "the template prompt should reappear")
    }

    @Test
    fun exhaustedInput_endsTheWizardRatherThanHanging() {
        // A piped run that stops early must terminate. It previously blocked forever.
        assertNull(run("1").first)
    }

    @Test
    fun exhaustedInput_isReportedSeparatelyFromCancelling() {
        // main exits 1 on exhausted input and 0 on a cancel, so the two must stay
        // distinguishable. Both end the wizard with a null result.
        val recorder = TerminalRecorder(width = 88, height = 30)
        recorder.inputLines += listOf("1")
        val wizard = TuiWizard(Terminal(terminalInterface = recorder), interactive = false)

        assertNull(wizard.run())
        assertTrue(wizard.wasInputExhausted, "running out of input should be recorded")
    }

    @Test
    fun completedRun_doesNotLookLikeExhaustedInput() {
        val recorder = TerminalRecorder(width = 88, height = 30)
        recorder.inputLines += listOf("1", "3", "MyApp", "com.acme.app", "1", "1")
        val wizard = TuiWizard(Terminal(terminalInterface = recorder), interactive = false)

        assertEquals("library", wizard.run()?.templateId)
        assertTrue(!wizard.wasInputExhausted, "a completed run consumed no missing input")
    }

    @Test
    fun theNumberedFallbackIsWhatRendered() {
        val (_, output) = templateRun()
        assertTrue(output.contains("Use number to select"), "expected the numbered-prompt hint")
        assertTrue(output.contains("Review your project"), "expected the confirmation summary")
    }
}
