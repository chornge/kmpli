package io.chornge.kmpli

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Covers the pure label/id logic behind the wizard's selection prompts. These run on
 * every host, so they must not touch a Terminal: CI executes them headlessly.
 */
class TuiWizardTest {

    @Test
    fun templateLabel_roundTripsForEveryTemplate() {
        for (option in TuiWizard.TEMPLATES) {
            val label = TuiWizard.templateLabel(option)
            assertEquals(
                option.id,
                TuiWizard.resolveTemplateId(label),
                "label '$label' must resolve back to '${option.id}'"
            )
        }
    }

    @Test
    fun templateLabel_showsBothIdAndDescription() {
        val option = TuiWizard.TemplateOption("some-id", "Some description")
        val label = TuiWizard.templateLabel(option)
        assertTrue(label.startsWith("some-id"), "label was '$label'")
        assertTrue(label.endsWith("Some description"), "label was '$label'")
    }

    @Test
    fun resolveTemplateId_returnsNullForLabelsThatAreNotTemplates() {
        assertNull(TuiWizard.resolveTemplateId(""))
        assertNull(TuiWizard.resolveTemplateId("not-a-template"))
        assertNull(TuiWizard.resolveTemplateId("← Back"))
        assertNull(TuiWizard.resolveTemplateId("✓ Done"))
    }

    /** shared-ui is a prefix of shared-ui-toolchain; exact matching must not conflate them. */
    @Test
    fun resolveTemplateId_distinguishesPrefixOverlappingIds() {
        val shorter = TuiWizard.TEMPLATES.first { it.id == "shared-ui" }
        val longer = TuiWizard.TEMPLATES.first { it.id == "shared-ui-toolchain" }
        assertEquals("shared-ui", TuiWizard.resolveTemplateId(TuiWizard.templateLabel(shorter)))
        assertEquals("shared-ui-toolchain", TuiWizard.resolveTemplateId(TuiWizard.templateLabel(longer)))
    }

    /**
     * Guards the bug class the exact-match resolver replaced: a substringBefore parse
     * would have truncated an id containing the separator to its first segment.
     */
    @Test
    fun resolveTemplateId_doesNotTruncateAnIdContainingTheSeparator() {
        val label = TuiWizard.templateLabel(TuiWizard.TemplateOption("weird - id", "desc"))
        assertNull(TuiWizard.resolveTemplateId(label))
    }

    /** Every template the wizard offers must be one the CLI can actually resolve. */
    @Test
    fun everyWizardTemplateIsKnownToTheCli() {
        for (option in TuiWizard.TEMPLATES) {
            assertNotNull(
                Template.fromId(option.id),
                "wizard offers '${option.id}' but Template.fromId does not know it"
            )
        }
    }

    @Test
    fun resolveUiFramework_mapsEveryRealIosOption() {
        assertEquals("compose", TuiWizard.resolveUiFramework(TuiWizard.IOS_UI_OPTIONS[0]))
        assertEquals("swiftui", TuiWizard.resolveUiFramework(TuiWizard.IOS_UI_OPTIONS[1]))
    }

    @Test
    fun resolveUiFramework_mapsEveryRealWebOption() {
        assertEquals("compose", TuiWizard.resolveUiFramework(TuiWizard.WEB_UI_OPTIONS[0]))
        assertEquals("react", TuiWizard.resolveUiFramework(TuiWizard.WEB_UI_OPTIONS[1]))
    }

    @Test
    fun resolveUiFramework_neverFallsThroughForARealOption() {
        val valid = setOf("compose", "swiftui", "react")
        for (label in TuiWizard.IOS_UI_OPTIONS + TuiWizard.WEB_UI_OPTIONS) {
            assertTrue(TuiWizard.resolveUiFramework(label) in valid, "unmapped label '$label'")
        }
    }

    @Test
    fun resolveUiFramework_defaultsToComposeForNullOrUnknown() {
        assertEquals("compose", TuiWizard.resolveUiFramework(null))
        assertEquals("compose", TuiWizard.resolveUiFramework(""))
        assertEquals("compose", TuiWizard.resolveUiFramework("Vue - not offered"))
    }

