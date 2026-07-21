package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF1E293B),
    primaryContainer = WarmSand,
    onPrimaryContainer = Color(0xFF78350F),
    secondary = HoneyAmber,
    onSecondary = Color.White,
    tertiary = ForestGreen,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = Color(0xFF0F172A),
    surface = CardSurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = GrayLight,
    onSurfaceVariant = Color(0xFF475569)
)

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF78350F),
    onPrimaryContainer = WarmSand,
    secondary = HoneyAmber,
    onSecondary = Color.White,
    tertiary = ForestGreen,
    onTertiary = Color(0xFF0F172A),
    background = DarkBackground,
    onBackground = Color(0xFFF8FAFC),
    surface = CardSurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFF334155),
    onSurfaceVariant = Color(0xFFCBD5E1)
)

fun parseHexColor(hex: String, fallback: Color): Color {
    return try {
        val cleanHex = hex.removePrefix("#").trim()
        if (cleanHex.length == 6) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else if (cleanHex.length == 8) {
            Color(android.graphics.Color.parseColor("#$cleanHex"))
        } else {
            fallback
        }
    } catch (e: Exception) {
        fallback
    }
}

fun isColorDark(color: Color): Boolean {
    val luminance = 0.2126 * color.red + 0.7152 * color.green + 0.0722 * color.blue
    return luminance < 0.5
}

@Composable
fun LocalBeeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themeName: String = "BEE_GOLD",
    customBgColorHex: String = "#FAF9F6",
    customPrimaryColorHex: String = "#EAB308",
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeName) {
        "DEEP_BLACK" -> darkColorScheme(
            primary = parseHexColor(customPrimaryColorHex, GoldPrimary),
            onPrimary = Color.White,
            primaryContainer = Color(0xFF1E293B),
            onPrimaryContainer = Color.White,
            secondary = HoneyAmber,
            onSecondary = Color.White,
            tertiary = ForestGreen,
            onTertiary = Color.White,
            background = Color(0xFF000000),
            onBackground = Color(0xFFF8FAFC),
            surface = Color(0xFF121212),
            onSurface = Color(0xFFF8FAFC),
            surfaceVariant = Color(0xFF1E293B),
            onSurfaceVariant = Color(0xFFCBD5E1)
        )
        "MONOCHROME" -> lightColorScheme(
            primary = Color(0xFF000000),
            onPrimary = Color(0xFFFFFFFF),
            primaryContainer = Color(0xFFE2E8F0),
            onPrimaryContainer = Color(0xFF000000),
            secondary = Color(0xFF475569),
            onSecondary = Color(0xFFFFFFFF),
            tertiary = Color(0xFF000000),
            onTertiary = Color(0xFFFFFFFF),
            background = Color(0xFFFFFFFF),
            onBackground = Color(0xFF000000),
            surface = Color(0xFFF8FAFC),
            onSurface = Color(0xFF000000),
            surfaceVariant = Color(0xFFF1F5F9),
            onSurfaceVariant = Color(0xFF334155)
        )
        "WARM_CREAM" -> lightColorScheme(
            primary = HoneyAmber,
            onPrimary = Color.White,
            primaryContainer = WarmCream,
            onPrimaryContainer = Color(0xFF78350F),
            secondary = GoldPrimary,
            onSecondary = Color(0xFF1E293B),
            tertiary = ForestGreen,
            onTertiary = Color.White,
            background = WarmCream,
            onBackground = Color(0xFF1E293B),
            surface = Color(0xFFFFFFFF),
            onSurface = Color(0xFF1E293B),
            surfaceVariant = Color(0xFFFEFBF3),
            onSurfaceVariant = Color(0xFF78350F)
        )
        "CUSTOM" -> {
            val bg = parseHexColor(customBgColorHex, LightBackground)
            val isDark = isColorDark(bg)
            val primaryColor = parseHexColor(customPrimaryColorHex, GoldPrimary)
            
            if (isDark) {
                darkColorScheme(
                    primary = primaryColor,
                    onPrimary = Color(0xFF0F172A),
                    primaryContainer = Color(0xFF78350F),
                    onPrimaryContainer = WarmSand,
                    secondary = HoneyAmber,
                    onSecondary = Color.White,
                    tertiary = ForestGreen,
                    onTertiary = Color(0xFF0F172A),
                    background = bg,
                    onBackground = Color(0xFFF8FAFC),
                    surface = Color(0xFF1E293B),
                    onSurface = Color(0xFFF8FAFC),
                    surfaceVariant = Color(0xFF334155),
                    onSurfaceVariant = Color(0xFFCBD5E1)
                )
            } else {
                lightColorScheme(
                    primary = primaryColor,
                    onPrimary = Color(0xFF1E293B),
                    primaryContainer = WarmSand,
                    onPrimaryContainer = Color(0xFF78350F),
                    secondary = HoneyAmber,
                    onSecondary = Color.White,
                    tertiary = ForestGreen,
                    onTertiary = Color.White,
                    background = bg,
                    onBackground = Color(0xFF0F172A),
                    surface = Color(0xFFFFFFFF),
                    onSurface = Color(0xFF0F172A),
                    surfaceVariant = GrayLight,
                    onSurfaceVariant = Color(0xFF475569)
                )
            }
        }
        else -> if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
