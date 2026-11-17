package br.com.nexj.nexsong.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val NexDark = darkColorScheme(
    primary = Color(0xFF7C9CFF),
    onPrimary = Color(0xFF0B1B4D),
    background = Color(0xFF111322),
    surface = Color(0xFF111322),
    surfaceVariant = Color(0xFF272A40),
    onSurface = Color(0xFFE3E4F2),
    onSurfaceVariant = Color(0xFFB4B7CC)
)

private val NexLight = lightColorScheme(
    primary = Color(0xFF3F5BD6)
)

/** Tema Material 3: escuro ou claro conforme o sistema, com cores dinâmicas no Android 12+. */
@Composable
fun NexSongTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> NexDark
        else -> NexLight
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
