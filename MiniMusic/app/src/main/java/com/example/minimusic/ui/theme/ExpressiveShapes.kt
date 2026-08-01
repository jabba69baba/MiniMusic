package com.example.minimusic.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/** Fully rounded "stadium" shape — used for pill controls and the mini player. */
val PillShape = RoundedCornerShape(50)

/**
 * An asymmetric rounded-rect with a different radius on each corner. Material 3
 * Expressive leans on shapes like this (rather than uniform rounding) to give
 * containers — album art, cards — a more distinctive, less "boxy" silhouette.
 */
fun expressiveBlobShape(): Shape = RoundedCornerShape(
    topStart = 48.dp,
    topEnd = 20.dp,
    bottomEnd = 48.dp,
    bottomStart = 20.dp
)

/**
 * A scalloped "cookie" shape with [petals] bumps — the shape family Google uses for
 * morphing FABs and media buttons in Material 3 Expressive. Built by walking points
 * around a circle, alternating between an outer and inner radius, and smoothing the
 * silhouette with quadratic curves through each midpoint.
 */
fun cookieShape(petals: Int = 12, amplitude: Float = 0.12f): Shape = GenericShape { size, _ ->
    val cx = size.width / 2f
    val cy = size.height / 2f
    val baseRadius = minOf(size.width, size.height) / 2f
    val bump = baseRadius * amplitude
    val totalPoints = petals * 2
    val angleStep = (2.0 * Math.PI / totalPoints).toFloat()

    val points = (0 until totalPoints).map { i ->
        val angle = i * angleStep - (Math.PI / 2).toFloat()
        val radius = if (i % 2 == 0) baseRadius + bump else baseRadius - bump
        androidx.compose.ui.geometry.Offset(
            x = cx + radius * cos(angle),
            y = cy + radius * sin(angle)
        )
    }

    val start = androidx.compose.ui.geometry.Offset(
        (points.last().x + points.first().x) / 2f,
        (points.last().y + points.first().y) / 2f
    )
    moveTo(start.x, start.y)
    for (i in points.indices) {
        val current = points[i]
        val next = points[(i + 1) % points.size]
        val midX = (current.x + next.x) / 2f
        val midY = (current.y + next.y) / 2f
        quadraticTo(current.x, current.y, midX, midY)
    }
    close()
}
