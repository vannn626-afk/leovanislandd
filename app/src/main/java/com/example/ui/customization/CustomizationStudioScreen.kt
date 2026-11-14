package com.example.ui.customization

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.AnimationPreset
import com.example.domain.model.CutoutAdaptationMode
import com.example.domain.model.IslandThemeStyle
import com.example.island.components.DynamicIslandCapsule
import com.example.island.state.IslandRuntimeState
import com.example.ui.dashboard.IslandViewModel
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.SapphireAccent
import com.example.ui.theme.TitaniumMuted
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun CustomizationStudioScreen(
    runtimeState: IslandRuntimeState,
    viewModel: IslandViewModel,
    modifier: Modifier = Modifier
) {
    val config = runtimeState.configuration

    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag("customization_studio_screen")
    ) {
        // Sticky Live Preview Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = CarbonSurface,
            border = BorderStroke(1.dp, GlassBorderSubtle)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LIVE STUDIO PREVIEW",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = SapphireAccent
                    )
                    Text(
                        text = config.themeStyle.title,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = TitaniumMuted
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                DynamicIslandCapsule(runtimeState = runtimeState)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Theme Appearance Selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
                    border = BorderStroke(1.dp, GlassBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Appearance Themes",
                                tint = SapphireAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Appearance & Material Themes",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        IslandThemeStyle.entries.forEach { style ->
                            val selected = config.themeStyle == style
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("theme_option_${style.name.lowercase()}")
                                    .clickable { viewModel.setThemeStyle(style) },
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) SapphireAccent.copy(alpha = 0.16f) else CarbonSurface,
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) SapphireAccent else GlassBorderSubtle
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = style.title,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = style.subtitle,
                                            color = TitaniumMuted,
                                            fontSize = 12.sp
                                        )
                                    }
                                    if (selected) {
                                        Text(
                                            text = "ACTIVE",
                                            fontFamily = JetBrainsMonoFontFamily,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SapphireAccent
                                        )
                                    }
                                }
                            }
                        }

                        // Custom Accent Swatches
                        Text(
                            text = "Custom Accent Highlight",
                            color = TitaniumMuted,
                            fontSize = 12.sp
                        )
                        val swatches = listOf(
                            0xFF38BDF8L,
                            0xFF818CF8L,
                            0xFF34D399L,
                            0xFFF59E0BL,
                            0xFFF43F5EL,
                            0xFFE2E8F0L
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            swatches.forEach { hex ->
                                val isSelectedSwatch = config.customAccentHex == hex
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .border(
                                            width = if (isSelectedSwatch) 3.dp else 1.dp,
                                            color = if (isSelectedSwatch) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.updateConfig {
                                                it.copy(
                                                    customAccentHex = hex,
                                                    themeStyle = IslandThemeStyle.CUSTOM
                                                )
                                            }
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // 2. Dimensions, Position & Cutout Awareness
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
                    border = BorderStroke(1.dp, GlassBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AspectRatio,
                                contentDescription = "Dimensions & Position",
                                tint = SapphireAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Geometry, Cutout & Positioning",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "Display Cutout & Notch Adaptation",
                            color = TitaniumMuted,
                            fontSize = 12.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CutoutAdaptationMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = config.cutoutMode == mode,
                                    onClick = { viewModel.setCutoutMode(mode) },
                                    label = { Text(mode.label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SapphireAccent.copy(alpha = 0.22f),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        StudioSliderRow(
                            label = "Compact Island Width",
                            valueText = "${config.compactWidthDp.roundToInt()} dp",
                            value = config.compactWidthDp,
                            valueRange = 100f..220f,
                            testTag = "slider_compact_width",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(compactWidthDp = v) } }
                        )

                        StudioSliderRow(
                            label = "Compact Island Height",
                            valueText = "${config.compactHeightDp.roundToInt()} dp",
                            value = config.compactHeightDp,
                            valueRange = 28f..56f,
                            testTag = "slider_compact_height",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(compactHeightDp = v) } }
                        )

                        StudioSliderRow(
                            label = "Expanded Island Width",
                            valueText = "${config.expandedWidthDp.roundToInt()} dp",
                            value = config.expandedWidthDp,
                            valueRange = 280f..390f,
                            testTag = "slider_expanded_width",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(expandedWidthDp = v) } }
                        )

                        StudioSliderRow(
                            label = "Expanded Island Height",
                            valueText = "${config.expandedHeightDp.roundToInt()} dp",
                            value = config.expandedHeightDp,
                            valueRange = 140f..240f,
                            testTag = "slider_expanded_height",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(expandedHeightDp = v) } }
                        )

                        StudioSliderRow(
                            label = "Horizontal Position (X Offset)",
                            valueText = "${config.horizontalOffsetDp.roundToInt()} dp",
                            value = config.horizontalOffsetDp,
                            valueRange = -160f..160f,
                            testTag = "slider_horizontal_offset",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(horizontalOffsetDp = v) } }
                        )

                        StudioSliderRow(
                            label = "Vertical Position (Y Offset)",
                            valueText = "${config.verticalOffsetDp.roundToInt()} dp",
                            value = config.verticalOffsetDp,
                            valueRange = 0f..180f,
                            testTag = "slider_vertical_offset",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(verticalOffsetDp = v) } }
                        )

                        StudioSliderRow(
                            label = "Capsule Corner Radius",
                            valueText = "${config.cornerRadiusDp.roundToInt()} dp",
                            value = config.cornerRadiusDp,
                            valueRange = 12f..42f,
                            testTag = "slider_corner_radius",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(cornerRadiusDp = v) } }
                        )

                        StudioSliderRow(
                            label = "Compact Mode Scale",
                            valueText = String.format(Locale.US, "%.2fx", config.compactSizeScale),
                            value = config.compactSizeScale,
                            valueRange = 0.8f..1.25f,
                            testTag = "slider_compact_scale",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(compactSizeScale = v) } }
                        )

                        StudioSliderRow(
                            label = "Expanded Mode Scale",
                            valueText = String.format(Locale.US, "%.2fx", config.expandedSizeScale),
                            value = config.expandedSizeScale,
                            valueRange = 0.85f..1.15f,
                            testTag = "slider_expanded_scale",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(expandedSizeScale = v) } }
                        )
                    }
                }
            }

            // 3. Glass Optics & Spring Animation Physics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
                    border = BorderStroke(1.dp, GlassBorderSubtle)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Animation,
                                contentDescription = "Optics & Motion",
                                tint = SapphireAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Glass Optics & Spring Motion",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }

                        StudioSliderRow(
                            label = "Surface Opacity / Transparency",
                            valueText = "${(config.transparencyAlpha * 100).roundToInt()}%",
                            value = config.transparencyAlpha,
                            valueRange = 0.65f..1.0f,
                            testTag = "slider_transparency",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(transparencyAlpha = v) } }
                        )

                        StudioSliderRow(
                            label = "Liquid Glass Blur Intensity",
                            valueText = "${(config.blurIntensity * 100).roundToInt()}%",
                            value = config.blurIntensity,
                            valueRange = 0.0f..1.0f,
                            testTag = "slider_blur_intensity",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(blurIntensity = v) } }
                        )

                        StudioSliderRow(
                            label = "Specular Border Intensity",
                            valueText = "${(config.borderIntensity * 100).roundToInt()}%",
                            value = config.borderIntensity,
                            valueRange = 0.1f..1.0f,
                            testTag = "slider_border_intensity",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(borderIntensity = v) } }
                        )

                        Text(
                            text = "Spring Physics Preset",
                            color = TitaniumMuted,
                            fontSize = 12.sp
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AnimationPreset.entries.forEach { preset ->
                                FilterChip(
                                    selected = config.animationPreset == preset,
                                    onClick = { viewModel.setAnimationPreset(preset) },
                                    label = { Text(preset.label, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = SapphireAccent.copy(alpha = 0.22f),
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        StudioSliderRow(
                            label = "Animation Speed Multiplier",
                            valueText = String.format(Locale.US, "%.1fx", config.animationSpeedMultiplier),
                            value = config.animationSpeedMultiplier,
                            valueRange = 0.5f..2.0f,
                            testTag = "slider_animation_speed",
                            onValueChange = { v -> viewModel.updateConfig { it.copy(animationSpeedMultiplier = v) } }
                        )

                        StudioSliderRow(
                            label = "Auto-Collapse Timeout",
                            valueText = "${config.autoCollapseSeconds}s",
                            value = config.autoCollapseSeconds.toFloat(),
                            valueRange = 2f..15f,
                            testTag = "slider_auto_collapse",
                            onValueChange = { v ->
                                viewModel.updateConfig { it.copy(autoCollapseSeconds = v.roundToInt()) }
                            }
                        )

                        OutlinedButton(
                            onClick = { viewModel.resetStudioDefaults() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("reset_studio_defaults_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Reset Defaults",
                                tint = SapphireAccent
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Reset Customization Studio to Factory Defaults",
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StudioSliderRow(
    label: String,
    valueText: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    testTag: String,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp
            )
            Text(
                text = valueText,
                fontFamily = JetBrainsMonoFontFamily,
                color = SapphireAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = SapphireAccent,
                inactiveTrackColor = CarbonSurface
            )
        )
    }
}
