package tech.eiki.globeswap.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import tech.eiki.globeswap.BuildConfig
import tech.eiki.globeswap.data.ServiceLocator
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import tech.eiki.globeswap.ui.chat.ChatScreen
import tech.eiki.globeswap.ui.conversations.ConversationsScreen
import tech.eiki.globeswap.ui.conversations.ConversationsViewModel
import tech.eiki.globeswap.ui.onboarding.OnboardingScreen
import tech.eiki.globeswap.ui.swap.SwapScreen

private const val ROUTE_SWAP = "swap"
private const val ROUTE_CONVERSATIONS = "conversations"
private const val ROUTE_CHAT = "chat/{conversationId}"
private const val PREFS = "globeswap"
private const val KEY_ONBOARDED = "onboarded"

@Composable
fun GlobeSwapApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var onboarded by remember { mutableStateOf(prefs.getBoolean(KEY_ONBOARDED, false)) }

    // Beim Start und bei jeder Rückkehr in die App: Mindestversion und neueste Version beim Server abfragen.
    val updates = ServiceLocator.updates
    val update by updates.state.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) { updates.refresh() }
    }
    if (update.required) {
        ForcedUpdateScreen(
            message = update.message,
            installedVersion = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            onUpdate = { updates.openUpdate(context) },
            onCheckAgain = { scope.launch { updates.refresh() } },
        )
        return
    }

    if (!onboarded) {
        OnboardingScreen(onAccept = {
            prefs.edit().putBoolean(KEY_ONBOARDED, true).apply()
            onboarded = true
        })
        return
    }

    val nav = rememberNavController()
    val conversationsVm: ConversationsViewModel = viewModel()
    val conversations by conversationsVm.conversations.collectAsStateWithLifecycle()
    val unread = conversations.count { it.unread }
    val route = nav.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (route == ROUTE_SWAP || route == ROUTE_CONVERSATIONS) {
              Column {
                update.available?.let { release ->
                    UpdateBanner(
                        release = release,
                        channel = updates.channel,
                        onUpdate = { updates.openUpdate(context) },
                        onLater = { updates.dismiss(release.versionCode) },
                    )
                }
                NavigationBar {
                    NavigationBarItem(
                        selected = route == ROUTE_SWAP,
                        onClick = { nav.switchTab(ROUTE_SWAP) },
                        icon = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                        label = { Text("Swap") },
                    )
                    NavigationBarItem(
                        selected = route == ROUTE_CONVERSATIONS,
                        onClick = { nav.switchTab(ROUTE_CONVERSATIONS) },
                        icon = {
                            BadgedBox(badge = { if (unread > 0) Badge { Text("$unread") } }) {
                                Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                            }
                        },
                        label = { Text("Chats") },
                    )
                }
              }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = ROUTE_SWAP, modifier = Modifier.padding(padding)) {
            composable(ROUTE_SWAP) {
                SwapScreen(onMatched = { id -> nav.navigate("chat/$id") })
            }
            composable(ROUTE_CONVERSATIONS) {
                val publicId by conversationsVm.publicId.collectAsStateWithLifecycle()
                ConversationsScreen(
                    publicId = publicId,
                    onAboutOpened = conversationsVm::loadPublicId,
                    conversations = conversations,
                    photoUrl = conversationsVm::photoUrl,
                    onOpen = { id -> nav.navigate("chat/$id") },
                    onDeleteAccount = {
                        conversationsVm.deleteAccount { error ->
                            if (error != null) {
                                Toast.makeText(context, error, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(context, "Your data has been deleted.", Toast.LENGTH_LONG).show()
                                prefs.edit().putBoolean(KEY_ONBOARDED, false).apply()
                                onboarded = false
                            }
                        }
                    },
                )
            }
            composable(
                ROUTE_CHAT,
                arguments = listOf(navArgument("conversationId") { type = NavType.StringType }),
            ) {
                ChatScreen(onBack = { nav.popBackStack() })
            }
        }
    }
}

private fun NavHostController.switchTab(route: String) = navigate(route) {
    popUpTo(ROUTE_SWAP) { saveState = true }
    launchSingleTop = true
    restoreState = true
}
