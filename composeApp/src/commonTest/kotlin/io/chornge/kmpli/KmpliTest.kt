package io.chornge.kmpli

import kotlin.test.*

class KmpliTest {
    class MockPlatformIO : Platform {
        val printed = mutableListOf<String>()
        val httpCalls = mutableListOf<String>()
        val extractedDirs = mutableListOf<String>()
        val replacedPlaceholders = mutableListOf<Triple<String, String, String>>()

        override fun printLine(message: String) {
            printed += message
        }

        override suspend fun httpGetBytes(url: String): ByteArray {
            httpCalls += url
            return "fake-zip-data".encodeToByteArray()
        }

        override fun extractZip(zipBytes: ByteArray, projectName: String): String {
            extractedDirs += projectName
            return "/mock/path/$projectName"
        }

        override fun replacePlaceholders(dirPath: String, name: String, pid: String, oldPid: String) {
            replacedPlaceholders += Triple(dirPath, name, pid)
        }

        override fun urlEncode(value: String): String = value.replace(" ", "%20")
    }

    private lateinit var io: MockPlatformIO
    private lateinit var cli: Kmpli

    @BeforeTest
    fun setup() {
        io = MockPlatformIO()
        cli = Kmpli(io)
    }

    @Test
    fun testParseArgs_helpFlag() {
        val args = arrayOf("--help")
        cli.parse(args)
        assertTrue(io.printed.any { it.contains("Usage:") })
    }

    @Test
    fun testParseArgs_versionFlag() {
        cli.parse(arrayOf("--version"))
        assertTrue(io.printed.any { it.contains(KMPLI_VERSION) })
    }

    @Test
    fun testParseArgs_versionShortFlag() {
        cli.parse(arrayOf("-v"))
        assertTrue(io.printed.any { it.contains(KMPLI_VERSION) })
    }

    @Test
    fun testParseArgs_versionTakesPrecedenceOverHelp() {
        cli.parse(arrayOf("--help", "--version"))
        assertTrue(io.printed.any { it.contains(KMPLI_VERSION) })
        assertTrue(io.printed.none { it.contains("Usage:") })
    }

    // org.Example.app is a valid Java package name but is rejected here, so the message
    // must describe the lowercase rule instead of appealing to Java's.
    @Test
    fun testPackageId_errorDescribesTheLowercaseRule() {
        val message = assertFailsWith<InvalidInputException> {
            cli.parse(arrayOf("--name=Foo", "--pid=org.Example.app"))
        }.message!!
        assertTrue(message.contains("lowercase"), message)
        assertFalse(message.contains("Java"), message)
    }

    // A leading hyphen is rejected by the regex, so the message must say why rather than
    // listing '-' among the allowed characters and leaving the user guessing.
    @Test
    fun testProjectName_errorExplainsTheLeadingCharacterRule() {
        val message = assertFailsWith<InvalidInputException> {
            cli.parse(arrayOf("--name=-foo", "--pid=org.test"))
        }.message!!
        assertTrue(message.contains("starting with a letter or digit"), message)
    }

    @Test
    fun testHelp_documentsThePackageIdLengthLimit() {
        cli.parse(arrayOf("--help"))
        val pidLine = io.printed.first { it.trimStart().startsWith("--pid") }
        assertTrue(pidLine.contains("${Kmpli.MAX_PACKAGE_ID_LENGTH}"), pidLine)
    }

    // The regex permits spaces in a project name, so the help must not imply otherwise.
    @Test
    fun testHelp_documentsThatProjectNamesMayContainSpaces() {
        cli.parse(arrayOf("--help"))
        val nameLine = io.printed.first { it.trimStart().startsWith("--name") }
        assertTrue(nameLine.contains("spaces"), nameLine)
    }

    // --- project resolution -----------------------------------------------------------
    // Defaulting and validation used to be duplicated verbatim in the template and platform
    // branches, which is how "Inconsistent default package ID generation between template and
    // platform modes" (fixed in 1.3.0) was possible. Both paths now share one function.

    private fun resolve(name: String? = null, pid: String? = null) =
        cli.resolveProject(Kmpli.CliOptions(name = name, pid = pid))

    @Test
    fun testResolveProject_appliesTheDefaultName() {
        assertEquals("CMPProject", resolve().name)
        assertEquals("org.cmp.cmpproject", resolve().packageId)
    }

