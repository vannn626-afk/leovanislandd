package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LeovnLiquidGlassColorScheme = darkColorScheme(
    primary = SapphireAccent,
    onPrimary = ObsidianBlack,
    primaryContainer = LiquidGlassElevated,
    onPrimaryContainer = SpecularSilver,
    secondary = IndigoAccent,
    onSecondary = ObsidianBlack,
    tertiary = EmeraldLive,
    background = ObsidianBlack,
    onBackground = SpecularSilver,
    surface = CarbonSurface,
    onSurface = SpecularSilver,
    surfaceVariant = LiquidGlassSurface,
    onSurfaceVariant = TitaniumMuted,
    outline = GlassBorderSubtle,
    error = CrimsonAlert,
    onError = Color.White
)

@Composable
fun LeovnIslandTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LeovnLiquidGlassColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    LeovnIslandTheme(content = content)
}