    // --- wizard to CLI contract -------------------------------------------------------
    // The wizard hands its result to Kmpli.parse as an argument array, so the platform
    // string it writes must be one parsePlatforms can read back. Nothing else pins this
    // format, and either side could change it without the other noticing.

    private fun wizardResult(
        platforms: List<TuiWizard.PlatformConfig>? = null,
        templateId: String? = null
    ) = TuiWizard.WizardResult(
        projectName = "My Project",
        packageId = "org.example.app",
        templateId = templateId,
        platforms = platforms,
        includeTests = true
    )

    @Test
    fun wizardPlatforms_roundTripThroughParsePlatforms() {
        val args = buildArgsFromWizardResult(
            wizardResult(
                platforms = listOf(
                    TuiWizard.PlatformConfig("android", "compose"),
                    TuiWizard.PlatformConfig("ios", "swiftui"),
                    TuiWizard.PlatformConfig("web", "react")
                )
            )
        )

        val platformsArg = args.single { it.startsWith("--platforms=") }.substringAfter("=")
        val parsed = Kmpli(NoopPlatform()).parsePlatforms(platformsArg)

        assertEquals(listOf("android", "ios", "web"), parsed.map { it.name })
        assertEquals(listOf("compose", "swiftui", "react"), parsed.map { it.ui })
    }

    @Test
    fun wizardPlatforms_parseArgsReadsBackEveryField() {
        val args = buildArgsFromWizardResult(
            wizardResult(platforms = listOf(TuiWizard.PlatformConfig("desktop", "compose")))
        )

        val options = Kmpli(NoopPlatform()).parseArgs(args)
        assertEquals("My Project", options.name)
        assertEquals("org.example.app", options.pid)
        assertEquals("desktop(compose)", options.platforms)
        assertTrue(options.includeTests)
        assertNull(options.template)
    }

    @Test
    fun wizardTemplate_emitsTemplateAndNoPlatforms() {
        val args = buildArgsFromWizardResult(wizardResult(templateId = "library"))

        val options = Kmpli(NoopPlatform()).parseArgs(args)
        assertEquals("library", options.template)
        assertNull(options.platforms)
        assertNotNull(Template.fromId(options.template!!))
    }

    @Test
    fun wizardBuildSystem_isOmittedAtTheDefaultAndEmittedOtherwise() {
        val platforms = listOf(TuiWizard.PlatformConfig("android", "compose"))

        val gradle = buildArgsFromWizardResult(
            TuiWizard.WizardResult("P", "org.p.app", null, platforms, true, "gradle")
        )
        assertTrue(gradle.none { it.startsWith("--build-system") }, gradle.joinToString(" "))

        val kotlin = buildArgsFromWizardResult(
            TuiWizard.WizardResult("P", "org.p.app", null, platforms, true, "kotlin")
        )
        assertTrue(kotlin.any { it == "--build-system=kotlin" }, kotlin.joinToString(" "))

        // and the CLI must read back what the wizard wrote
        assertEquals("kotlin", Kmpli(NoopPlatform()).parseArgs(kotlin).buildSystem)
    }

    @Test
    fun wizardPlatforms_omittedUiRoundTripsAsCompose() {
        val args = buildArgsFromWizardResult(
            wizardResult(platforms = listOf(TuiWizard.PlatformConfig("server", null)))
        )

        val platformsArg = args.single { it.startsWith("--platforms=") }.substringAfter("=")
        assertEquals("server", platformsArg)
        assertEquals("compose", Kmpli(NoopPlatform()).parsePlatforms(platformsArg).single().ui)
    }
}

/** Stands in for the real Platform: these tests never perform IO. */
private class NoopPlatform : Platform {
    override suspend fun httpGetBytes(url: String): ByteArray = ByteArray(0)
    override fun extractZip(zipBytes: ByteArray, projectName: String): String = ""
    override fun replacePlaceholders(dirPath: String, name: String, pid: String, oldPid: String) {}
    override fun printLine(message: String) {}
    override fun urlEncode(value: String): String = value
}