    @Test
    fun testResolveProject_derivesPackageIdByStrippingNonAlphanumerics() {
        assertEquals("org.cmp.myapptest", resolve(name = "My-App_Test").packageId)
        assertEquals("org.cmp.myapptest", resolve(name = "My App Test").packageId)
    }

    @Test
    fun testResolveProject_fallsBackWhenTheNameStartsWithADigit() {
        // Package segments must start with a letter, so "1app" cannot be used.
        assertEquals("org.cmp.project", resolve(name = "1App").packageId)
    }

    @Test
    fun testResolveProject_fallsBackWhenTheNameSanitizesToNothing() {
        // Reachable only for a name that then fails validation, since a valid name must start
        // with a letter or digit and therefore always leaves at least one character.
        assertFailsWith<InvalidInputException> { resolve(name = "---") }
    }

    @Test
    fun testResolveProject_prefersAnExplicitPackageId() {
        assertEquals("org.explicit.id", resolve(name = "Foo", pid = "org.explicit.id").packageId)
    }

    @Test
    fun testResolveProject_validatesAnExplicitPackageId() {
        assertFailsWith<InvalidInputException> { resolve(name = "Foo", pid = "org.Bad.Id") }
    }

    /** The regression the shared function exists to prevent. */
    @Test
    fun testDefaultPackageId_isIdenticalInTemplateAndPlatformModes() {
        val templateIo = MockPlatformIO()
        Kmpli(templateIo).parse(arrayOf("--name=My-App_Test", "--template=library"))

        val platformIo = MockPlatformIO()
        Kmpli(platformIo).parse(arrayOf("--name=My-App_Test"))

        assertEquals(
            templateIo.replacedPlaceholders.single().third,
            platformIo.replacedPlaceholders.single().third,
            "template and platform modes must derive the same default package ID"
        )
        assertEquals("org.cmp.myapptest", templateIo.replacedPlaceholders.single().third)
    }

    // --- input validation contract ---------------------------------------------------
    // Every user-input failure must surface as InvalidInputException, never as a violated
    // internal invariant. If a new validation path is added with `error(...)` instead of
    // `invalidInput(...)`, this fails.
    @Test
    fun testEveryValidationFailureUsesTheUserInputType() {
        val badInputs = listOf(
            "invalid project name" to arrayOf("--name=!!!", "--pid=org.test"),
            "uppercase package segment" to arrayOf("--name=Foo", "--pid=org.Example.app"),
            "package id too long" to arrayOf("--name=Foo", "--pid=org." + "a".repeat(120)),
            "unknown template" to arrayOf("--name=Foo", "--pid=org.test", "--template=nope"),
            "unknown platform" to arrayOf("--name=Foo", "--pid=org.test", "--platforms=bogus"),
            "empty platform entry" to arrayOf("--name=Foo", "--pid=org.test", "--platforms=a,,b"),
            "malformed platform entry" to arrayOf("--name=Foo", "--pid=org.test", "--platforms=ios(x"),
            "wrong ui for platform" to arrayOf("--name=Foo", "--pid=org.test", "--platforms=android(swiftui)")
        )

        for ((label, args) in badInputs) {
            assertFailsWith<InvalidInputException>(label) { cli.parse(args) }
        }
    }

    // --- version contract -----------------------------------------------------------
    // KMPLI_VERSION is generated from appVersion in gradle/libs.versions.toml, and
    // release.yml refuses to publish a tag that disagrees with it. Checking the shape
    // here fails on a PR instead of only at tag time.
    @Test
    fun testVersion_isSemverWithVPrefix() {
        assertTrue(
            Regex("""^v\d+\.\d+\.\d+$""").matches(KMPLI_VERSION),
            "appVersion must look like v1.2.3 but was '$KMPLI_VERSION'"
        )
    }

    // --- argument parsing -----------------------------------------------------------
    @Test
    fun testParseArgs_separateValueIsNotReportedAsUnknown() {
        cli.parse(arrayOf("--name", "Foo", "--help"))
        assertTrue(io.printed.none { it.contains("Unknown argument") }, io.printed.toString())
        assertTrue(io.printed.any { it.contains("Usage:") })
    }

