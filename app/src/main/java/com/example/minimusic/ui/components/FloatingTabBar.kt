package com.example.minimusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
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
 * A row of plain icon buttons (no enclosing pill container) — the active
 * destination gets a tinted pill highlight behind just its own icon; inactive
 * ones are transparent. Used for the Songs/Albums/Artists switcher.
 */
@Composable
fun <T> FloatingTabBar(
    items: List<TabBarItem<T>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { item ->
            val isSelected = item.value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(PillShape)
                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    .clickable { onSelect(item.value) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
