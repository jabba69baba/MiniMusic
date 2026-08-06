package com.example.minimusic.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.ui.graphics.Shape
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * A soft, asymmetric "blob" outline — Material 3 Expressive's signature organic
 * shape, used for album art so it doesn't read as a plain rectangle. Built as a
 * closed cubic-bezier loop around a slightly perturbed circle so it stays a
 * believable, gently lopsided blob rather than a starburst.
 */
fun expressiveBlobShape(): Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val points = 8
    // Fixed per-vertex radius perturbation so the blob has a consistent,
    // recognizable silhouette rather than a perfect circle.
    val radii = listOf(1f, 0.90f, 1.05f, 0.85f, 1.0f, 0.92f, 1.08f, 0.88f)

    val vertices = (0 until points).map { i ->
        val angle = (2 * PI * i / points) - (PI / 2)
        val r = radii[i % radii.size]
        val rx = (w / 2f) * r
        val ry = (h / 2f) * r
        Pair(cx + rx * cos(angle).toFloat(), cy + ry * sin(angle).toFloat())
    }

    moveTo(vertices[0].first, vertices[0].second)
    for (i in vertices.indices) {
        val current = vertices[i]
        val next = vertices[(i + 1) % vertices.size]
        val midX = (current.first + next.first) / 2f
        val midY = (current.second + next.second) / 2f
        quadraticTo(current.first, current.second, midX, midY)
    }
    close()
}

/**
 * A scalloped "cookie" shape — a circle with a fixed number of shallow rounded
 * petals around its edge. Used on the primary play button and active toggle
 * chips as Material 3 Expressive's shape-morph accent.
 *
 * @param petals number of scallops around the circumference
 * @param amplitude how far each petal bulges outward, as a fraction of the radius
 */
fun cookieShape(petals: Int, amplitude: Float): Shape = GenericShape { size, _ ->
    val w = size.width
    val h = size.height
    val cx = w / 2f
    val cy = h / 2f
    val baseRadius = minOf(w, h) / 2f
    val steps = petals * 8

    for (i in 0..steps) {
        val angle = (2 * PI * i / steps)
        val petalPhase = angle * petals
        val r = baseRadius * (1f + amplitude * cos(petalPhase).toFloat())
        val x = cx + r * cos(angle).toFloat()
        val y = cy + r * sin(angle).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}
