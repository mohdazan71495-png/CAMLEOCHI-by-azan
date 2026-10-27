package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkObservatoryScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = ObsidianNight,
    primaryContainer = Color(0xFF0C4A6E),
    onPrimaryContainer = Color(0xFFE0F2FE),
    secondary = SolarAmber,
    onSecondary = ObsidianNight,
    secondaryContainer = Color(0xFF78350F),
    onSecondaryContainer = Color(0xFFFEF3C7),
    tertiary = TelemetryEmerald,
    onTertiary = ObsidianNight,
    tertiaryContainer = Color(0xFF064E3B),
    onTertiaryContainer = Color(0xFFD1FAE5),
    background = ObsidianNight,
    onBackground = TextPrimaryDark,
    surface = DeepSlateNavy,
    onSurface = TextPrimaryDark,
    surfaceVariant = ElevatedSlate,
    onSurfaceVariant = TextSecondaryDark,
    outline = SurfaceHighlight,
    error = TelemetryHoldCoral,
    onError = Color.White
)

private val LightGlacierScheme = lightColorScheme(
    primary = DeepCyan,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF075985),
    secondary = DeepSolarAmber,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFEF3C7),
    onSecondaryContainer = Color(0xFF92400E),
    tertiary = DeepEmerald,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1FAE5),
    onTertiaryContainer = Color(0xFF065F46),
    background = GlacierAlabaster,
    onBackground = TextPrimaryLight,
    surface = CrispSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = SoftSlateSurface,
    onSurfaceVariant = TextSecondaryLight,
    outline = LightBorderSlate,
    error = DeepHoldCoral,
    onError = Color.White
)

val AeroPulseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AeroPulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkObservatoryScheme else LightGlacierScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = AeroPulseShapes,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    AeroPulseTheme(darkTheme = darkTheme, content = content)
}
