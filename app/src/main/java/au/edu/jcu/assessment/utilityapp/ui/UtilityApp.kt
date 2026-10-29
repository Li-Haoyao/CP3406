@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package au.edu.jcu.assessment.utilityapp.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import au.edu.jcu.assessment.utilityapp.R
import au.edu.jcu.assessment.utilityapp.data.LanguageSettings
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.edu.jcu.assessment.utilityapp.model.*
import au.edu.jcu.assessment.utilityapp.ui.theme.RadarInk
import au.edu.jcu.assessment.utilityapp.ui.theme.RadarLime
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun UtilityApp(state: RadarUiState, model: RadarViewModel) {
    var selectedTab by rememberSaveable { mutableStateOf("Utility") }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Box(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 22.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
                Row(Modifier.widthIn(max = 520.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp))
                    .background(RadarInk).padding(7.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BottomItem(stringResource(R.string.nav_discover), Icons.Default.Explore, selectedTab == "Utility", Modifier.weight(1f)) { selectedTab = "Utility" }
                    BottomItem(stringResource(R.string.nav_settings), Icons.Default.Person, selectedTab == "Settings", Modifier.weight(1f)) { selectedTab = "Settings" }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 680.dp).fillMaxSize()) {
                when (selectedTab) {
                    "Utility" -> UtilityScreen(state, model)
                    else -> MeScreen(state, model) { model.resetFilters(); model.showSaved(true); selectedTab = "Utility" }
                }
            }
        }
    }
}

