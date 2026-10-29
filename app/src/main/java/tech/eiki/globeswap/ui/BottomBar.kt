package tech.eiki.globeswap.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import tech.eiki.globeswap.R
import tech.eiki.globeswap.ui.theme.GlobeGradient

enum class Tab { CHATS, SWAP, WORLD }

/**
 * Schwebende Leiste unten (Design A): links Chats, in der Mitte der große Swap-Knopf, rechts die Weltkarte.
 * Die Leiste ist in beiden Modi dunkel, damit sie sich vom Inhalt abhebt.
 */
@Composable
fun FloatingNavBar(selected: Tab, unread: Int, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.inverseSurface
    Box(modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(top = 18.dp, bottom = 12.dp), contentAlignment = Alignment.BottomCenter) {
        Surface(color = dark, shape = RoundedCornerShape(34.dp), shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth().height(68.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround, verticalAlignment = Alignment.CenterVertically) {
                NavItem(Icons.Default.ChatBubble, stringResource(R.string.chats), selected == Tab.CHATS, unread) { onSelect(Tab.CHATS) }
                Box(Modifier.width(72.dp))
                NavItem(Icons.Default.Public, stringResource(R.string.world_tab), selected == Tab.WORLD) { onSelect(Tab.WORLD) }
            }
        }
        // Großer Swap-Knopf, ragt über die Leiste hinaus
        val swapLabel = stringResource(R.string.swap_tab)
        val swapSelected = selected == Tab.SWAP
        Column(
            Modifier.offset(y = (-14).dp)
                .semantics { contentDescription = swapLabel; this.selected = swapSelected }
                .clip(RoundedCornerShape(20.dp))
                .clickable(role = Role.Tab) { onSelect(Tab.SWAP) },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier.size(64.dp).shadow(10.dp, CircleShape).background(GlobeGradient, CircleShape)
                    .border(4.dp, if (selected == Tab.SWAP) Color.White else dark, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp)) }
            Text(swapLabel, color = if (selected == Tab.SWAP) Color.White else Color.White.copy(alpha = .6f), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NavItem(icon: ImageVector, label: String, selected: Boolean, badge: Int = 0, onClick: () -> Unit) {
    val color = if (selected) Color.White else Color.White.copy(alpha = .55f)
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).clickable(role = Role.Tab, onClick = onClick).semantics { this.selected = selected }
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BadgedBox(badge = { if (badge > 0) Badge(containerColor = MaterialTheme.colorScheme.tertiary, contentColor = MaterialTheme.colorScheme.onTertiary) { Text("$badge") } }) {
            Icon(icon, contentDescription = null, tint = color)
        }
        Text(label, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}
