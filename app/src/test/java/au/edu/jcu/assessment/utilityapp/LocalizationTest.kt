package au.edu.jcu.assessment.utilityapp

import au.edu.jcu.assessment.utilityapp.data.DemoEvents
import au.edu.jcu.assessment.utilityapp.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class LocalizationTest {
    private val today = LocalDate.of(2026, 10, 15)
    private val events = DemoEvents.create(today)

    @Test fun `new or unsupported language defaults to English`() {
        for (tag in listOf(null, "", "en-US", "fr"))
            assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag(tag))
        for (tag in listOf("zh-Hans", "zh_CN"))
            assertEquals(AppLanguage.CHINESE, AppLanguage.fromTag(tag))
    }

    @Test fun `every local demo has a complete English guide without changing identity`() {
        for (event in events) {
            assertNotNull(event.englishTranslation)
            val english = event.localized(AppLanguage.ENGLISH)
            assertFalse(english.steps.isEmpty())
            assertTrue(english.location.isNotBlank())
            assertTrue(english.requirement.isNotBlank())
            assertNotEquals(event.title, english.title)
            assertEquals(event.id, english.id)
            assertEquals(event.game, english.game)
            assertEquals(event.brand, english.brand)
            assertEquals(event.regions, english.regions)
            assertEquals(event.startDate, english.startDate)
            assertEquals(event.endDate, english.endDate)
            assertSame(event, event.localized(AppLanguage.CHINESE))
        }
    }

    @Test fun `language change preserves region category and favorites filtering`() {
        val prefs = Preferences(region = Region.SINGAPORE, game = "星轨漫游")
        for (language in AppLanguage.entries) {
            val result = filterEvents(events, today, prefs, CollabCategory.DRINK,
                savedOnly = true, savedIds = setOf("demo-tea"))
                .map { it.localized(language) }
            assertEquals(listOf("demo-tea"), result.map { it.id })
        }
    }

    @Test fun `English search finds translated demo text`() {
        val result = filterEvents(events, today, Preferences(), query = "starlight")
        assertEquals(listOf("demo-tea"), result.map { it.id })
    }

    @Test fun `Chinese and English queries remain valid when display language changes`() {
        for (language in AppLanguage.entries) {
            for (query in listOf("星光", "starlight")) {
                val result = filterEvents(events, today, Preferences(), query = query)
                    .map { it.localized(language) }
                assertEquals(listOf("demo-tea"), result.map { it.id })
            }
        }
    }

    @Test fun `untranslated remote content never receives demo text even with same id`() {
        val remote = events.first().copy(title = "Remote event", steps = listOf("Remote instructions"),
            englishTranslation = null, translations = emptyMap())
        for (language in AppLanguage.entries) assertSame(remote, remote.localized(language))
    }
}