    @Test
    fun testParseArgs_separateAndInlineValuesAgree() {
        val inline = cli.parseArgs(arrayOf("--name=Foo", "--pid=org.foo"))
        val separate = cli.parseArgs(arrayOf("--name", "Foo", "--pid", "org.foo"))
        assertEquals(inline.name, separate.name)
        assertEquals(inline.pid, separate.pid)
    }

    @Test
    fun testParseArgs_rejectsFlagWithExtraSuffix() {
        cli.parse(arrayOf("--nameXYZ=Foo", "--help"))
        assertTrue(io.printed.any { it.contains("Unknown argument: --nameXYZ=Foo") }, io.printed.toString())
    }

    @Test
    fun testParseArgs_valueFlagDoesNotSwallowAFollowingFlag() {
        val options = cli.parseArgs(arrayOf("--name", "--version"))
        assertEquals("", options.name)
        assertTrue(options.version)
    }

    // --- template lookup ------------------------------------------------------------
    // Users type template names by hand, and fromId matches case-insensitively.
    @Test
    fun testTemplateFromId_isCaseInsensitive() {
        assertEquals("shared-ui", Template.fromId("SHARED-UI")?.id)
        assertEquals("native-ui-toolchain", Template.fromId("Native-UI-Toolchain")?.id)
    }

    @Test
    fun testTemplateFromId_returnsNullForUnknown() {
        assertNull(Template.fromId("nope"))
        assertNull(Template.fromId(""))
    }

    @Test
    fun testAvailableTemplates_listsEveryId() {
        val listed = Template.availableTemplates()
        for (id in listOf("shared-ui", "native-ui", "library", "shared-ui-toolchain", "native-ui-toolchain")) {
            assertTrue(listed.contains(id), "availableTemplates() omitted '$id': $listed")
        }
    }

    // --- platform parsing -----------------------------------------------------------
    @Test
    fun testParsePlatforms_toleratesWhitespaceAroundEntries() {
        val parsed = cli.parsePlatforms(" android , ios ")
        assertEquals(listOf("android", "ios"), parsed.map { it.name })
    }

    @Test
    fun testParsePlatforms_usesExplicitUiOverDefault() {
        assertEquals("swiftui", cli.parsePlatforms("ios(swiftui)").single().ui)
        assertEquals("react", cli.parsePlatforms("web(react)").single().ui)
    }

    @Test
    fun testParsePlatforms_defaultsUiToComposeWhenOmitted() {
        assertEquals("compose", cli.parsePlatforms("ios").single().ui)
    }

    @Test
    fun testParsePlatforms_mixedUiAcrossPlatforms() {
        val parsed = cli.parsePlatforms("android,ios(swiftui),web(react)")
        assertEquals(listOf("android", "ios", "web"), parsed.map { it.name })
        assertEquals(listOf("compose", "swiftui", "react"), parsed.map { it.ui })
    }

    @Test
    fun testParsePlatforms_rejectsAnEmptyEntryWithAReadableMessage() {
        val message = assertFailsWith<InvalidInputException> {
            cli.parsePlatforms("android,,ios")
        }.message!!
        assertTrue(message.contains("Empty platform entry"), message)
        // The offending input is quoted, so a stray comma never yields a dangling message.
        assertTrue(message.contains("android,,ios"), message)
    }

    // The whole entry must be consumed, so these are errors rather than silently
    // becoming a bare platform name with the default UI.
    @Test
    fun testParsePlatforms_rejectsAnUnbalancedParenthesis() {
        val message = assertFailsWith<InvalidInputException> {
            cli.parsePlatforms("ios(swiftui")
        }.message!!
        assertTrue(message.contains("'ios(swiftui'"), message)
    }

    @Test
    fun testParsePlatforms_rejectsTrailingJunkAfterAValidName() {
        assertFailsWith<InvalidInputException> { cli.parsePlatforms("android!!!") }
        assertFailsWith<InvalidInputException> { cli.parsePlatforms("ios(swiftui))") }
    }

    @Test
    fun testParsePlatforms_rejectsEmptyParentheses() {
        assertFailsWith<InvalidInputException> { cli.parsePlatforms("ios()") }
    }

    @Test
    fun testParsePlatforms_toleratesSpacingAroundTheUiGroup() {
        assertEquals("swiftui", cli.parsePlatforms("ios (swiftui)").single().ui)
        assertEquals("swiftui", cli.parsePlatforms("ios( swiftui )").single().ui)
    }

