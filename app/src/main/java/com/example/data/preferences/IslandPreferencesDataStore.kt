package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AnimationPreset
import com.example.domain.model.CutoutAdaptationMode
import com.example.domain.model.DoubleTapAction
import com.example.domain.model.IslandConfiguration
import com.example.domain.model.IslandThemeStyle
import com.example.domain.model.SupportedAppCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.islandDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "leovnisland_preferences"
)

class IslandPreferencesDataStore(
    private val context: Context,
    private val customDataStore: DataStore<Preferences>? = null
) {
    private val dataStore: DataStore<Preferences>
        get() = customDataStore ?: context.islandDataStore

    private object Keys {
        val IS_ENABLED = booleanPreferencesKey("is_island_enabled")
        val IS_VISIBLE = booleanPreferencesKey("is_island_visible")
        val THEME_STYLE = stringPreferencesKey("theme_style")
        val COMPACT_WIDTH = floatPreferencesKey("compact_width_dp")
        val COMPACT_HEIGHT = floatPreferencesKey("compact_height_dp")
        val EXPANDED_WIDTH = floatPreferencesKey("expanded_width_dp")
        val EXPANDED_HEIGHT = floatPreferencesKey("expanded_height_dp")
        val MINIMAL_SCALE = floatPreferencesKey("minimal_size_scale")
        val COMPACT_SCALE = floatPreferencesKey("compact_size_scale")
        val EXPANDED_SCALE = floatPreferencesKey("expanded_size_scale")
        val HORIZONTAL_OFFSET = floatPreferencesKey("horizontal_offset_dp")
        val VERTICAL_OFFSET = floatPreferencesKey("vertical_offset_dp")
        val CORNER_RADIUS = floatPreferencesKey("corner_radius_dp")
        val TRANSPARENCY = floatPreferencesKey("transparency_alpha")
        val BLUR_INTENSITY = floatPreferencesKey("blur_intensity")
        val BORDER_INTENSITY = floatPreferencesKey("border_intensity")
        val CUSTOM_ACCENT_HEX = longPreferencesKey("custom_accent_hex")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed_multiplier")
        val ANIMATION_PRESET = stringPreferencesKey("animation_preset")
        val AUTO_COLLAPSE_SECONDS = intPreferencesKey("auto_collapse_seconds")
        val IS_DRAGGABLE = booleanPreferencesKey("is_draggable")
        val CUTOUT_MODE = stringPreferencesKey("cutout_mode")
        val DOUBLE_TAP_ACTION = stringPreferencesKey("double_tap_action")
        val CUSTOM_SOUND = booleanPreferencesKey("custom_notification_sound")
        val RESPECT_DND = booleanPreferencesKey("respect_dnd")
        val HIDE_LANDSCAPE = booleanPreferencesKey("hide_in_landscape")
        val BATTERY_FRIENDLY = booleanPreferencesKey("battery_friendly_mode")
        val AUTO_START_BOOT = booleanPreferencesKey("auto_start_on_boot")
        val ENABLED_APP_CATEGORIES = stringSetPreferencesKey("enabled_app_categories")
    }

    val configurationFlow: Flow<IslandConfiguration> = dataStore.data.map { prefs ->
        val default = IslandConfiguration()
        IslandConfiguration(
            isIslandEnabled = prefs[Keys.IS_ENABLED] ?: default.isIslandEnabled,
            isIslandVisible = prefs[Keys.IS_VISIBLE] ?: default.isIslandVisible,
            themeStyle = prefs[Keys.THEME_STYLE]?.let {
                runCatching { IslandThemeStyle.valueOf(it) }.getOrNull()
            } ?: default.themeStyle,
            compactWidthDp = prefs[Keys.COMPACT_WIDTH] ?: default.compactWidthDp,
            compactHeightDp = prefs[Keys.COMPACT_HEIGHT] ?: default.compactHeightDp,
            expandedWidthDp = prefs[Keys.EXPANDED_WIDTH] ?: default.expandedWidthDp,
            expandedHeightDp = prefs[Keys.EXPANDED_HEIGHT] ?: default.expandedHeightDp,
            minimalSizeScale = prefs[Keys.MINIMAL_SCALE] ?: default.minimalSizeScale,
            compactSizeScale = prefs[Keys.COMPACT_SCALE] ?: default.compactSizeScale,
            expandedSizeScale = prefs[Keys.EXPANDED_SCALE] ?: default.expandedSizeScale,
            horizontalOffsetDp = prefs[Keys.HORIZONTAL_OFFSET] ?: default.horizontalOffsetDp,
            verticalOffsetDp = prefs[Keys.VERTICAL_OFFSET] ?: default.verticalOffsetDp,
            cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: default.cornerRadiusDp,
            transparencyAlpha = prefs[Keys.TRANSPARENCY] ?: default.transparencyAlpha,
            blurIntensity = prefs[Keys.BLUR_INTENSITY] ?: default.blurIntensity,
            borderIntensity = prefs[Keys.BORDER_INTENSITY] ?: default.borderIntensity,
            customAccentHex = prefs[Keys.CUSTOM_ACCENT_HEX] ?: default.customAccentHex,
            animationSpeedMultiplier = prefs[Keys.ANIMATION_SPEED] ?: default.animationSpeedMultiplier,
            animationPreset = prefs[Keys.ANIMATION_PRESET]?.let {
                runCatching { AnimationPreset.valueOf(it) }.getOrNull()
            } ?: default.animationPreset,
            autoCollapseSeconds = prefs[Keys.AUTO_COLLAPSE_SECONDS] ?: default.autoCollapseSeconds,
            isDraggable = prefs[Keys.IS_DRAGGABLE] ?: default.isDraggable,
            cutoutMode = prefs[Keys.CUTOUT_MODE]?.let {
                runCatching { CutoutAdaptationMode.valueOf(it) }.getOrNull()
            } ?: default.cutoutMode,
            doubleTapAction = prefs[Keys.DOUBLE_TAP_ACTION]?.let {
                runCatching { DoubleTapAction.valueOf(it) }.getOrNull()
            } ?: default.doubleTapAction,
            customNotificationSound = prefs[Keys.CUSTOM_SOUND] ?: default.customNotificationSound,
            respectDoNotDisturb = prefs[Keys.RESPECT_DND] ?: default.respectDoNotDisturb,
            hideInFullscreenLandscape = prefs[Keys.HIDE_LANDSCAPE] ?: default.hideInFullscreenLandscape,
            batteryFriendlyMode = prefs[Keys.BATTERY_FRIENDLY] ?: default.batteryFriendlyMode,
            autoStartOnBoot = prefs[Keys.AUTO_START_BOOT] ?: default.autoStartOnBoot,
            enabledAppCategories = prefs[Keys.ENABLED_APP_CATEGORIES]
                ?: SupportedAppCategory.entries.map { it.name }.toSet()
        )
    }

    suspend fun updateConfiguration(transform: (IslandConfiguration) -> IslandConfiguration) {
        dataStore.edit { prefs ->
            val default = IslandConfiguration()
            val current = IslandConfiguration(
                isIslandEnabled = prefs[Keys.IS_ENABLED] ?: default.isIslandEnabled,
                isIslandVisible = prefs[Keys.IS_VISIBLE] ?: default.isIslandVisible,
                themeStyle = prefs[Keys.THEME_STYLE]?.let {
                    runCatching { IslandThemeStyle.valueOf(it) }.getOrNull()
                } ?: default.themeStyle,
                compactWidthDp = prefs[Keys.COMPACT_WIDTH] ?: default.compactWidthDp,
                compactHeightDp = prefs[Keys.COMPACT_HEIGHT] ?: default.compactHeightDp,
                expandedWidthDp = prefs[Keys.EXPANDED_WIDTH] ?: default.expandedWidthDp,
                expandedHeightDp = prefs[Keys.EXPANDED_HEIGHT] ?: default.expandedHeightDp,
                minimalSizeScale = prefs[Keys.MINIMAL_SCALE] ?: default.minimalSizeScale,
                compactSizeScale = prefs[Keys.COMPACT_SCALE] ?: default.compactSizeScale,
                expandedSizeScale = prefs[Keys.EXPANDED_SCALE] ?: default.expandedSizeScale,
                horizontalOffsetDp = prefs[Keys.HORIZONTAL_OFFSET] ?: default.horizontalOffsetDp,
                verticalOffsetDp = prefs[Keys.VERTICAL_OFFSET] ?: default.verticalOffsetDp,
                cornerRadiusDp = prefs[Keys.CORNER_RADIUS] ?: default.cornerRadiusDp,
                transparencyAlpha = prefs[Keys.TRANSPARENCY] ?: default.transparencyAlpha,
                blurIntensity = prefs[Keys.BLUR_INTENSITY] ?: default.blurIntensity,
                borderIntensity = prefs[Keys.BORDER_INTENSITY] ?: default.borderIntensity,
                customAccentHex = prefs[Keys.CUSTOM_ACCENT_HEX] ?: default.customAccentHex,
                animationSpeedMultiplier = prefs[Keys.ANIMATION_SPEED] ?: default.animationSpeedMultiplier,
                animationPreset = prefs[Keys.ANIMATION_PRESET]?.let {
                    runCatching { AnimationPreset.valueOf(it) }.getOrNull()
                } ?: default.animationPreset,
                autoCollapseSeconds = prefs[Keys.AUTO_COLLAPSE_SECONDS] ?: default.autoCollapseSeconds,
                isDraggable = prefs[Keys.IS_DRAGGABLE] ?: default.isDraggable,
                cutoutMode = prefs[Keys.CUTOUT_MODE]?.let {
                    runCatching { CutoutAdaptationMode.valueOf(it) }.getOrNull()
                } ?: default.cutoutMode,
                doubleTapAction = prefs[Keys.DOUBLE_TAP_ACTION]?.let {
                    runCatching { DoubleTapAction.valueOf(it) }.getOrNull()
                } ?: default.doubleTapAction,
                customNotificationSound = prefs[Keys.CUSTOM_SOUND] ?: default.customNotificationSound,
                respectDoNotDisturb = prefs[Keys.RESPECT_DND] ?: default.respectDoNotDisturb,
                hideInFullscreenLandscape = prefs[Keys.HIDE_LANDSCAPE] ?: default.hideInFullscreenLandscape,
                batteryFriendlyMode = prefs[Keys.BATTERY_FRIENDLY] ?: default.batteryFriendlyMode,
                autoStartOnBoot = prefs[Keys.AUTO_START_BOOT] ?: default.autoStartOnBoot,
                enabledAppCategories = prefs[Keys.ENABLED_APP_CATEGORIES]
                    ?: SupportedAppCategory.entries.map { it.name }.toSet()
            )
            val updated = transform(current)
            prefs[Keys.IS_ENABLED] = updated.isIslandEnabled
            prefs[Keys.IS_VISIBLE] = updated.isIslandVisible
            prefs[Keys.THEME_STYLE] = updated.themeStyle.name
            prefs[Keys.COMPACT_WIDTH] = updated.compactWidthDp
            prefs[Keys.COMPACT_HEIGHT] = updated.compactHeightDp
            prefs[Keys.EXPANDED_WIDTH] = updated.expandedWidthDp
            prefs[Keys.EXPANDED_HEIGHT] = updated.expandedHeightDp
            prefs[Keys.MINIMAL_SCALE] = updated.minimalSizeScale
            prefs[Keys.COMPACT_SCALE] = updated.compactSizeScale
            prefs[Keys.EXPANDED_SCALE] = updated.expandedSizeScale
            prefs[Keys.HORIZONTAL_OFFSET] = updated.horizontalOffsetDp
            prefs[Keys.VERTICAL_OFFSET] = updated.verticalOffsetDp
            prefs[Keys.CORNER_RADIUS] = updated.cornerRadiusDp
            prefs[Keys.TRANSPARENCY] = updated.transparencyAlpha
            prefs[Keys.BLUR_INTENSITY] = updated.blurIntensity
            prefs[Keys.BORDER_INTENSITY] = updated.borderIntensity
            prefs[Keys.CUSTOM_ACCENT_HEX] = updated.customAccentHex
            prefs[Keys.ANIMATION_SPEED] = updated.animationSpeedMultiplier
            prefs[Keys.ANIMATION_PRESET] = updated.animationPreset.name
            prefs[Keys.AUTO_COLLAPSE_SECONDS] = updated.autoCollapseSeconds
            prefs[Keys.IS_DRAGGABLE] = updated.isDraggable
            prefs[Keys.CUTOUT_MODE] = updated.cutoutMode.name
            prefs[Keys.DOUBLE_TAP_ACTION] = updated.doubleTapAction.name
            prefs[Keys.CUSTOM_SOUND] = updated.customNotificationSound
            prefs[Keys.RESPECT_DND] = updated.respectDoNotDisturb
            prefs[Keys.HIDE_LANDSCAPE] = updated.hideInFullscreenLandscape
            prefs[Keys.BATTERY_FRIENDLY] = updated.batteryFriendlyMode
            prefs[Keys.AUTO_START_BOOT] = updated.autoStartOnBoot
            prefs[Keys.ENABLED_APP_CATEGORIES] = updated.enabledAppCategories
        }
    }

    suspend fun resetToDefaults() {
        updateConfiguration { IslandConfiguration() }
    }
}
