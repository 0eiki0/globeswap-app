package tech.eiki.globeswap.ui.world

import android.app.Application
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tech.eiki.globeswap.data.Countries
import tech.eiki.globeswap.data.ServiceLocator
import tech.eiki.globeswap.data.World
import tech.eiki.globeswap.data.userMessage

class WorldViewModel(app: Application) : AndroidViewModel(app) {
    sealed interface State {
        data object Loading : State
        data class Error(val message: String) : State
        data class Loaded(val world: World, val shapes: WorldShapes) : State
    }

    private val _state = MutableStateFlow<State>(State.Loading)
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.value = try {
                val shapes = withContext(Dispatchers.IO) { WorldShapes.load(getApplication()) }
                State.Loaded(ServiceLocator.repository.world(), shapes)
            } catch (e: Exception) {
                Log.w("WorldViewModel", "Weltkarte", e)
                State.Error(e.userMessage())
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldScreen(onBack: () -> Unit, vm: WorldViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Your world") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                WorldViewModel.State.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is WorldViewModel.State.Error -> Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Could not load your map: ${s.message}")
                    TextButton(onClick = vm::load) { Text("Try again") }
                }
                is WorldViewModel.State.Loaded -> WorldContent(s.world, s.shapes)
            }
        }
    }
}

@Composable
internal fun WorldContent(world: World, shapes: WorldShapes) {
    val countries = world.countries
    LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Column(Modifier.padding(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    BigNumber(countries.size, if (countries.size == 1) "country" else "countries")
                    BigNumber(world.continents, if (world.continents == 1) "continent" else "continents")
                    BigNumber(countries.sumOf { it.photos }, "photos received")
                }
                WorldMap(
                    shapes = shapes,
                    highlighted = countries.map { it.country }.toSet(),
                    landColor = MaterialTheme.colorScheme.surfaceVariant,
                    highlightColor = MaterialTheme.colorScheme.primary,
                    borderColor = MaterialTheme.colorScheme.surface,
                    modifier = Modifier.padding(top = 16.dp).clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow).padding(6.dp),
                )
                world.percentile?.let { p ->
                    Text(
                        "You've seen more of the world than $p% of GlobeSwap users.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }
                if (countries.isEmpty()) {
                    Text(
                        "Your map is still empty. Every photo you get from a stranger colors in their country.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    )
                }
            }
        }
        val badges = achievements(world)
        item(key = "badges-title") {
            Text(
                "Achievements · ${badges.count { it.unlocked }} of ${badges.size}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
            )
        }
        items(badges, key = { it.id }) { AchievementRow(it) }
        if (countries.isNotEmpty()) {
            item(key = "countries-title") {
                Text(
                    "Countries",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 4.dp),
                )
            }
        }
        items(countries, key = { it.country }) { c ->
            ListItem(
                leadingContent = { Text(Countries.flag(c.country), fontSize = 28.sp) },
                headlineContent = { Text(Countries.name(c.country)) },
                supportingContent = { Text(Countries.continent(c.continent)) },
                trailingContent = { Text(if (c.photos == 1) "1 photo" else "${c.photos} photos") },
            )
            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
        }
        item {
            Text(
                buildAnnotatedString {
                    append("Countries are worked out from IP addresses on our server (")
                    withLink(
                        LinkAnnotation.Url(
                            "https://db-ip.com",
                            TextLinkStyles(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)),
                        ),
                    ) { append("IP Geolocation by DB-IP") }
                    append("). Map: Natural Earth. People who switched off their country don't appear here.")
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun BigNumber(value: Int, label: String) {
    Column {
        Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun AchievementRow(a: Achievement) {
    ListItem(
        modifier = Modifier.alpha(if (a.unlocked) 1f else 0.55f),
        leadingContent = {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(if (a.unlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) { Text(if (a.unlocked) a.emoji else "🔒", fontSize = 22.sp) }
        },
        headlineContent = { Text(a.title, fontWeight = if (a.unlocked) FontWeight.SemiBold else FontWeight.Normal) },
        supportingContent = {
            Column {
                Text(a.description)
                if (!a.unlocked && a.goal > 1) {
                    LinearProgressIndicator(
                        progress = { a.progress.toFloat() / a.goal },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                    )
                }
            }
        },
        trailingContent = {
            when {
                a.unlocked && a.unlockedAt != null && a.unlockedAt > 0 ->
                    Text(DateUtils.getRelativeTimeSpanString(a.unlockedAt).toString(), style = MaterialTheme.typography.labelSmall)
                !a.unlocked && a.goal > 1 -> Text("${a.progress}/${a.goal}", style = MaterialTheme.typography.labelMedium)
            }
        },
    )
}