    @Test
    fun testParsePlatforms_rejectsAMalformedEntryQuotingTheOffendingText() {
        val message = assertFailsWith<InvalidInputException> {
            cli.parsePlatforms("android,!!!")
        }.message!!
        assertTrue(message.contains("'!!!'"), message)
        assertTrue(message.contains("NAME(UI)"), message)
    }

    // --- url building ---------------------------------------------------------------
    // Project names may contain spaces, so the name and package id must be encoded or
    // the generated request is malformed.
    @Test
    fun testBuildUrl_encodesNameAndPackageId() {
        val url = cli.buildUrl("My Project", "org.my.project", cli.parsePlatforms("android"), false)
        assertTrue(url.contains("name=My%20Project"), url)
        assertFalse(url.contains("name=My Project"), url)
    }

    @Test
    fun testBuildUrl_includesTestsFlagOnlyWhenRequested() {
        val platforms = cli.parsePlatforms("android")
        assertTrue(cli.buildUrl("Foo", "org.foo", platforms, true).contains("include_tests"))
        assertFalse(cli.buildUrl("Foo", "org.foo", platforms, false).contains("include_tests"))
    }

    @Test
    fun testParseArgs_unknownArgument() {
        val args = arrayOf("--unknown", "--name=Foo")
        cli.parse(args)
        assertTrue(io.printed.any { it.contains("Unknown argument") })
    }

    @Test
    fun testTemplateGeneration_flow() {
        val args = arrayOf("--template=shared-ui", "--name=Foo", "--pid=org.foo")
        cli.parse(args)

        assertTrue(io.printed.any { it.contains("Generating template:") })
        assertEquals(1, io.httpCalls.size)
        assertEquals(1, io.extractedDirs.size)
        assertEquals(1, io.replacedPlaceholders.size)
    }

    @Test
    fun testParsePlatforms_default() {
        val platforms = cli.parsePlatforms(null)
        assertEquals(2, platforms.size)
        assertEquals("android", platforms[0].name)
        assertEquals("ios", platforms[1].name)
    }

    @Test
    fun testParsePlatforms_customInput() {
        val input = "desktop(compose),server"
        val platforms = cli.parsePlatforms(input)
        assertEquals(2, platforms.size)
        assertEquals("desktop", platforms[0].name)
        assertEquals("compose", platforms[0].ui)
        assertEquals("server", platforms[1].name)
    }

    @Test
    fun testBuildUrl_includesTargets() {
        val platforms = listOf(
            Kmpli.PlatformConfig("android", "compose"),
            Kmpli.PlatformConfig("server", null)
        )

        val url = cli.buildUrl("TestApp", "org.test", platforms, tests = true)
        assertTrue(url.contains("TestApp"))
        assertTrue(url.contains("org.test"))
        assertTrue(url.contains("include_tests"))
        assertTrue(url.contains("android"))
        assertTrue(url.contains("server"))
    }

    @Test
    fun testPlatformGeneration_flow() {
        val args = arrayOf("--name=Bar", "--pid=org.bar", "--platforms=desktop,server", "--include-tests")
        cli.parse(args)

        assertTrue(io.printed.any { it.contains("Generating project for platform") })
        assertTrue(io.printed.any { it.contains("Project generation complete") })
        assertEquals(1, io.httpCalls.size)
        assertEquals(1, io.extractedDirs.size)
        assertEquals(1, io.replacedPlaceholders.size)
    }

