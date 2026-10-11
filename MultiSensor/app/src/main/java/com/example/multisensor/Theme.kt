package com.example.multisensor

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val CharcoalBackground = Color(0xFF0F1113)
val DarkSlateSurface = Color(0xFF1A1D21)
val DarkSubtleBorder = Color(0xFF2E3238)
val NeonCyan = Color(0xFF00F5FF)
val PureWhite = Color.White

val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightBorder = Color(0xFFE5E7EB)
val LightAccent = Color(0xFF0891B2)
val LightTextPrimary = Color(0xFF111827)

private val CyberpunkDarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = CharcoalBackground,
    background = CharcoalBackground,
    onBackground = PureWhite,
    surface = DarkSlateSurface,
    onSurface = PureWhite,
    outline = DarkSubtleBorder
)

private val IndustrialLightColorScheme = lightColorScheme(
    primary = LightAccent,
    onPrimary = PureWhite,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    outline = LightBorder
)

val CyberpunkShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp)
)

val CyberpunkTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 1.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        letterSpacing = 1.5.sp
    )
)

@Composable
fun MultiSensorTheme(
    darkTheme: Boolean,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) CyberpunkDarkColorScheme else IndustrialLightColorScheme,
        typography = CyberpunkTypography,
        shapes = CyberpunkShapes,
        content = content
    )
}
