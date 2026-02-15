package com.menswear.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand colors
val Brand = Color(0xFF1A1A1A)
val BrandLight = Color(0xFF333333)
val Accent = Color(0xFFC4A265) // Gold accent
val Surface = Color(0xFFF8F7F5)
val OnSurface = Color(0xFF1A1A1A)
val Error = Color(0xFFBA1A1A)
val OnSale = Color(0xFFCC0000)

private val LightColorScheme = lightColorScheme(
    primary = Brand,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E0E0),
    onPrimaryContainer = Brand,
    secondary = Accent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF5ECD7),
    onSecondaryContainer = Color(0xFF3D2E00),
    tertiary = Color(0xFF4A6356),
    onTertiary = Color.White,
    background = Color.White,
    onBackground = OnSurface,
    surface = Color.White,
    onSurface = OnSurface,
    surfaceVariant = Surface,
    onSurfaceVariant = Color(0xFF49454F),
    outline = Color(0xFFCAC4D0),
    outlineVariant = Color(0xFFE7E0EC),
    error = Error,
    onError = Color.White,
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFD4C4A0),
    onPrimary = Color(0xFF3A3000),
    primaryContainer = BrandLight,
    onPrimaryContainer = Color(0xFFE0E0E0),
    secondary = Accent,
    onSecondary = Color(0xFF3D2E00),
    background = Color(0xFF1A1A1A),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF2C2C2C),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
)

@Composable
fun MenswearTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}
