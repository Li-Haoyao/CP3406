package au.edu.jcu.assessment.utilityapp.model
import java.util.Locale

enum class AppLanguage(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"), CHINESE("zh-Hans", "简体中文"),
    MALAY("ms", "Bahasa Melayu"), TAMIL("ta", "தமிழ்");
    companion object {
        fun fromTag(tag: String?): AppLanguage = when (tag?.substringBefore('-')?.substringBefore('_')?.lowercase(Locale.ROOT)) {
            "zh" -> CHINESE; "ms" -> MALAY; "ta" -> TAMIL; else -> ENGLISH
        }
    }
}
data class EventTranslation(
    val title: String, val tagline: String, val rewards: List<String>,
    val steps: List<String>, val location: String, val requirement: String, val notice: String
)
/** Never substitute demo text into a remote record based on its ID. */
fun CollabEvent.localized(language: AppLanguage): CollabEvent {
    val text = when (language) {
        AppLanguage.CHINESE -> null
        AppLanguage.ENGLISH -> englishTranslation
        else -> translations[language.tag]
    } ?: return this
    return copy(title = text.title, tagline = text.tagline, rewards = text.rewards,
        steps = text.steps, location = text.location, requirement = text.requirement, notice = text.notice)
}
