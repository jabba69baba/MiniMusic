package com.example.minimusic.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.minimusic.ui.theme.PillShape

/** A single destination in a [FloatingTabBar]. */
data class TabBarItem<T>(
    val value: T,
    val label: String,
    val icon: ImageVector
)

/**
 * Solid, full-width nav bar — same opaque background as the app's dark
 * surface, flush edges, square corners, no side margins, sitting directly
 * beneath [MiniPlayer] with no gap so the two read as one continuous
 * footer. Destinations are evenly spaced across the full width; only the
 * active one gets a pill-shaped highlight behind its icon+label, sized to
 * fit just that icon+label. Inactive destinations are plain icon+label in a
 * muted tone with no background at all.
 */
@Composable
fun <T> FloatingTabBar(
    items: List<TabBarItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { item ->
            val isSelected = item.value == selected
            val backgroundColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                label = "tabBarPillColor"
            )
            val contentColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                label = "tabBarContentColor"
            )
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(backgroundColor)
                    .clickable { onSelect(item.value) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = item.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor
                )
            }
        }
    }
}
