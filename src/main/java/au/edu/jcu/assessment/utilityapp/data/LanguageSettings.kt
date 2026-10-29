package au.edu.jcu.assessment.utilityapp.data

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import au.edu.jcu.assessment.utilityapp.model.AppLanguage

object LanguageSettings {
    private const val PREFERENCES = "radar_language"
    private const val LANGUAGE_TAG = "language_tag"

    fun storedLanguage(context: Context): AppLanguage = AppLanguage.fromTag(
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(LANGUAGE_TAG, "en")
    )

    /** First launch is English even on a Chinese device. Honor existing system app locales. */
    fun initializePlatform(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val manager = context.getSystemService(LocaleManager::class.java)
            if (manager.applicationLocales.isEmpty) {
                manager.applicationLocales = LocaleList.forLanguageTags(storedLanguage(context).tag)
            }
        }
    }

    /** Custom storage for Android 12 and earlier, restored before AppCompat attaches. */
    fun restoreBeforeActivity(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(storedLanguage(context).tag))
        }
    }

    fun select(context: Context, language: AppLanguage) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString(LANGUAGE_TAG, language.tag).apply()
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
