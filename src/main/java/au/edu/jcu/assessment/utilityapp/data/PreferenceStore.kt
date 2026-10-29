package au.edu.jcu.assessment.utilityapp.data
import android.content.Context
import au.edu.jcu.assessment.utilityapp.model.*

class PreferenceStore(context: Context) {
    private val storage = context.getSharedPreferences("radar_preferences", Context.MODE_PRIVATE)
    fun read() = Preferences(
        region = Region.fromValue(storage.getString("region", "CN")) ?: Region.CHINA,
        game = storage.getString("game", "全部游戏") ?: "全部游戏",
        defaultCategory = CollabCategory.entries.firstOrNull { it.code == storage.getString("category", "all") } ?: CollabCategory.ALL,
        sortOrder = SortOrder.entries.firstOrNull { it.name == storage.getString("sort", "ENDING") } ?: SortOrder.ENDING,
        showEnded = storage.getBoolean("show_ended", false), darkMode = storage.getBoolean("dark", false))
    fun save(p: Preferences) { storage.edit().putString("region", p.region.code).putString("game", p.game)
        .putString("category", p.defaultCategory.code).putString("sort", p.sortOrder.name)
        .putBoolean("show_ended", p.showEnded).putBoolean("dark", p.darkMode).apply() }
    fun guestFavorites(): Set<String> = storage.getStringSet("guest_favorites", emptySet())!!.toSet()
    fun saveGuestFavorites(ids: Set<String>) { storage.edit().putStringSet("guest_favorites", ids.toSet()).apply() }
}
