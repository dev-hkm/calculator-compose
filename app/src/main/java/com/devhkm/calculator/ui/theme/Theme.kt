package com.devhkm.calculator.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// ---------- Palette ----------
val AccentBlue = Color(0xFF4CC2FF)
val AccentViolet = Color(0xFF9B7BFF)
val AccentMint = Color(0xFF5EE7B7)

private val DarkScheme = darkColorScheme(
    primary = AccentBlue,
    onPrimary = Color(0xFF04121B),
    primaryContainer = Color(0xFF123244),
    onPrimaryContainer = AccentBlue,
    secondary = AccentViolet,
    tertiary = AccentMint,
    background = Color(0xFF0F1115),
    onBackground = Color(0xFFEDF1F7),
    surface = Color(0xFF161A21),
    onSurface = Color(0xFFEDF1F7),
    surfaceVariant = Color(0xFF1E242D),
    onSurfaceVariant = Color(0xFFA8B3C4),
    outline = Color(0xFF2C3542),
    error = Color(0xFFFF6B81),
)

private val LightScheme = lightColorScheme(
    primary = Color(0xFF0B6FA4),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2EDFF),
    onPrimaryContainer = Color(0xFF06283A),
    secondary = Color(0xFF6950C4),
    tertiary = Color(0xFF12866A),
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF141A21),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF141A21),
    surfaceVariant = Color(0xFFEDF1F7),
    onSurfaceVariant = Color(0xFF556072),
    outline = Color(0xFFD5DDE8),
    error = Color(0xFFC5263B),
)

private val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Light,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-1).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        letterSpacing = 0.2.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        letterSpacing = 0.4.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp
    )
)

@Composable
fun CalculatorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = scheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = scheme,
        typography = AppTypography,
        content = content
    )
}
