package au.edu.jcu.assessment.utilityapp.data
import android.content.Context
import au.edu.jcu.assessment.utilityapp.BuildConfig
import au.edu.jcu.assessment.utilityapp.model.*
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.io.IOException
import java.util.concurrent.TimeUnit

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val email: String, val password: String, val displayName: String)
data class NameRequest(val displayName: String)
data class PasswordRequest(val currentPassword: String, val newPassword: String)
data class DeleteRequest(val password: String)
data class FavoriteRequest(val id: String, val saved: Boolean)
data class FavoriteIds(val favorites: List<String>)
interface AccountApi {
    @POST("auth/register") suspend fun register(@Body body: RegisterRequest): AuthSession
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): AuthSession
    @POST("auth/logout") suspend fun logout(@Header("Authorization") auth: String)
    @GET("me") suspend fun me(@Header("Authorization") auth: String): AccountSnapshot
    @PATCH("me") suspend fun rename(@Header("Authorization") auth: String, @Body body: NameRequest): UserProfile
    @POST("auth/password") suspend fun password(@Header("Authorization") auth: String, @Body body: PasswordRequest): AuthSession
    @POST("me/delete") suspend fun delete(@Header("Authorization") auth: String, @Body body: DeleteRequest)
    @POST("me/favorites") suspend fun favorite(@Header("Authorization") auth: String, @Body body: FavoriteRequest): FavoriteIds
}
class AccountRepository(context: Context) {
    private val endpoint = AccountEndpoint.parse(BuildConfig.ACCOUNT_BASE_URL, BuildConfig.DEBUG)
    val configured get() = endpoint != null
    private val store = SecureSessionStore(context, endpoint.orEmpty())
    private var token: String? = null
    private val api = endpoint?.let {
        Retrofit.Builder().baseUrl(it).client(OkHttpClient.Builder().callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(10, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build())
            .addConverterFactory(GsonConverterFactory.create()).build().create(AccountApi::class.java)
    }
    private fun auth() = "Bearer " + (token ?: throw AccountException(AccountProblem.SESSION_EXPIRED))
    private suspend fun <T> request(block: suspend (AccountApi) -> T): T = withContext(Dispatchers.IO) {
        try { block(api ?: throw AccountException(AccountProblem.UNAVAILABLE)) }
        catch (e: HttpException) {
            val code = runCatching { JsonParser.parseString(e.response()?.errorBody()?.string()).asJsonObject["detail"].asString }.getOrNull()
            val problem = AccountProblem.entries.firstOrNull { it.name == code } ?: when (e.code()) {
                401 -> AccountProblem.SESSION_EXPIRED; 429 -> AccountProblem.RATE_LIMITED
                422 -> AccountProblem.INVALID_INPUT; else -> AccountProblem.SERVER
            }
            if (problem == AccountProblem.SESSION_EXPIRED) clear()
            throw AccountException(problem)
        } catch (e: IOException) { throw AccountException(AccountProblem.NETWORK) }
    }
    private fun clear() { token = null; store.clear() }
    private suspend fun remember(session: AuthSession): AccountSnapshot = withContext(Dispatchers.IO) {
        try { store.save(session.token) } catch (e: Exception) {
            try { api?.logout("Bearer " + session.token) } catch (cancel: CancellationException) { throw cancel } catch (_: Exception) { }
            throw AccountException(AccountProblem.STORAGE)
        }
        token = session.token
        AccountSnapshot(session.user, session.favorites)
    }
    suspend fun restore(): AccountSnapshot? = withContext(Dispatchers.IO) {
        token = store.read()
        if (token == null) null else reload()
    }
    suspend fun reload() = request { it.me(auth()) }
    suspend fun register(email: String, password: String, name: String) = remember(request { it.register(RegisterRequest(email.trim(), password, name.trim())) })
    suspend fun login(email: String, password: String) = remember(request { it.login(LoginRequest(email.trim(), password)) })
    suspend fun rename(name: String) = request { it.rename(auth(), NameRequest(name.trim())) }
    suspend fun changePassword(current: String, new: String) = remember(request { it.password(auth(), PasswordRequest(current, new)) })
    suspend fun favorite(id: String, saved: Boolean) = request { it.favorite(auth(), FavoriteRequest(id, saved)).favorites.toSet() }
    suspend fun logout(): Boolean {
        return try { request { it.logout(auth()) }; true }
        catch (e: AccountException) { e.problem == AccountProblem.SESSION_EXPIRED }
        finally { withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { clear() } }
    }
    suspend fun delete(password: String) { request { it.delete(auth(), DeleteRequest(password)); clear() } }
}
