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
}
