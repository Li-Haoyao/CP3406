package au.edu.jcu.assessment.utilityapp.model

import java.time.LocalDate
import java.time.temporal.ChronoUnit

enum class CollabCategory(val label: String, val code: String, val english: String) {
    ALL("全部", "all", "ALL"), DRINK("茶饮", "drink", "SIP & PLAY"),
    FOOD("餐饮", "food", "TASTE THE QUEST"), FASHION("服饰", "fashion", "WEAR YOUR WORLD"),
    RETAIL("零售", "retail", "LITTLE COLLECTIBLES"), GAME("游戏 / 动漫", "game", "WORLDS COLLIDE");
    companion object {
        fun fromCode(code: String?) = entries.firstOrNull { it.code == code && it != ALL }
    }
}
enum class EventStatus { UPCOMING, ACTIVE, ENDED }
enum class SortOrder(val label: String) { ENDING("即将结束优先"), NEWEST("最新发布优先") }

data class CollabEvent(
    val id: String, val game: String, val brand: String, val title: String, val tagline: String,
    val category: CollabCategory, val regions: List<String>, val startDate: LocalDate,
    val endDate: LocalDate, val publishedDate: LocalDate, val rewards: List<String>,
    val steps: List<String>, val location: String, val requirement: String, val notice: String,
    val sourceUrl: String? = null, val featured: Boolean = false,
    val englishTranslation: EventTranslation? = null,
    val translations: Map<String, EventTranslation> = emptyMap()
) {
    fun status(today: LocalDate): EventStatus = when {
        today.isBefore(startDate) -> EventStatus.UPCOMING
        today.isAfter(endDate) -> EventStatus.ENDED
        else -> EventStatus.ACTIVE
    }
    fun countdown(today: LocalDate): String = when (status(today)) {
        EventStatus.UPCOMING -> "${ChronoUnit.DAYS.between(today, startDate)} 天后开始"
        EventStatus.ENDED -> "活动已结束"
        EventStatus.ACTIVE -> if (today == endDate) "今天结束" else "剩 ${ChronoUnit.DAYS.between(today, endDate)} 天"
    }
}
data class Preferences(
    val region: Region = Region.CHINA, val game: String = "全部游戏",
    val defaultCategory: CollabCategory = CollabCategory.ALL,
    val sortOrder: SortOrder = SortOrder.ENDING, val showEnded: Boolean = false, val darkMode: Boolean = false
)

/** Pure business rules, shared by the feed and the featured selection. */
fun filterEvents(
    events: List<CollabEvent>, today: LocalDate, preferences: Preferences,
    category: CollabCategory = preferences.defaultCategory, query: String = "",
    savedOnly: Boolean = false, savedIds: Set<String> = emptySet()
): List<CollabEvent> {
    val needle = query.trim()
    val filtered = events.filter { event ->
        (preferences.showEnded || event.status(today) != EventStatus.ENDED) &&
        event.regions.any { Region.fromValue(it) == preferences.region } &&
        (preferences.game == "全部游戏" || preferences.game == event.game) &&
        (category == CollabCategory.ALL || category == event.category) &&
        (!savedOnly || event.id in savedIds) &&
        (needle.isEmpty() || (event.translations.values.flatMap { listOf(it.title, it.tagline) } + listOfNotNull(event.game, event.brand, event.title, event.tagline,
            event.englishTranslation?.title, event.englishTranslation?.tagline)).any { it.contains(needle, ignoreCase = true) })
    }
    return when (preferences.sortOrder) {
        SortOrder.NEWEST -> filtered.sortedByDescending { it.publishedDate }
        SortOrder.ENDING -> filtered.sortedWith(compareBy<CollabEvent> {
            when (it.status(today)) { EventStatus.ACTIVE -> 0; EventStatus.UPCOMING -> 1; EventStatus.ENDED -> 2 }
        }.thenBy { if (it.status(today) == EventStatus.UPCOMING) it.startDate else it.endDate })
    }
}
