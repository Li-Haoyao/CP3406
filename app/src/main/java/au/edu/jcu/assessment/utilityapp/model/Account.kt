package au.edu.jcu.assessment.utilityapp.model

data class UserProfile(val id: String, val email: String, val displayName: String)
data class AccountSnapshot(val user: UserProfile, val favorites: List<String>)
data class AuthSession(val token: String, val user: UserProfile, val favorites: List<String>)
enum class AccountProblem {
    UNAVAILABLE, NETWORK, INVALID_EMAIL, INVALID_NAME, WEAK_PASSWORD, PASSWORD_MISMATCH,
    INVALID_CREDENTIALS, EMAIL_TAKEN, SESSION_EXPIRED, RATE_LIMITED, STORAGE,
    INVALID_INPUT, SIGN_OUT_OFFLINE, FAVORITES_LIMIT, SERVER
}
class AccountException(val problem: AccountProblem) : Exception(problem.name)
enum class AccountNotice { PROFILE_SAVED, PASSWORD_CHANGED, SIGNED_OUT, ACCOUNT_DELETED }
object AccountValidation {
    fun email(value: String) = value.trim().length <= 254 && Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value.trim())
    fun displayName(value: String) = value.trim().length in 1..40 && value.none { it.code < 32 }
    fun password(value: String) = value.length in 12..128
}
