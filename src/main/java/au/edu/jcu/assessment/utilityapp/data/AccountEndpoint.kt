package au.edu.jcu.assessment.utilityapp.data
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
object AccountEndpoint {
    fun parse(value: String, debug: Boolean): String? {
        val url = value.trim().toHttpUrlOrNull() ?: return null
        if (url.username.isNotEmpty() || url.password.isNotEmpty() || url.query != null || url.fragment != null) return null
        if (!url.isHttps && !(debug && url.host in setOf("10.0.2.2", "127.0.0.1", "localhost"))) return null
        return url.toString().let { if (it.endsWith('/')) it else "$it/" }
    }
}
