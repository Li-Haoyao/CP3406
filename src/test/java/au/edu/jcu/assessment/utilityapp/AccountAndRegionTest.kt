package au.edu.jcu.assessment.utilityapp

import au.edu.jcu.assessment.utilityapp.data.AccountEndpoint
import au.edu.jcu.assessment.utilityapp.data.DemoEvents
import au.edu.jcu.assessment.utilityapp.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AccountAndRegionTest {
    private val today = LocalDate.of(2026, 10, 20)
    private val events = DemoEvents.create(today)
    @Test fun `each country returns only available events and changing country preserves saves`() {
        for (region in Region.entries) {
            val result = filterEvents(events, today, Preferences(region = region))
            assertTrue(result.isNotEmpty())
            assertTrue(result.all { region.code in it.regions })
            val saved = filterEvents(events, today, Preferences(region = region), savedOnly = true, savedIds = setOf("demo-tea", "demo-food"))
            assertTrue(saved.any { it.id == "demo-tea" })
            assertEquals(region == Region.CHINA, saved.any { it.id == "demo-food" })
        }
    }
    @Test fun `region aliases normalize but unsupported regions are rejected`() {
        assertEquals(Region.CHINA, Region.fromValue(" 中国大陆 "))
        assertEquals(Region.MALAYSIA, Region.fromValue("my"))
        assertEquals(Region.SINGAPORE, Region.fromValue("Singapore"))
        for (invalid in listOf(null, "线上", "All", "US", "")) assertNull(Region.fromValue(invalid))
    }
    @Test fun `Malay and Tamil have full guides and searchable text`() {
        for (language in listOf(AppLanguage.MALAY, AppLanguage.TAMIL)) {
            assertEquals(language, AppLanguage.fromTag(language.tag + "-MY"))
            for (event in events) {
                val translated = event.localized(language)
                assertNotEquals(event.title, translated.title)
                assertEquals(event.steps.size, translated.steps.size)
                assertEquals(event.rewards.size, translated.rewards.size)
                assertTrue(translated.notice.isNotBlank())
                assertTrue(translated.location.isNotBlank())
                assertEquals(event.id, translated.id)
            }
        }
        assertEquals(listOf("demo-tea"), filterEvents(events, today, Preferences(), query = "cahaya bintang").map { it.id })
        assertEquals(listOf("demo-tea"), filterEvents(events, today, Preferences(), query = "நட்சத்திர").map { it.id })
    }
    @Test fun `account endpoint permits HTTPS and debug loopback only`() {
        assertEquals("https://accounts.example.test/api/", AccountEndpoint.parse("https://accounts.example.test/api", false))
        for (host in listOf("10.0.2.2", "127.0.0.1", "localhost")) {
            assertNotNull(AccountEndpoint.parse("http://$host:8000", true))
            assertNull(AccountEndpoint.parse("http://$host:8000", false))
        }
        for (url in listOf("http://public.example.test", "https://user:secret@example.test/", "https://example.test/?key=secret", "https://example.test/#fragment", "", "file:///tmp/local"))
            assertNull(AccountEndpoint.parse(url, true))
    }
    @Test fun `account input validation accepts international names and rejects invalid values`() {
        assertTrue(AccountValidation.email(" person@example.test "))
        assertFalse(AccountValidation.email("person@localhost"))
        assertFalse(AccountValidation.email("person with spaces@example.test"))
        assertTrue(AccountValidation.displayName("探索者 தமிழ்"))
        assertFalse(AccountValidation.displayName(" \n "))
        assertFalse(AccountValidation.displayName("a".repeat(41)))
        assertTrue(AccountValidation.password("a".repeat(12)))
        assertTrue(AccountValidation.password("a".repeat(128)))
        assertFalse(AccountValidation.password("short"))
        assertFalse(AccountValidation.password("a".repeat(129)))
    }
}
