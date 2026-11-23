package io.github.uveshm003.anchor.core.ui

import android.content.pm.PackageManager
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import io.github.uveshm003.anchor.core.designsystem.component.AnchorIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** An app's launcher icon, loaded off the main thread and cached; a neutral glyph while loading or if missing. */
@Composable
fun AppIcon(packageName: String, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val pm = LocalContext.current.packageManager
    val icon by produceState(initialValue = AppIconCache.get(packageName), packageName) {
        if (value == null) value = withContext(Dispatchers.IO) { AppIconCache.load(pm, packageName) }
    }
    val bitmap = icon
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = null, modifier = modifier.size(size))
    } else {
        Icon(
            painter = painterResource(AnchorIcons.Block),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier.size(size),
        )
    }
}

private object AppIconCache {
    private const val MAX_ICONS = 400
    private const val ICON_PX = 96
    private val cache = LruCache<String, ImageBitmap>(MAX_ICONS)

    fun get(packageName: String): ImageBitmap? = cache.get(packageName)

    fun load(pm: PackageManager, packageName: String): ImageBitmap? = try {
        pm.getApplicationIcon(packageName).toBitmap(ICON_PX, ICON_PX).asImageBitmap().also {
            cache.put(packageName, it)
        }
    } catch (_: PackageManager.NameNotFoundException) {
        null
    }
}
