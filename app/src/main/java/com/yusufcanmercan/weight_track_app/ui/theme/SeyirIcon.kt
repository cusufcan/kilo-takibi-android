package com.yusufcanmercan.weight_track_app.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun SeyirIcon(
    @DrawableRes glyph: Int,
    modifier: Modifier = Modifier,
    description: String? = null,
    size: Dp = 20.dp,
    tint: Color = LocalContentColor.current,
) {
    Icon(painterResource(glyph), description, modifier.size(size), tint)
}

@Composable
internal fun SeyirIconTile(
    @DrawableRes glyph: Int,
    description: String? = null,
    tileSize: Dp = 36.dp,
    iconSize: Dp = 18.dp,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier.size(tileSize).background(colors.primaryContainer, MaterialTheme.shapes.small),
        contentAlignment = Alignment.Center,
    ) {
        SeyirIcon(glyph, description = description, size = iconSize, tint = colors.onPrimaryContainer)
    }
}
