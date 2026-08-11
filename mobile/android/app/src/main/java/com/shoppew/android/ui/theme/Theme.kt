package com.shoppew.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

val Brand950 = Color(0xFF102F2E)
val Brand800 = Color(0xFF174B49)
val Brand600 = Color(0xFF087E76)
val Brand500 = Color(0xFF0A9489)
val Brand100 = Color(0xFFDDF3EF)
val Spark400 = Color(0xFFD8ED4B)
val Violet500 = Color(0xFF6258D6)
val Coral500 = Color(0xFFD9604C)
val Canvas = Color(0xFFF5F7F4)
val Surface = Color(0xFFFFFFFF)
val Ink = Color(0xFF18211F)
val Muted = Color(0xFF65716D)
val Line = Color(0xFFDCE3DF)
val Success = Color(0xFF18794E)
val Warning = Color(0xFFA15C00)
val Danger = Color(0xFFB42318)
val Info = Color(0xFF2457C5)

private val LightColors = lightColorScheme(
    primary = Brand600,
    onPrimary = Color.White,
    primaryContainer = Brand100,
    onPrimaryContainer = Brand950,
    secondary = Violet500,
    tertiary = Coral500,
    background = Canvas,
    onBackground = Ink,
    surface = Surface,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEDF1EE),
    onSurfaceVariant = Muted,
    outline = Line,
    error = Danger,
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF61D8CE),
    onPrimary = Brand950,
    primaryContainer = Brand800,
    onPrimaryContainer = Color(0xFFDDF3EF),
    secondary = Color(0xFFC8C3FF),
    tertiary = Color(0xFFFFB4A7),
    background = Color(0xFF101514),
    onBackground = Color(0xFFE2E8E4),
    surface = Color(0xFF171D1B),
    onSurface = Color(0xFFE2E8E4),
    surfaceVariant = Color(0xFF26302D),
    onSurfaceVariant = Color(0xFFBAC5C0),
    outline = Color(0xFF414B48),
    error = Color(0xFFFFB4AB),
)

@Composable
fun ShoppewTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isStatusBarContrastEnforced = false
                window.isNavigationBarContrastEnforced = false
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = ShoppewTypography, content = content)
}
