package au.edu.jcu.assessment.utilityapp.data

/** Errors remain language-neutral until rendered, so a visible error also switches language. */
enum class FeedProblem {
    URL, VERSION, MISSING_FIELD, LIMIT, DUPLICATE_ID, DATES, CATEGORY,
    REGIONS, STEPS, SOURCE_URL, OFFICIAL_SOURCE, HTTP, NETWORK, FORMAT
}

data class FeedFailure(val problem: FeedProblem, val detail: String = "")
class FeedValidationException(val failure: FeedFailure) : IllegalArgumentException(failure.problem.name)
