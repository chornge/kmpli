import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.Test

class KmpliTest {
    private lateinit var kmpli: Kmpli

    @BeforeEach
    fun setUp() {
        kmpli = Kmpli()
    }

    @Test
    fun `test buildUrl with all parameters`() {
        val url = kmpli.buildUrl(
            name = "CMPProject",
            id = "io.chornge.cmpproject",
            android = true,
            ios = true,
            iosui = "compose",
            desktop = true,
            web = true,
            webui = "compose",
            server = true,
            tests = true
        )

        assertTrue(url.startsWith("https://kmp.jetbrains.com/generateKmtProject"))
        assertTrue(url.contains("name=CMPProject"))
        assertTrue(url.contains("id=io.chornge.cmpproject"))
        assertTrue(url.contains("spec="))

        val specEncoded = Regex("""spec=([^&]+)""").find(url)?.groupValues?.get(1)
        assertNotNull(specEncoded)
        val decoded = java.net.URLDecoder.decode(specEncoded, "UTF-8")
        assertTrue(decoded.contains("\"template_id\":\"kmt\""))
        assertTrue(decoded.contains("\"android\""))
        assertTrue(decoded.contains("\"ios\""))
        assertTrue(decoded.contains("\"desktop\""))
        assertTrue(decoded.contains("\"web\""))
        assertTrue(decoded.contains("\"server\""))
        assertTrue(decoded.contains("\"include_tests\":true"))
    }

    @Test
    fun `test extractZip`() {
        val zipFile = File("test.zip")

        // Create a simple zip file for testing
        zipFile.outputStream().use { output ->
            ZipOutputStream(output).use { zipOut ->
                zipOut.putNextEntry(ZipEntry("testFile.txt"))
                zipOut.write("Hello, World!".toByteArray())
                zipOut.closeEntry()
            }
        }

        // Extract the zip file
        val extractedDir = kmpli.extractZip(zipFile, "testProject")
        val extractedFile = File(extractedDir, "testFile.txt")
        assertTrue(extractedFile.exists())
        assertEquals("Hello, World!", extractedFile.readText())

        // Clean up
        zipFile.delete()
        assertFalse(zipFile.exists())
        extractedDir.deleteRecursively()
        assertFalse(extractedDir.exists())
    }

    @Test
    fun `test replacePlaceholders`() {
        val testDir = File("testNested/org/example/project")
        testDir.mkdirs()
        val testFile = File(testDir, "testFile.txt")
        testFile.writeText("KotlinProject is a project with ID org.example.project.")

        // Check if the new directory was created with the correct structure
        kmpli.replacePlaceholders(testDir, "CMPProject", "io.chornge.cmpproject")
        val newDir = File("CMPProject/io/chornge/cmpproject")
        assertTrue(newDir.exists())

        // Check if the file content was updated
        val updatedContent = testFile.readText()
        assertTrue(updatedContent.contains("CMPProject"))
        assertTrue(updatedContent.contains("io.chornge.cmpproject"))
        assertFalse(updatedContent.contains("KotlinProject"))
        assertFalse(updatedContent.contains("org.example.project"))

        // Clean up
        testDir.deleteRecursively()
        assertFalse(testDir.exists())
    }
}