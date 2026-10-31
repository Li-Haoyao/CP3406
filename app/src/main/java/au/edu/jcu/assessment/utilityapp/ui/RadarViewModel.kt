package au.edu.jcu.assessment.utilityapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import au.edu.jcu.assessment.utilityapp.RadarApplication
import au.edu.jcu.assessment.utilityapp.data.*
import au.edu.jcu.assessment.utilityapp.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate

data class RadarUiState(
    val events: List<CollabEvent> = emptyList(), val today: LocalDate = LocalDate.now(),
    val preferences: Preferences = Preferences(), val category: CollabCategory = CollabCategory.ALL,
    val query: String = "", val savedOnly: Boolean = false, val savedIds: Set<String> = emptySet(),
    val isDemo: Boolean = true, val loading: Boolean = false, val error: FeedFailure? = null,
    val updatedAt: String = "", val remoteUrl: String = "",
    val user: UserProfile? = null, val accountConfigured: Boolean = false,
    val accountBusy: Boolean = false, val restoringSession: Boolean = false,
    val accountProblem: AccountProblem? = null, val accountNotice: AccountNotice? = null,
    val accountActionVersion: Int = 0
) {
    val visibleEvents get() = filterEvents(events, today, preferences, category, query, savedOnly, savedIds)
    val games get() = events.filter { event -> event.regions.any { Region.fromValue(it) == preferences.region } }.map { it.game }.distinct()
    val availableSavedCount get() = filterEvents(events, today, preferences.copy(game = "全部游戏"), CollabCategory.ALL, savedOnly = true, savedIds = savedIds).size
}
class RadarViewModel(private val repository: CollabRepository, private val accounts: AccountRepository,
    private val preferencesStore: PreferenceStore) : ViewModel() {
    private val initialPreferences = preferencesStore.read()
    private var guestSavedIds = preferencesStore.guestFavorites()
    private val state = MutableStateFlow(RadarUiState(preferences = initialPreferences,
        category = initialPreferences.defaultCategory, savedIds = guestSavedIds, accountConfigured = accounts.configured))
    val uiState = state.asStateFlow()
    private var loadJob: Job? = null
    init {
        showDemo(resetGame = false)
        val feedUrl = preferencesStore.feedUrl()
        if (feedUrl.isNotBlank()) loadRemote(feedUrl)
        restoreAccount()
        viewModelScope.launch { while (true) { delay(60_000); state.update { it.copy(today = LocalDate.now()) } } }
    }
    fun search(query: String) { state.update { it.copy(query = query) } }
    fun selectCategory(category: CollabCategory) { state.update { it.copy(category = category) } }
    fun showSaved(only: Boolean) { state.update { it.copy(savedOnly = only) } }
    fun dismissError() { state.update { it.copy(error = null) } }
    fun dismissAccountMessage() { state.update { it.copy(accountProblem = null, accountNotice = null) } }
    fun toggleSaved(id: String) {
        if (state.value.accountBusy) return
        val next = id !in state.value.savedIds
        if (state.value.user == null) {
            guestSavedIds = if (next) guestSavedIds + id else guestSavedIds - id
            preferencesStore.saveGuestFavorites(guestSavedIds)
            state.update { it.copy(savedIds = guestSavedIds) }
        } else accountAction(closeDialog = false) {
            val saved = accounts.favorite(id, next)
            state.update { it.copy(savedIds = saved) }
        }
    }
    fun updatePreferences(preferences: Preferences) {
        preferencesStore.save(preferences)
        state.update { it.copy(preferences = preferences,
            category = if (preferences.defaultCategory != it.preferences.defaultCategory) preferences.defaultCategory else it.category) }
    }
    fun selectRegion(region: Region) { updatePreferences(state.value.preferences.copy(region = region, game = "全部游戏")) }
    fun resetFilters() {
        updatePreferences(state.value.preferences.copy(game = "全部游戏", defaultCategory = CollabCategory.ALL))
        state.update { it.copy(category = CollabCategory.ALL, query = "", savedOnly = false) }
    }
    fun showDemo(resetGame: Boolean = true) {
        loadJob?.cancel()
        if (resetGame) preferencesStore.saveFeedUrl("")
        val catalog = repository.demo(LocalDate.now())
        if (resetGame) updatePreferences(state.value.preferences.copy(game = "全部游戏"))
        state.update { it.copy(events = catalog.events, today = LocalDate.now(), isDemo = true, loading = false,
            error = null, updatedAt = catalog.updatedAt, remoteUrl = "") }
    }
    fun refresh() {
        if (state.value.remoteUrl.isBlank()) state.update { it.copy(today = LocalDate.now()) }
        else loadRemote(state.value.remoteUrl)
    }
    fun loadRemote(url: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            state.update { it.copy(loading = true, error = null, remoteUrl = url.trim()) }
            try {
                val catalog = repository.remote(url)
                preferencesStore.saveFeedUrl(url)
                state.update { it.copy(events = catalog.events, isDemo = catalog.isDemo, updatedAt = catalog.updatedAt,
                    remoteUrl = url.trim(), loading = false, today = LocalDate.now()) }
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                val failure = when (e) {
                    is HttpException -> FeedFailure(FeedProblem.HTTP, e.code().toString())
                    is IOException -> FeedFailure(FeedProblem.NETWORK)
                    is FeedValidationException -> e.failure
                    else -> FeedFailure(FeedProblem.FORMAT)
                }
                state.update { it.copy(loading = false, error = failure) }
            }
        }
    }
    private fun applyAccount(snapshot: AccountSnapshot) { state.update { it.copy(user = snapshot.user, savedIds = snapshot.favorites.toSet()) } }
    private fun clearAccount() { state.update { it.copy(user = null, savedIds = guestSavedIds, savedOnly = false) } }
    private fun invalid(problem: AccountProblem) { state.update { it.copy(accountProblem = problem, accountNotice = null) } }
    private fun accountAction(closeDialog: Boolean = true, action: suspend () -> Unit) {
        if (state.value.accountBusy) return
        state.update { it.copy(accountBusy = true, accountProblem = null, accountNotice = null) }
        viewModelScope.launch {
            try {
                action()
                if (closeDialog) state.update { it.copy(accountActionVersion = it.accountActionVersion + 1) }
            } catch (e: CancellationException) { throw e
            } catch (e: AccountException) {
                if (e.problem == AccountProblem.SESSION_EXPIRED) clearAccount()
                invalid(e.problem)
            } catch (_: Exception) { invalid(AccountProblem.SERVER)
            } finally { state.update { it.copy(accountBusy = false, restoringSession = false) } }
        }
    }
    fun restoreAccount() {
        if (!accounts.configured || state.value.accountBusy || state.value.user != null) return
        state.update { it.copy(restoringSession = true) }
        accountAction(false) { accounts.restore()?.let(::applyAccount) }
    }
    fun syncAccount() { accountAction(false) { applyAccount(accounts.reload()) } }
    fun login(email: String, password: String) {
        if (!AccountValidation.email(email)) return invalid(AccountProblem.INVALID_EMAIL)
        if (password.isEmpty()) return invalid(AccountProblem.INVALID_INPUT)
        accountAction { applyAccount(accounts.login(email, password)) }
    }
    fun register(email: String, password: String, confirmation: String, name: String) {
        if (!AccountValidation.displayName(name)) return invalid(AccountProblem.INVALID_NAME)
        if (!AccountValidation.email(email)) return invalid(AccountProblem.INVALID_EMAIL)
        if (!AccountValidation.password(password)) return invalid(AccountProblem.WEAK_PASSWORD)
        if (password != confirmation) return invalid(AccountProblem.PASSWORD_MISMATCH)
        accountAction { applyAccount(accounts.register(email, password, name)) }
    }
    fun rename(name: String) {
        if (!AccountValidation.displayName(name)) return invalid(AccountProblem.INVALID_NAME)
        accountAction { val user = accounts.rename(name); state.update { it.copy(user = user, accountNotice = AccountNotice.PROFILE_SAVED) } }
    }
    fun changePassword(current: String, new: String, confirmation: String) {
        if (current.isEmpty()) return invalid(AccountProblem.INVALID_INPUT)
        if (!AccountValidation.password(new)) return invalid(AccountProblem.WEAK_PASSWORD)
        if (new != confirmation) return invalid(AccountProblem.PASSWORD_MISMATCH)
        accountAction { applyAccount(accounts.changePassword(current, new)); state.update { it.copy(accountNotice = AccountNotice.PASSWORD_CHANGED) } }
    }
    fun logout() { accountAction {
        val revoked = accounts.logout(); clearAccount()
        state.update { it.copy(accountNotice = AccountNotice.SIGNED_OUT, accountProblem = if (revoked) null else AccountProblem.SIGN_OUT_OFFLINE) }
    } }
    fun deleteAccount(password: String) {
        if (password.isEmpty()) return invalid(AccountProblem.INVALID_INPUT)
        accountAction { accounts.delete(password); clearAccount(); state.update { it.copy(accountNotice = AccountNotice.ACCOUNT_DELETED) } }
    }
    companion object {
        val Factory = viewModelFactory { initializer {
            val app = this[APPLICATION_KEY] as RadarApplication
            RadarViewModel(app.container.repository, app.container.accounts, app.container.preferences)
        } }
    }
}
