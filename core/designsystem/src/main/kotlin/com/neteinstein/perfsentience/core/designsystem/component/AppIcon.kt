package com.neteinstein.perfsentience.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Launcher icon of an installed app, loaded off the main thread. Falls back to a generic glyph (e.g. once uninstalled). */
@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier, size: Int = 44) {
    val context = LocalContext.current
    val sizePx = with(LocalDensity.current) { size.dp.roundToPx() }
    val bitmap by produceState<ImageBitmap?>(initialValue = null, packageName, sizePx) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap(sizePx, sizePx).asImageBitmap() }.getOrNull()
        }
    }
    val shape = RoundedCornerShape((size / 3.5f).dp)
    val current = bitmap
    if (current != null) {
        Image(current, contentDescription = null, modifier = modifier.size(size.dp).clip(shape))
    } else {
        Box(
            modifier.size(size.dp).background(MaterialTheme.colorScheme.surfaceVariant, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Android, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