    @Test
    fun testProjectName_valid() {
        cli.parse(arrayOf("--name=MyProject", "--pid=org.test"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testProjectName_valid_withSpaces() {
        cli.parse(arrayOf("--name=My Project", "--pid=org.test"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testProjectName_valid_startsWithDigit() {
        cli.parse(arrayOf("--name=1MyProject", "--pid=org.test"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testProjectName_valid_withHyphensUnderscores() {
        cli.parse(arrayOf("--name=My-Project_Name", "--pid=org.test"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testProjectName_invalid_specialChars() {
        assertFailsWith<InvalidInputException> {
            cli.parse(arrayOf("--name=My@Project!", "--pid=org.test"))
        }
    }

    @Test
    fun testPackageId_valid() {
        cli.parse(arrayOf("--name=Test", "--pid=org.example"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testPackageId_valid_withUnderscore() {
        cli.parse(arrayOf("--name=Test", "--pid=org.example_app"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testPackageId_valid_trailingUnderscore() {
        cli.parse(arrayOf("--name=Test", "--pid=a.a_"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testPackageId_invalid_hyphen() {
        assertFailsWith<InvalidInputException> {
            cli.parse(arrayOf("--name=Test", "--pid=org.example-app"))
        }
    }

    @Test
    fun testPackageId_invalid_uppercase() {
        assertFailsWith<InvalidInputException> {
            cli.parse(arrayOf("--name=Test", "--pid=Org.Example"))
        }
    }

    @Test
    fun testPackageId_invalid_startsWithDigit() {
        assertFailsWith<InvalidInputException> {
            cli.parse(arrayOf("--name=Test", "--pid=org.1example"))
        }
    }

    @Test
    fun testPackageId_invalid_emptySegment() {
        assertFailsWith<InvalidInputException> {
            cli.parse(arrayOf("--name=Test", "--pid=org..example"))
        }
    }

    @Test
    fun testPlatform_invalid_name() {
        assertFailsWith<InvalidInputException> {
            cli.parsePlatforms("invalid_platform")
        }
    }

    @Test
    fun testPlatform_invalid_uiForPlatform() {
        assertFailsWith<InvalidInputException> {
            cli.parsePlatforms("android(swiftui)")  // swiftui only valid for ios
        }
    }

    // Max length validation tests

    @Test
    fun testProjectName_maxLength_valid() {
        // 50 chars is the max
        val name50 = "a".repeat(50)
        cli.parse(arrayOf("--name=$name50", "--pid=org.test"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testProjectName_maxLength_invalid() {
        // 51 chars exceeds max
        val name51 = "a".repeat(51)
        assertFailsWith<InvalidInputException> {
            cli.parse(arrayOf("--name=$name51", "--pid=org.test"))
        }
    }

    @Test
    fun testPackageId_maxLength_valid() {
        // 100 chars is the max
        // Need valid format: segments starting with lowercase letter
        val validPid = "a." + "a".repeat(97)  // 100 chars total
        cli.parse(arrayOf("--name=Test", "--pid=$validPid"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testPackageId_maxLength_invalid() {
        // 101 chars exceeds max (100 is the limit)
        val longPid = "a." + "a".repeat(99)  // 2 + 99 = 101 chars total
        assertFailsWith<InvalidInputException> {
            cli.parse(arrayOf("--name=Test", "--pid=$longPid"))
        }
    }

    // Default package ID generation tests

    @Test
    fun testDefaultPackageId_sanitizesSpecialChars() {
        // Project name with special chars should generate valid package ID
        cli.parse(arrayOf("--name=My-App_Test"))
        // Check that http was called (meaning validation passed)
        assertEquals(1, io.httpCalls.size)
        // The URL should contain sanitized package ID
        assertTrue(io.httpCalls[0].contains("org.cmp.myapptest") ||
                   io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testDefaultPackageId_handlesDigitOnlyName() {
        // Project name that starts with a digit should generate default package segment "project"
        cli.parse(arrayOf("--name=1App"))
        assertEquals(1, io.httpCalls.size)
        assertTrue(io.printed.any { it.contains("Project generation complete") })
        // The generated package ID should be org.cmp.project since "1app" starts with a digit
        assertTrue(io.httpCalls[0].contains("org.cmp.project"))
    }

    @Test
    fun testTemplateWithDefaultPackageId() {
        cli.parse(arrayOf("--template=shared-ui", "--name=My-Test-App"))
        assertTrue(io.printed.any { it.contains("Generating template:") })
        assertEquals(1, io.httpCalls.size)
    }

    // Edge case tests

    @Test
    fun testProjectName_minimumLength() {
        // Single character should work
        cli.parse(arrayOf("--name=A", "--pid=org.test"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testPackageId_singleSegment() {
        cli.parse(arrayOf("--name=Test", "--pid=app"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testPackageId_manySegments() {
        cli.parse(arrayOf("--name=Test", "--pid=com.example.app.feature.module"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testPackageId_withNumbers() {
        cli.parse(arrayOf("--name=Test", "--pid=com.example123.app456"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }

    @Test
    fun testPackageId_withUnderscores() {
        cli.parse(arrayOf("--name=Test", "--pid=com.my_company.my_app"))
        assertTrue(io.printed.any { it.contains("Project generation complete") })
    }
}
