package io.chornge.kmpli

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.net.URLDecoder
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KmpliTest {
    private lateinit var kmpli: Kmpli

    @BeforeEach
    fun setUp() {
        kmpli = Kmpli()
    }

    @Test
    fun `parsePlatforms defaults to android and ios(compose)`() {
        val platforms = kmpli.parsePlatforms(null)
        assertEquals(2, platforms.size)
        assertEquals("android", platforms[0].name)
        assertEquals(null, platforms[0].ui)
        assertEquals("ios", platforms[1].name)
        assertEquals("compose", platforms[1].ui ?: "compose")
    }

    @Test
    fun `parsePlatforms parses single platform without ui`() {
        val platforms = kmpli.parsePlatforms("web")
        assertEquals(1, platforms.size)
        assertEquals("web", platforms[0].name)
        assertEquals(null, platforms[0].ui)
    }

    @Test
    fun `parsePlatforms parses single platform with ui`() {
        val platforms = kmpli.parsePlatforms("web(react)")
        assertEquals(1, platforms.size)
        assertEquals("web", platforms[0].name)
        assertEquals("react", platforms[0].ui)
    }

    @Test
    fun `parsePlatforms parses multiple platforms with and without ui`() {
        val platforms = kmpli.parsePlatforms("android,ios(swiftui),web(react)")
        assertEquals(3, platforms.size)
        assertEquals("android", platforms[0].name)
        assertNull(platforms[0].ui)

        assertEquals("ios", platforms[1].name)
        assertEquals("swiftui", platforms[1].ui)

        assertEquals("web", platforms[2].name)
        assertEquals("react", platforms[2].ui)
    }

    @Test
    fun `buildUrl encodes name, id, and includes defaults`() {
        val platforms = listOf(
            Kmpli.PlatformConfig("android"),
            Kmpli.PlatformConfig("ios", "swiftui")
        )
        val url = kmpli.buildUrl(
            name = "My Project",
            id = "com.example.test",
            platforms = platforms,
            tests = true
        )

        assertTrue(url.contains("name=My+Project"))
        assertTrue(url.contains("id=com.example.test"))
        assertTrue(url.contains("swiftui"))
        assertTrue(url.contains("android"))
        assertTrue(url.contains("include_tests"))
        assertTrue(url.contains("template_id"))
    }

    @Test
    fun `buildUrl includes correct UI defaults`() {
        val platforms = listOf(
            Kmpli.PlatformConfig("android", null),
            Kmpli.PlatformConfig("web", "react")
        )
        val url = kmpli.buildUrl(
            name = "Sample",
            id = "org.sample",
            platforms = platforms,
            tests = false
        )

        val decodedSpec = URLDecoder.decode(url.substringAfter("&spec="), "UTF-8")
        val json = Json.Default.parseToJsonElement(decodedSpec).jsonObject

        val androidUI = json["targets"]!!.jsonObject["android"]!!.jsonObject["ui"]!!.jsonArray
        val webUI = json["targets"]!!.jsonObject["web"]!!.jsonObject["ui"]!!.jsonArray

        assertEquals(JsonPrimitive("compose"), androidUI[0])
        assertEquals(JsonPrimitive("react"), webUI[0])
    }

    @Test
    fun `buildUrl errors on unsupported platform`() {
        val ex = assertThrows<IllegalStateException> {
            kmpli.buildUrl(
                name = "Test",
                id = "org.test",
                platforms = listOf(Kmpli.PlatformConfig("notarealplatform")),
                tests = false
            )
        }
        assertEquals("Unsupported platform: notarealplatform", ex.message)
    }
}