package au.edu.jcu.assessment.utilityapp.data

import au.edu.jcu.assessment.utilityapp.model.CollabCategory
import au.edu.jcu.assessment.utilityapp.model.CollabEvent
import au.edu.jcu.assessment.utilityapp.model.Region
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import retrofit2.http.GET
import retrofit2.http.Url
import java.time.LocalDate

interface CollabApi { @GET suspend fun getFeed(@Url url: String): FeedDto }
data class FeedDto(val schemaVersion: Int? = null, val isDemo: Boolean? = null,
    val updatedAt: String? = null, val events: List<EventDto>? = null)
data class EventDto(
    val id: String? = null, val game: String? = null, val brand: String? = null,
    val title: String? = null, val tagline: String? = null, val category: String? = null,
    val regions: List<String>? = null, val startDate: String? = null, val endDate: String? = null,
    val publishedDate: String? = null, val rewards: List<String>? = null,
    val steps: List<String>? = null, val location: String? = null,
    val requirement: String? = null, val notice: String? = null,
    val sourceUrl: String? = null, val featured: Boolean? = null)
data class EventCatalog(val events: List<CollabEvent>, val isDemo: Boolean, val updatedAt: String)

interface CollabRepository {
    fun demo(today: LocalDate): EventCatalog
    suspend fun remote(url: String): EventCatalog
}
class DefaultCollabRepository(private val api: CollabApi) : CollabRepository {
    override fun demo(today: LocalDate) = EventCatalog(DemoEvents.create(today), true, "")
    override suspend fun remote(url: String): EventCatalog {
        val parsed = url.trim().toHttpUrlOrNull() ?: invalid(FeedProblem.URL)
        checkFeed(parsed.isHttps && parsed.username.isEmpty() && parsed.password.isEmpty(), FeedProblem.URL)
        val feed = api.getFeed(parsed.toString())
        checkFeed(feed.schemaVersion == 1, FeedProblem.VERSION)
        val isDemo = feed.isDemo ?: invalid(FeedProblem.MISSING_FIELD, "isDemo")
        val rows = feed.events ?: invalid(FeedProblem.MISSING_FIELD, "events")
        checkFeed(rows.size <= 500, FeedProblem.LIMIT)
        val events = rows.map { it.toEvent(isDemo) }
        checkFeed(events.map { it.id }.distinct().size == events.size, FeedProblem.DUPLICATE_ID)
        return EventCatalog(events, isDemo, required(feed.updatedAt, "updatedAt"))
    }
    private fun invalid(problem: FeedProblem, detail: String = ""): Nothing =
        throw FeedValidationException(FeedFailure(problem, detail))
    private fun checkFeed(condition: Boolean, problem: FeedProblem) {
        if (!condition) invalid(problem)
    }
    private fun required(value: String?, name: String): String =
        value?.trim()?.takeIf { it.isNotEmpty() } ?: invalid(FeedProblem.MISSING_FIELD, name)
    private fun date(value: String?, name: String): LocalDate {
        val text = required(value, name)
        return try { LocalDate.parse(text) }
        catch (_: java.time.format.DateTimeParseException) { invalid(FeedProblem.DATES) }
    }
    private fun EventDto.toEvent(isDemo: Boolean): CollabEvent {
        val start = date(startDate, "startDate")
        val end = date(endDate, "endDate")
        checkFeed(!end.isBefore(start), FeedProblem.DATES)
        val kind = CollabCategory.fromCode(category) ?: invalid(FeedProblem.CATEGORY)
        val areas = regions.orEmpty().map { Region.fromValue(it)?.code ?: invalid(FeedProblem.REGIONS) }.distinct()
        checkFeed(areas.isNotEmpty(), FeedProblem.REGIONS)
        val guide = steps.orEmpty().filter { it.isNotBlank() }
        checkFeed(guide.isNotEmpty(), FeedProblem.STEPS)
        val source = sourceUrl?.takeIf { it.isNotBlank() }?.toHttpUrlOrNull()
        checkFeed(sourceUrl.isNullOrBlank() || (source != null && source.isHttps &&
            source.username.isEmpty() && source.password.isEmpty()), FeedProblem.SOURCE_URL)
        checkFeed(isDemo || source != null, FeedProblem.OFFICIAL_SOURCE)
        return CollabEvent(required(id, "id"), required(game, "game"), required(brand, "brand"),
            required(title, "title"), tagline.orEmpty(), kind, areas, start, end,
            date(publishedDate, "publishedDate"), rewards.orEmpty(), guide,
            required(location, "location"), required(requirement, "requirement"), notice.orEmpty(),
            source?.toString(), featured == true)
    }
}
