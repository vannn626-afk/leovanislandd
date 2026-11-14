package com.example.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SettingsSuggest
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.DoubleTapAction
import com.example.domain.model.SupportedAppCategory
import com.example.island.state.IslandRuntimeState
import com.example.ui.dashboard.IslandViewModel
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.GlassBorderSubtle
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.LiquidGlassSurface
import com.example.ui.theme.SapphireAccent
import com.example.ui.theme.TitaniumMuted

@Composable
fun SettingsScreen(
    runtimeState: IslandRuntimeState,
    viewModel: IslandViewModel,
    modifier: Modifier = Modifier
) {
    val config = runtimeState.configuration

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Per-App Smart Notification Filters
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
                border = BorderStroke(1.dp, GlassBorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Per-App Filters",
                            tint = SapphireAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Per-App Notification Filters",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Choose which apps are allowed to trigger Dynamic Island notifications",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TitaniumMuted
                            )
                        }
                    }

                    SupportedAppCategory.entries.forEach { category ->
                        val isEnabled = config.enabledAppCategories.contains(category.name)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = CarbonSurface,
                            border = BorderStroke(1.dp, GlassBorderSubtle)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = category.displayName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = category.packageName,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        color = TitaniumMuted,
                                        fontSize = 10.sp
                                    )
                                }
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        viewModel.toggleAppCategory(category, checked)
                                    },
                                    modifier = Modifier.testTag("filter_switch_${category.name.lowercase()}")
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Gesture & Double-Tap Configuration
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
                border = BorderStroke(1.dp, GlassBorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = "Gestures",
                            tint = SapphireAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Gesture & Interaction System",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                            Text(
                                text = "Configure Double-Tap action and free-drag behavior",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TitaniumMuted
                            )
                        }
                    }

                    SettingsToggleRow(
                        title = "Draggable Island Capsule",
                        subtitle = "Allow dragging the Compact Island directly on screen",
                        checked = config.isDraggable,
                        testTag = "switch_draggable_island",
                        onCheckedChange = { v -> viewModel.updateConfig { it.copy(isDraggable = v) } }
                    )

                    Text(
                        text = "Double-Tap Quick Action",
                        color = SapphireAccent,
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 6.dp)
                    )

                    DoubleTapAction.entries.forEach { action ->
                        val selected = config.doubleTapAction == action
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.setDoubleTapAction(action) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { viewModel.setDoubleTapAction(action) },
                                colors = RadioButtonDefaults.colors(selectedColor = SapphireAccent)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = action.label,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // 3. System Preferences, DND & Battery Saver
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = LiquidGlassSurface),
                border = BorderStroke(1.dp, GlassBorderSubtle)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SettingsSuggest,
                            contentDescription = "System Preferences",
                            tint = SapphireAccent
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "System Preferences & Power",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }

                    SettingsToggleRow(
                        title = "Respect Do Not Disturb (DND)",
                        subtitle = "Suppress automatic pop-up expansion when system DND or Silent mode is active",
                        checked = config.respectDoNotDisturb,
                        testTag = "switch_respect_dnd",
                        onCheckedChange = { v -> viewModel.updateConfig { it.copy(respectDoNotDisturb = v) } }
                    )

                    SettingsToggleRow(
                        title = "Custom Island Notification Chime",
                        subtitle = "Play subtle chime when an important notification enters the Island",
                        checked = config.customNotificationSound,
                        testTag = "switch_custom_sound",
                        onCheckedChange = { v -> viewModel.updateConfig { it.copy(customNotificationSound = v) } }
                    )

                    SettingsToggleRow(
                        title = "Battery-Friendly Mode",
                        subtitle = "Reduces continuous equalizer animations and uses low-overhead transitions",
                        checked = config.batteryFriendlyMode,
                        testTag = "switch_battery_friendly",
                        onCheckedChange = { v -> viewModel.updateConfig { it.copy(batteryFriendlyMode = v) } }
                    )

                    SettingsToggleRow(
                        title = "Hide in Landscape Fullscreen",
                        subtitle = "Automatically hide overlay when device rotates to landscape for gaming/video",
                        checked = config.hideInFullscreenLandscape,
                        testTag = "switch_hide_landscape",
                        onCheckedChange = { v -> viewModel.updateConfig { it.copy(hideInFullscreenLandscape = v) } }
                    )

                    SettingsToggleRow(
                        title = "Auto-Start on Device Boot",
                        subtitle = "Restore LEOVNISLAND overlay automatically after reboot",
                        checked = config.autoStartOnBoot,
                        testTag = "switch_auto_start_boot",
                        onCheckedChange = { v -> viewModel.updateConfig { it.copy(autoStartOnBoot = v) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = CarbonSurface,
        border = BorderStroke(1.dp, GlassBorderSubtle)
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
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
                Text(
                    text = subtitle,
                    color = TitaniumMuted,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = Modifier.testTag(testTag)
            )
        }
    }
}
