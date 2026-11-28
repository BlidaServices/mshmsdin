package com.mshmsdin.captionsearch.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import com.mshmsdin.captionsearch.R

val THEME_NAMES = listOf("royal", "green", "red", "yellow", "blue", "carbon")

fun themeScheme(name: String, dark: Boolean): ColorScheme {
    if (name == "carbon") {
        return darkColorScheme(
            primary = Color(0xFFB0FF57),
            onPrimary = Color(0xFF102000),
            primaryContainer = Color(0xFF244000),
            onPrimaryContainer = Color(0xFFC6FF91),
            secondary = Color(0xFF9E9E9E),
            onSecondary = Color(0xFF202020),
            surface = Color(0xFF0B0B0B),
            onSurface = Color(0xFFE8E8E8),
            surfaceVariant = Color(0xFF1D1D1D),
            onSurfaceVariant = Color(0xFFB5B5B5),
            background = Color(0xFF0B0B0B),
            onBackground = Color(0xFFE8E8E8),
            error = Color(0xFFFF6E6E)
        )
    }
    val seed = when (name) {
        "green" -> Color(0xFF1B7F4B)
        "red" -> Color(0xFFB3261E)
        "yellow" -> Color(0xFFF2B01E)
        "blue" -> Color(0xFF0B3D91)
        else -> Color(0xFF6A3FBF)
    }
    val container = when (name) {
        "green" -> Color(0xFFC8F2D8)
        "red" -> Color(0xFFFFD6D4)
        "yellow" -> Color(0xFFFFE9A8)
        "blue" -> Color(0xFFD9E4FF)
        else -> Color(0xFFE8DDFF)
    }
    return if (dark) darkColorScheme(
        primary = seed,
        onPrimary = Color.White,
        primaryContainer = seed.copy(alpha = 0.35f),
        onPrimaryContainer = Color.White,
        secondary = seed.copy(alpha = 0.75f),
        onSecondary = Color.White,
        surface = Color(0xFF121318),
        onSurface = Color(0xFFE2E1E9),
        surfaceVariant = Color(0xFF2A2B32),
        onSurfaceVariant = Color(0xFFC4C6D0),
        background = Color(0xFF121318),
        onBackground = Color(0xFFE2E1E9),
        error = Color(0xFFFFB4AB)
    ) else lightColorScheme(
        primary = seed,
        onPrimary = Color.White,
        primaryContainer = container,
        onPrimaryContainer = Color(0xFF1A1024),
        secondary = seed,
        onSecondary = Color.White,
        secondaryContainer = container,
        onSecondaryContainer = Color(0xFF1A1024),
        surface = Color(0xFFFAFAFF),
        onSurface = Color(0xFF1A1B20),
        surfaceVariant = Color(0xFFE6E7EF),
        onSurfaceVariant = Color(0xFF44474E),
        background = Color(0xFFFAFAFF),
        onBackground = Color(0xFF1A1B20),
        error = Color(0xFFBA1A1A)
    )
}

val FONT_NAMES = listOf("افتراضي", "أميري", "القاهرة", "نصخ")

private fun typographyFor(name: String): androidx.compose.material3.Typography {
    val family = fontFamily(name)
    val base = androidx.compose.material3.Typography()
    fun TextStyle.withFont() = copy(fontFamily = family)
    return base.copy(
        displayLarge = base.displayLarge.withFont(),
        displayMedium = base.displayMedium.withFont(),
        displaySmall = base.displaySmall.withFont(),
        headlineLarge = base.headlineLarge.withFont(),
        headlineMedium = base.headlineMedium.withFont(),
        headlineSmall = base.headlineSmall.withFont(),
        titleLarge = base.titleLarge.withFont(),
        titleMedium = base.titleMedium.withFont(),
        titleSmall = base.titleSmall.withFont(),
        bodyLarge = base.bodyLarge.withFont(),
        bodyMedium = base.bodyMedium.withFont(),
        bodySmall = base.bodySmall.withFont(),
        labelLarge = base.labelLarge.withFont(),
        labelMedium = base.labelMedium.withFont(),
        labelSmall = base.labelSmall.withFont()
    )
}

private fun fontFamily(name: String) = when (name) {
    "أميري" -> FontFamily(Font(R.font.amiri_regular))
    "القاهرة" -> FontFamily(Font(R.font.cairo_regular))
    "نصخ" -> FontFamily(Font(R.font.noto_naskh_arabic))
    else -> FontFamily.Default
}

@Composable
fun CaptionSearchTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeName: String = "royal",
    fontName: String = "افتراضي",
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = themeScheme(themeName, darkTheme),
        typography = typographyFor(fontName),
        content = content
    )
}
