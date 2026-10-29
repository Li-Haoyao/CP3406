package au.edu.jcu.assessment.utilityapp

import au.edu.jcu.assessment.utilityapp.data.DemoEvents
import au.edu.jcu.assessment.utilityapp.model.*
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class CollabRulesTest {
    private val today = LocalDate.of(2026, 10, 12)
    private val events = DemoEvents.create(today)

    @Test fun `end date remains active and tomorrow is ended`() {
        val event = events.first().copy(startDate = today.minusDays(2), endDate = today)
        assertEquals(EventStatus.ACTIVE, event.status(today))
        assertEquals("今天结束", event.countdown(today))
        assertEquals(EventStatus.ENDED, event.status(today.plusDays(1)))
    }
    @Test fun `upcoming event changes to active on its start date`() {
        val event = events.first().copy(startDate = today.plusDays(3), endDate = today.plusDays(8))
        assertEquals("3 天后开始", event.countdown(today))
        assertEquals(EventStatus.ACTIVE, event.status(today.plusDays(3)))
    }
    @Test fun `region game category and query filters intersect`() {
        val results = filterEvents(events, today, Preferences(region = Region.SINGAPORE, game = "星轨漫游"), CollabCategory.DRINK, "青柚")
        assertEquals(listOf("demo-tea"), results.map { it.id })
    }
    @Test fun `ended records require explicit opt in`() {
        assertEquals(4, filterEvents(events, today, Preferences()).size)
        assertEquals(5, filterEvents(events, today, Preferences(showEnded = true)).size)
    }
    @Test fun `favorites empty state does not leak unselected events`() {
        assertTrue(filterEvents(events, today, Preferences(), savedOnly = true).isEmpty())
        assertEquals(listOf("demo-food"), filterEvents(events, today, Preferences(), savedOnly = true,
            savedIds = setOf("demo-food", "missing")).map { it.id })
    }
    @Test fun `ending sort prioritizes active events before upcoming and expired`() {
        val ids = filterEvents(events, today, Preferences(showEnded = true)).map { it.id }
        assertEquals("demo-game", ids.first())
        assertEquals("demo-ended", ids.last())
        assertTrue(ids.indexOf("demo-fashion") > ids.indexOf("demo-food"))
    }
    @Test fun `no match gives empty result and newest order uses publication date`() {
        assertTrue(filterEvents(events, today, Preferences(), query = "not a matching brand").isEmpty())
        assertEquals("demo-fashion", filterEvents(events, today, Preferences(sortOrder = SortOrder.NEWEST)).first().id)
    }
}
