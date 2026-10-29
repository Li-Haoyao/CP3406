package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.model.*

@Composable
fun HomeRegionPicker(region: Region, onSelect: (Region) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        TextButton({ expanded = true }, modifier = Modifier.widthIn(max = 138.dp), contentPadding = PaddingValues(horizontal = 6.dp)) {
            Icon(Icons.Default.Place, stringResource(R.string.choose_region), Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
            Text(regionLabel(region), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false), style = MaterialTheme.typography.labelMedium)
            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(16.dp))
        }
        DropdownMenu(expanded, { expanded = false }) {
            Region.entries.forEach { option ->
                DropdownMenuItem(text = { Text(regionLabel(option)) }, leadingIcon = { Text(option.code) },
                    trailingIcon = { if (option == region) Icon(Icons.Default.Check, null) },
                    onClick = { onSelect(option); expanded = false })
            }
        }
    }
}

@Composable
fun AccountCard(state: RadarUiState, model: RadarViewModel, open: (String) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(26.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surface.copy(alpha = .65f)) {
                    Box(Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        if (state.user == null) Icon(Icons.Default.PersonOutline, null, Modifier.size(29.dp))
                        else Text(state.user.displayName.take(1).uppercase(), style = MaterialTheme.typography.headlineSmall)
                    }
                }
                Spacer(Modifier.width(13.dp))
                Column(Modifier.weight(1f)) {
                    Text(state.user?.displayName ?: stringResource(R.string.guest_title), style = MaterialTheme.typography.titleLarge)
                    Text(state.user?.email ?: stringResource(R.string.guest_subtitle), style = MaterialTheme.typography.bodySmall)
                }
                if (state.user != null) IconButton({ open("rename") }, enabled = !state.accountBusy) { Icon(Icons.Default.Edit, stringResource(R.string.edit_profile), Modifier.size(20.dp)) }
            }
            if (state.restoringSession) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(9.dp)); Text(stringResource(R.string.restoring_session), style = MaterialTheme.typography.bodySmall)
                }
            } else if (state.user == null) {
                Button({ open("login") }, Modifier.fillMaxWidth(), enabled = !state.accountBusy, shape = RoundedCornerShape(14.dp)) { Text(stringResource(R.string.sign_in)) }
                OutlinedButton({ open("register") }, Modifier.fillMaxWidth(), enabled = !state.accountBusy, shape = RoundedCornerShape(14.dp)) { Text(stringResource(R.string.register)) }
                Text(stringResource(R.string.guest_saves_hint), style = MaterialTheme.typography.bodySmall)
            } else {
                Text(stringResource(R.string.account_sync_hint), style = MaterialTheme.typography.bodySmall)
                OutlinedButton(model::syncAccount, enabled = !state.accountBusy, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Icon(Icons.Default.Sync, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text(stringResource(R.string.sync_account))
                }
            }
        }
    }
}

@Composable
fun SavedEntry(state: RadarUiState, onClick: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) {
        Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.FavoriteBorder, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(13.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.saved_events), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.saved_entry_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(state.availableSavedCount.toString(), style = MaterialTheme.typography.titleLarge)
            Icon(Icons.Default.ChevronRight, null)
        }
    }
}

@Composable
fun accountProblemText(problem: AccountProblem): String = stringResource(when (problem) {
    AccountProblem.UNAVAILABLE -> R.string.account_unavailable
    AccountProblem.NETWORK -> R.string.account_network
    AccountProblem.INVALID_EMAIL -> R.string.invalid_email
    AccountProblem.INVALID_NAME -> R.string.invalid_name
    AccountProblem.WEAK_PASSWORD -> R.string.password_hint
    AccountProblem.PASSWORD_MISMATCH -> R.string.password_mismatch
    AccountProblem.INVALID_CREDENTIALS -> R.string.invalid_credentials
    AccountProblem.EMAIL_TAKEN -> R.string.email_taken
    AccountProblem.SESSION_EXPIRED -> R.string.session_expired
    AccountProblem.RATE_LIMITED -> R.string.rate_limited
    AccountProblem.STORAGE -> R.string.session_storage_error
    AccountProblem.INVALID_INPUT -> R.string.invalid_account_input
    AccountProblem.SIGN_OUT_OFFLINE -> R.string.sign_out_offline
    AccountProblem.FAVORITES_LIMIT -> R.string.favorites_limit
    AccountProblem.SERVER -> R.string.account_server_error
})

