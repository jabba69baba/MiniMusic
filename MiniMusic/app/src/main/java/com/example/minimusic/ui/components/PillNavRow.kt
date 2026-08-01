package com.example.minimusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.example.minimusic.ui.theme.PillShape

/**
 * A pill-shaped segmented control: a rounded tonal track holding one pill per
 * option, with the selected option filled solid and a gentle scale pop on the
 * label when it becomes active. Used for both the Songs/Albums/Artists switch
 * on the library screen and the Player/Lyrics switch on the now-playing screen,
 * so the app has one consistent "nav" shape language throughout — the same
 * role PixelPlayer's pill-shaped bottom navigation plays there.
 */
@Composable
fun <T> PillNavRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    fillWidth: Boolean = false
) {
    Surface(
        shape = PillShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .padding(4.dp)
                .let { if (fillWidth) it.fillMaxWidth() else it }
        ) {
            options.forEach { (value, label) ->
                val isSelected = selected == value
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.94f,
                    label = "pill_nav_scale"
                )
                Surface(
                    shape = PillShape,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                    modifier = Modifier
                        .let { if (fillWidth) it.weight(1f) else it }
                        .clickable { onSelect(value) }
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}
