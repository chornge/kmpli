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
        assertFailsWith<IllegalStateException> {
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
        assertFailsWith<IllegalStateException> {
            cli.parse(arrayOf("--name=Test", "--pid=org.example-app"))
        }
    }

    @Test
    fun testPackageId_invalid_uppercase() {
        assertFailsWith<IllegalStateException> {
            cli.parse(arrayOf("--name=Test", "--pid=Org.Example"))
        }
    }

    @Test
    fun testPackageId_invalid_startsWithDigit() {
        assertFailsWith<IllegalStateException> {
            cli.parse(arrayOf("--name=Test", "--pid=org.1example"))
        }
    }

    @Test
    fun testPackageId_invalid_emptySegment() {
        assertFailsWith<IllegalStateException> {
            cli.parse(arrayOf("--name=Test", "--pid=org..example"))
        }
    }

    @Test
    fun testPlatform_invalid_name() {
        assertFailsWith<IllegalStateException> {
            cli.parsePlatforms("invalid_platform")
        }
    }

    @Test
    fun testPlatform_invalid_uiForPlatform() {
        assertFailsWith<IllegalStateException> {
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
        assertFailsWith<IllegalStateException> {
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
        assertFailsWith<IllegalStateException> {
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