@Composable
private fun BottomItem(label: String, icon: ImageVector, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    TextButton(onClick, modifier = modifier.heightIn(min = 48.dp), shape = RoundedCornerShape(23.dp),
        colors = ButtonDefaults.textButtonColors(containerColor = if (selected) RadarLime else Color.Transparent,
            contentColor = if (selected) RadarInk else Color(0xFFE2E7DB))) {
        Icon(icon, null, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text(label)
    }
}

fun categoryIcon(category: CollabCategory): ImageVector = when (category) {
    CollabCategory.ALL -> Icons.Default.Apps
    CollabCategory.DRINK -> Icons.Default.LocalCafe
    CollabCategory.FOOD -> Icons.Default.Restaurant
    CollabCategory.FASHION -> Icons.Default.Checkroom
    CollabCategory.RETAIL -> Icons.Default.ShoppingBag
    CollabCategory.GAME -> Icons.Default.SportsEsports
}

@Composable
fun UtilityScreen(state: RadarUiState, model: RadarViewModel) {
    val language = currentAppLanguage()
    val events = state.visibleEvents.map { it.localized(language) }
    val featured = events.firstOrNull { it.featured && it.status(state.today) != EventStatus.ENDED }
    LazyColumn(contentPadding = PaddingValues(horizontal = 22.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(13.dp), color = RadarInk) {
                    Icon(Icons.Default.Radar, null, Modifier.padding(9.dp).size(24.dp), tint = RadarLime)
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleMedium)
                    Text("COLLAB RADAR", style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                HomeRegionPicker(state.preferences.region, model::selectRegion)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(R.string.home_headline), style = MaterialTheme.typography.headlineLarge)
                Text(stringResource(R.string.home_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            TextField(value = state.query, onValueChange = model::search, singleLine = true,
                placeholder = { Text(stringResource(R.string.search_placeholder), style = MaterialTheme.typography.bodyMedium) },
                leadingIcon = { Icon(Icons.Default.Search, stringResource(R.string.search), Modifier.size(21.dp)) },
                trailingIcon = { if (state.query.isNotEmpty()) IconButton({ model.search("") }) { Icon(Icons.Default.Clear, stringResource(R.string.clear_search)) } },
                shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent))
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                items(CollabCategory.entries) { category ->
                    FilterChip(selected = state.category == category, onClick = { model.selectCategory(category) },
                        label = { Text(categoryLabel(category)) }, leadingIcon = { Icon(categoryIcon(category), null, Modifier.size(16.dp)) },
                        shape = RoundedCornerShape(14.dp),
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RadarInk, selectedLabelColor = RadarLime,
                            selectedLeadingIconColor = RadarLime, containerColor = MaterialTheme.colorScheme.surface))
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (state.isDemo) Icons.Default.Info else Icons.Default.CloudDone, null, Modifier.size(15.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(7.dp))
                Text(if (state.isDemo) stringResource(R.string.demo_banner) else stringResource(R.string.data_updated, state.updatedAt),
                    style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (state.error != null) item { ErrorNotice(localizedError(state.error), model::dismissError) }
        if (state.loading || state.accountBusy) item { LinearProgressIndicator(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary) }
        if (featured != null) item(key = "featured-${featured.id}") {
            FeaturedEvent(featured, state.today, featured.id in state.savedIds, state.isDemo) { model.toggleSaved(featured.id) }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (state.savedOnly) stringResource(R.string.saved_events) else stringResource(R.string.discover_events), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(R.string.event_summary, pluralStringResource(R.plurals.event_count, events.size, events.size), sortLabel(state.preferences.sortOrder)), style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconToggleButton(state.savedOnly, { model.showSaved(it) }) {
                    Icon(if (state.savedOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder, stringResource(R.string.saved_only),
                        tint = if (state.savedOnly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(model::refresh, enabled = !state.loading && state.remoteUrl.isNotBlank()) { Icon(Icons.Default.Refresh, stringResource(R.string.refresh_events)) }
            }
        }
        if (state.accountProblem != null) item { AccountMessages(state, model) }
        if (events.isEmpty()) item {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Icon(Icons.Default.TravelExplore, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
                    Text(stringResource(R.string.empty_subtitle), style = MaterialTheme.typography.bodyMedium)
                    TextButton(model::resetFilters) { Text(stringResource(R.string.clear_filters)) }
                }
            }
        }
        items(events.filter { it.id != featured?.id }, key = { it.id }) { event ->
            EventCard(event, state.today, event.id in state.savedIds, state.isDemo) { model.toggleSaved(event.id) }
        }
        item {
            Text(stringResource(R.string.home_footer), modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

@Composable
private fun FeaturedEvent(event: CollabEvent, today: LocalDate, saved: Boolean, isDemo: Boolean, onSave: () -> Unit) {
    var expanded by rememberSaveable(event.id) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column {
            Column(Modifier.fillMaxWidth().background(Color(0xFFE4EDCF)).padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = RadarInk, shape = RoundedCornerShape(8.dp)) {
                        Text(stringResource(R.string.featured_category, categoryLabel(event.category)), Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = RadarLime, style = MaterialTheme.typography.labelSmall)
                    }
                    Spacer(Modifier.weight(1f))
                    Text(eventCountdown(event, today), color = RadarInk, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(event.category.english, color = Color(0xFF526146), style = MaterialTheme.typography.labelSmall, letterSpacing = 1.2.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(event.title.replace("，", "，\n"), color = RadarInk, style = MaterialTheme.typography.headlineMedium)
                        Spacer(Modifier.height(8.dp))
                        Text("${event.game}\n× ${event.brand}", color = Color(0xFF47533D), style = MaterialTheme.typography.bodyMedium)
                    }
                    CampaignArt(event.category, Modifier.size(120.dp))
                }
            }
            Row(Modifier.padding(start = 18.dp, end = 10.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(event.tagline, style = MaterialTheme.typography.titleMedium)
                    Text(event.rewards.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconToggleButton(saved, { onSave() }) { Icon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, stringResource(R.string.save_event)) }
            }
            TextButton({ expanded = !expanded }, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp)) {
                Text(if (expanded) stringResource(R.string.hide_full_guide) else stringResource(R.string.view_full_guide))
                Spacer(Modifier.width(6.dp)); Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.ArrowForward, null, Modifier.size(17.dp))
            }
            AnimatedVisibility(expanded) { GuideContent(event, isDemo) }
        }
    }
}

@Composable
private fun EventCard(event: CollabEvent, today: LocalDate, saved: Boolean, isDemo: Boolean, onSave: () -> Unit) {
    var expanded by rememberSaveable(event.id) { mutableStateOf(false) }
    Surface(shape = RoundedCornerShape(23.dp), color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f)),
        modifier = Modifier.fillMaxWidth().animateContentSize()) {
        Column {
            Row(Modifier.padding(start = 15.dp, top = 16.dp, end = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).clip(RoundedCornerShape(17.dp)).background(artBackground(event.category))) {
                    CampaignArt(event.category, Modifier.fillMaxSize().padding(5.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("${event.game} × ${event.brand}", style = MaterialTheme.typography.titleMedium)
                    Text(event.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(5.dp))
                    Text("${categoryLabel(event.category)}  /  ${event.regions.map { regionLabel(it) }.joinToString(" · ")}", style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconToggleButton(saved, { onSave() }, modifier = Modifier.size(48.dp)) {
                    Icon(if (saved) Icons.Default.Favorite else Icons.Default.FavoriteBorder, stringResource(R.string.save_named_event, event.game, event.brand), Modifier.size(20.dp))
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                val color = when (event.status(today)) {
                    EventStatus.ACTIVE -> MaterialTheme.colorScheme.secondary
                    EventStatus.UPCOMING -> MaterialTheme.colorScheme.primary
                    EventStatus.ENDED -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                Icon(Icons.Default.AccessTime, null, Modifier.size(14.dp), tint = color)
                Spacer(Modifier.width(5.dp)); Text(eventCountdown(event, today), style = MaterialTheme.typography.labelMedium, color = color)
                Spacer(Modifier.weight(1f))
                TextButton({ expanded = !expanded }) {
                    Text(if (expanded) stringResource(R.string.hide_guide) else stringResource(R.string.guide))
                    Icon(if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, null, Modifier.size(18.dp))
                }
            }
            AnimatedVisibility(expanded) { GuideContent(event, isDemo) }
        }
    }
}

@Composable
private fun GuideContent(event: CollabEvent, isDemo: Boolean) {
    val uriHandler = LocalUriHandler.current
    var linkError by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp).padding(bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val dateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        InfoLine(Icons.Default.CalendarMonth, stringResource(R.string.event_dates), "${event.startDate.format(dateFormat)} — ${event.endDate.format(dateFormat)}")
        InfoLine(Icons.Default.Place, stringResource(R.string.event_location), event.location)
        InfoLine(Icons.Default.ConfirmationNumber, stringResource(R.string.event_requirement), event.requirement)
        Text(stringResource(R.string.guide), style = MaterialTheme.typography.titleMedium)
        event.steps.forEachIndexed { index, step ->
            Row(verticalAlignment = Alignment.Top) {
                Box(Modifier.size(25.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Text("${index + 1}", color = MaterialTheme.colorScheme.onPrimaryContainer, style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.width(10.dp)); Text(step, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            event.rewards.forEach { reward ->
                Surface(shape = RoundedCornerShape(7.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(reward, Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        if (event.notice.isNotBlank()) Text(event.notice, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (event.sourceUrl != null && !isDemo) {
            OutlinedButton({ linkError = runCatching { uriHandler.openUri(event.sourceUrl) }.isFailure }, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.official_details)); Spacer(Modifier.width(6.dp)); Icon(Icons.Default.OpenInNew, null, Modifier.size(16.dp))
            }
        }
        if (linkError) Text(stringResource(R.string.link_error), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun InfoLine(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, null, Modifier.padding(top = 2.dp).size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(9.dp))
        Column { Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant); Text(value, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun MeScreen(state: RadarUiState, model: RadarViewModel, openSaved: () -> Unit) {
    var accountDialog by rememberSaveable { mutableStateOf<String?>(null) }
    var handledAction by rememberSaveable { mutableStateOf(state.accountActionVersion) }
    LaunchedEffect(state.accountActionVersion) {
        if (handledAction != state.accountActionVersion) { accountDialog = null; handledAction = state.accountActionVersion }
    }
    val openAccount: (String) -> Unit = { model.dismissAccountMessage(); accountDialog = it }
    accountDialog?.let { AccountDialog(it, state, model, { accountDialog = null }, openAccount) }
    var endpoint by rememberSaveable { mutableStateOf(state.remoteUrl) }
    val prefs = state.preferences
    val context = LocalContext.current
    val language = currentAppLanguage()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Text(stringResource(R.string.nav_settings).uppercase(), style = MaterialTheme.typography.labelSmall, letterSpacing = 2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(stringResource(R.string.settings_headline), style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(R.string.settings_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        AccountCard(state, model, openAccount)
        SavedEntry(state, openSaved)
        AccountMessages(state, model)
        if (state.accountBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (state.user != null) SettingsGroup(stringResource(R.string.account_security)) {
            TextButton({ openAccount("password") }, Modifier.fillMaxWidth(), enabled = !state.accountBusy) { Text(stringResource(R.string.change_password)) }
            TextButton({ openAccount("logout") }, Modifier.fillMaxWidth(), enabled = !state.accountBusy) { Text(stringResource(R.string.sign_out)) }
            TextButton({ openAccount("delete") }, Modifier.fillMaxWidth(), enabled = !state.accountBusy) { Text(stringResource(R.string.delete_account), color = MaterialTheme.colorScheme.error) }
        }
        SettingsGroup(stringResource(R.string.discovery_preferences)) {
            ChoiceRow(stringResource(R.string.region), prefs.region, Region.entries.map { it to regionLabel(it) }, Icons.Default.Public) { model.selectRegion(it) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChoiceRow(stringResource(R.string.game), prefs.game, (listOf("全部游戏") + state.games).map { it to if (it == "全部游戏") stringResource(R.string.game_all) else it }, Icons.Default.SportsEsports) { model.updatePreferences(prefs.copy(game = it)) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChoiceRow(stringResource(R.string.default_category), prefs.defaultCategory, CollabCategory.entries.map { it to categoryLabel(it) }, Icons.Default.Apps) { model.updatePreferences(prefs.copy(defaultCategory = it)) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ChoiceRow(stringResource(R.string.sort_order), prefs.sortOrder, SortOrder.entries.map { it to sortLabel(it) }, Icons.Default.Sort) { model.updatePreferences(prefs.copy(sortOrder = it)) }
        }
        SettingsGroup(stringResource(R.string.appearance_language)) {
            ChoiceRow(stringResource(R.string.language), language, AppLanguage.entries.map { it to it.nativeName }, Icons.Default.Language) {
                LanguageSettings.select(context, it)
            }
            Text(stringResource(R.string.language_hint), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(stringResource(R.string.show_ended), stringResource(R.string.show_ended_hint), prefs.showEnded, Icons.Default.History) { model.updatePreferences(prefs.copy(showEnded = it)) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            SwitchRow(stringResource(R.string.dark_mode), stringResource(R.string.dark_mode_hint), prefs.darkMode, Icons.Default.DarkMode) { model.updatePreferences(prefs.copy(darkMode = it)) }
        }
        SettingsGroup(stringResource(R.string.event_data)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(if (state.isDemo) stringResource(R.string.data_demo) else stringResource(R.string.data_live), style = MaterialTheme.typography.titleMedium)
                Text(if (state.isDemo) stringResource(R.string.demo_description) else stringResource(R.string.data_updated, state.updatedAt),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(endpoint, { endpoint = it }, modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text(stringResource(R.string.data_endpoint)) }, shape = RoundedCornerShape(13.dp))
                Text(stringResource(R.string.data_hint), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button({ model.loadRemote(endpoint) }, enabled = !state.loading && endpoint.isNotBlank(), modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(13.dp)) {
                    if (state.loading) { CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(Modifier.width(7.dp)) }
                    Text(if (state.loading) stringResource(R.string.loading) else stringResource(R.string.connect_events))
                }
                TextButton({ endpoint = ""; model.showDemo() }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.restore_demo)) }
            }
        }
        if (state.error != null) ErrorNotice(localizedError(state.error), model::dismissError)
        Text(stringResource(R.string.settings_footer), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 16.dp))
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(color = MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(22.dp)) { Column(content = content) }
    }
}

@Composable
private fun <T> ChoiceRow(title: String, value: T, choices: List<Pair<T, String>>, icon: ImageVector, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.fillMaxWidth().clickable { expanded = true }.padding(horizontal = 16.dp, vertical = 19.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp)); Text(title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp)); Text(choices.firstOrNull { it.first == value }?.second.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 128.dp))
            Icon(Icons.Default.KeyboardArrowDown, null, Modifier.size(19.dp))
        }
        DropdownMenu(expanded, { expanded = false }) {
            choices.forEach { (choice, label) -> DropdownMenuItem(text = { Text(label) },
                trailingIcon = { if (choice == value) Icon(Icons.Default.Check, null, Modifier.size(16.dp)) },
                onClick = { onSelect(choice); expanded = false }) }
        }
    }
}

@Composable
private fun SwitchRow(title: String, subtitle: String, checked: Boolean, icon: ImageVector, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(19.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) { Text(title, style = MaterialTheme.typography.bodyLarge); Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        Switch(checked, onChange)
    }
}

@Composable
private fun ErrorNotice(message: String, dismiss: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
        Row(Modifier.padding(start = 13.dp, top = 9.dp, bottom = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(message, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            IconButton(dismiss) { Icon(Icons.Default.Close, stringResource(R.string.dismiss_notice), Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onErrorContainer) }
        }
    }
}