@Composable
fun AccountMessages(state: RadarUiState, model: RadarViewModel) {
    val problem = state.accountProblem
    val message = problem?.let { accountProblemText(it) } ?: state.accountNotice?.let { stringResource(when (it) {
        AccountNotice.PROFILE_SAVED -> R.string.profile_saved
        AccountNotice.PASSWORD_CHANGED -> R.string.password_changed
        AccountNotice.SIGNED_OUT -> R.string.signed_out
        AccountNotice.ACCOUNT_DELETED -> R.string.account_deleted
    }) } ?: return
    Surface(color = if (problem != null) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Row(Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                IconButton(model::dismissAccountMessage) { Icon(Icons.Default.Close, stringResource(R.string.dismiss_notice), Modifier.size(18.dp)) }
            }
            if (problem == AccountProblem.NETWORK && state.user == null) TextButton(model::restoreAccount, enabled = !state.accountBusy) { Text(stringResource(R.string.retry_session)) }
        }
    }
}

@Composable
private fun PasswordField(label: Int, value: String, change: (String) -> Unit, enabled: Boolean) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(value, change, Modifier.fillMaxWidth(), enabled = enabled, singleLine = true,
        label = { Text(stringResource(label)) }, shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = { IconButton({ visible = !visible }) { Icon(if (visible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            stringResource(if (visible) R.string.hide_password else R.string.show_password)) } })
}

@Composable
fun AccountDialog(action: String, state: RadarUiState, model: RadarViewModel, dismiss: () -> Unit, switch: (String) -> Unit) {
    var email by rememberSaveable(action) { mutableStateOf("") }
    var name by rememberSaveable(action) { mutableStateOf(state.user?.displayName.orEmpty()) }
    // Passwords deliberately never enter saved instance state or preferences.
    var password by remember(action) { mutableStateOf("") }
    var current by remember(action) { mutableStateOf("") }
    var confirmation by remember(action) { mutableStateOf("") }
    val title = when (action) { "register" -> R.string.register; "rename" -> R.string.edit_profile
        "password" -> R.string.change_password; "logout" -> R.string.sign_out; "delete" -> R.string.delete_account; else -> R.string.sign_in }
    val hint = when (action) { "register" -> R.string.register_hint; "password" -> R.string.change_password_hint
        "logout" -> R.string.sign_out_hint; "delete" -> R.string.delete_account_hint; else -> R.string.login_hint }
    AlertDialog(onDismissRequest = { if (!state.accountBusy) dismiss() }, title = { Text(stringResource(title)) },
        text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (action != "rename") Text(stringResource(hint))
                if (!state.accountConfigured) Text(stringResource(R.string.account_unavailable), color = MaterialTheme.colorScheme.error)
                if (action in listOf("register", "rename")) OutlinedTextField(name, { name = it }, Modifier.fillMaxWidth(), enabled = !state.accountBusy,
                    singleLine = true, label = { Text(stringResource(R.string.display_name)) }, shape = RoundedCornerShape(12.dp))
                if (action in listOf("register", "login")) OutlinedTextField(email, { email = it }, Modifier.fillMaxWidth(), enabled = !state.accountBusy,
                    singleLine = true, label = { Text(stringResource(R.string.email)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email), shape = RoundedCornerShape(12.dp))
                if (action == "password") PasswordField(R.string.current_password, current, { current = it }, !state.accountBusy)
                if (action in listOf("register", "login", "password", "delete")) PasswordField(if (action == "password") R.string.new_password else R.string.password, password, { password = it }, !state.accountBusy)
                if (action in listOf("register", "password")) {
                    PasswordField(R.string.confirm_password, confirmation, { confirmation = it }, !state.accountBusy)
                    Text(stringResource(R.string.password_hint), style = MaterialTheme.typography.bodySmall)
                }
                if (state.accountProblem != null) Text(accountProblemText(state.accountProblem), color = MaterialTheme.colorScheme.error)
                if (state.accountBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
                if (action == "login" || action == "register") TextButton({ switch(if (action == "login") "register" else "login") }, enabled = !state.accountBusy) {
                    Text(stringResource(if (action == "login") R.string.need_account else R.string.have_account))
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when (action) {
                    "register" -> model.register(email, password, confirmation, name)
                    "rename" -> model.rename(name)
                    "password" -> model.changePassword(current, password, confirmation)
                    "logout" -> model.logout()
                    "delete" -> model.deleteAccount(password)
                    else -> model.login(email, password)
                }
            }, enabled = state.accountConfigured && !state.accountBusy) {
                Text(stringResource(if (action == "rename") R.string.save_changes else title),
                    color = if (action == "delete") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        }, dismissButton = { TextButton(dismiss, enabled = !state.accountBusy) { Text(stringResource(R.string.cancel)) } })
}
