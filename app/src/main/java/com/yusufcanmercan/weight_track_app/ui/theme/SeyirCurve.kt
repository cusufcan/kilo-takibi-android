package com.yusufcanmercan.weight_track_app.ui.theme

import androidx.compose.ui.geometry.Offset

/** Panodaki eğri. Kontrol noktaları iki ölçüm arasındaki aralığın içinde kalır. */
internal fun seyirCurve(
    points: List<Offset>,
    moveTo: (Float, Float) -> Unit,
    cubicTo: (Float, Float, Float, Float, Float, Float) -> Unit,
) {
    val first = points.firstOrNull() ?: return
    moveTo(first.x, first.y)
    for (index in 1..points.lastIndex) {
        val from = points[index - 1]
        val to = points[index]
        if (to.x == from.x) continue
        val before = points[(index - 2).coerceAtLeast(0)]
        val after = points[(index + 1).coerceAtMost(points.lastIndex)]
        val dx = (to.x - from.x) / 3f
        val min = minOf(from.y, to.y)
        val max = maxOf(from.y, to.y)
        val firstY = (from.y + (to.y - before.y) * dx / (to.x - before.x)).coerceIn(min, max)
        val secondY = (to.y - (after.y - from.y) * dx / (after.x - from.x)).coerceIn(min, max)
        cubicTo(from.x + dx, firstY, to.x - dx, secondY, to.x, to.y)
    }
}
