package au.edu.jcu.assessment.utilityapp

import android.app.Application
import au.edu.jcu.assessment.utilityapp.data.LanguageSettings
import au.edu.jcu.assessment.utilityapp.data.CollabApi
import au.edu.jcu.assessment.utilityapp.data.CollabRepository
import au.edu.jcu.assessment.utilityapp.data.DefaultCollabRepository
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Explicit dependency injection: a single repository is passed to the ViewModel. */
class RadarApplication : Application() {
    val container by lazy { AppContainer(this) }
    override fun onCreate() {
        super.onCreate()
        LanguageSettings.initializePlatform(this)
    }
}

class AppContainer(context: android.content.Context) {
    val accounts = au.edu.jcu.assessment.utilityapp.data.AccountRepository(context)
    val preferences = au.edu.jcu.assessment.utilityapp.data.PreferenceStore(context)
    private val client = OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS)
        .connectTimeout(10, TimeUnit.SECONDS).followSslRedirects(false).build()
    private val api = Retrofit.Builder()
        // Retrofit requires this placeholder; @Url supplies each validated HTTPS feed URL.
        .baseUrl("https://example.com/").client(client)
        .addConverterFactory(GsonConverterFactory.create()).build().create(CollabApi::class.java)
    val repository: CollabRepository = DefaultCollabRepository(api)
}
