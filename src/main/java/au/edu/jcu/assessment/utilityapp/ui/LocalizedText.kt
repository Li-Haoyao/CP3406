package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.FeedFailure
import au.edu.jcu.assessment.utilityapp.data.FeedProblem
import au.edu.jcu.assessment.utilityapp.model.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun currentAppLanguage(): AppLanguage =
    AppLanguage.fromTag(LocalConfiguration.current.locales[0]?.toLanguageTag())

@Composable
fun categoryLabel(category: CollabCategory): String = stringResource(when (category) {
    CollabCategory.ALL -> R.string.category_all
    CollabCategory.DRINK -> R.string.category_drink
    CollabCategory.FOOD -> R.string.category_food
    CollabCategory.FASHION -> R.string.category_fashion
    CollabCategory.RETAIL -> R.string.category_retail
    CollabCategory.GAME -> R.string.category_game
})

@Composable
fun sortLabel(sort: SortOrder): String = stringResource(when (sort) {
    SortOrder.ENDING -> R.string.sort_ending
    SortOrder.NEWEST -> R.string.sort_newest
})

@Composable
fun regionLabel(region: Region): String = stringResource(when (region) {
    Region.CHINA -> R.string.region_mainland
    Region.MALAYSIA -> R.string.region_malaysia
    Region.SINGAPORE -> R.string.region_singapore
})
@Composable
fun regionLabel(region: String): String = Region.fromValue(region)?.let { regionLabel(it) } ?: region

@Composable
fun eventCountdown(event: CollabEvent, today: LocalDate): String = when (event.status(today)) {
    EventStatus.ENDED -> stringResource(R.string.ended)
    EventStatus.UPCOMING -> {
        val days = ChronoUnit.DAYS.between(today, event.startDate).toInt()
        pluralStringResource(R.plurals.days_to_start, days, days)
    }
    EventStatus.ACTIVE -> if (today == event.endDate) stringResource(R.string.ends_today) else {
        val days = ChronoUnit.DAYS.between(today, event.endDate).toInt()
        pluralStringResource(R.plurals.days_remaining, days, days)
    }
}

@Composable
fun localizedError(failure: FeedFailure): String {
    val message = when (failure.problem) {
        FeedProblem.URL -> stringResource(R.string.error_url)
        FeedProblem.VERSION -> stringResource(R.string.error_version)
        FeedProblem.MISSING_FIELD -> stringResource(R.string.error_missing_field, failure.detail)
        FeedProblem.LIMIT -> stringResource(R.string.error_limit)
        FeedProblem.DUPLICATE_ID -> stringResource(R.string.error_duplicate_id)
        FeedProblem.DATES -> stringResource(R.string.error_dates)
        FeedProblem.CATEGORY -> stringResource(R.string.error_category)
        FeedProblem.REGIONS -> stringResource(R.string.error_regions)
        FeedProblem.STEPS -> stringResource(R.string.error_steps)
        FeedProblem.SOURCE_URL -> stringResource(R.string.error_source_url)
        FeedProblem.OFFICIAL_SOURCE -> stringResource(R.string.error_official_source)
        FeedProblem.HTTP -> stringResource(R.string.error_http, failure.detail)
        FeedProblem.NETWORK -> stringResource(R.string.error_network)
        FeedProblem.FORMAT -> stringResource(R.string.error_format)
    }
    return stringResource(R.string.error_previous_data, message)
}
