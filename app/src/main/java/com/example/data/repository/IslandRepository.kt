package com.example.data.repository

import android.content.Context
import com.example.data.preferences.IslandPreferencesDataStore
import com.example.domain.model.IslandConfiguration
import com.example.domain.model.SupportedAppCategory
import kotlinx.coroutines.flow.Flow

class IslandRepository(
    context: Context,
    private val preferencesDataStore: IslandPreferencesDataStore = IslandPreferencesDataStore(context.applicationContext)
) {
    val configurationFlow: Flow<IslandConfiguration> = preferencesDataStore.configurationFlow

    suspend fun updateConfiguration(transform: (IslandConfiguration) -> IslandConfiguration) {
        preferencesDataStore.updateConfiguration(transform)
    }

    suspend fun setIslandEnabled(enabled: Boolean) {
        preferencesDataStore.updateConfiguration { it.copy(isIslandEnabled = enabled) }
    }

    suspend fun setIslandVisible(visible: Boolean) {
        preferencesDataStore.updateConfiguration { it.copy(isIslandVisible = visible) }
    }

    suspend fun updatePositionOffset(horizontalDp: Float, verticalDp: Float) {
        preferencesDataStore.updateConfiguration {
            it.copy(
                horizontalOffsetDp = horizontalDp.coerceIn(-160f, 160f),
                verticalOffsetDp = verticalDp.coerceIn(0f, 180f)
            )
        }
    }

    suspend fun toggleAppCategoryFilter(category: SupportedAppCategory, enabled: Boolean) {
        preferencesDataStore.updateConfiguration { current ->
            val mutable = current.enabledAppCategories.toMutableSet()
            if (enabled) {
                mutable.add(category.name)
            } else {
                mutable.remove(category.name)
            }
            current.copy(enabledAppCategories = mutable)
        }
    }

    suspend fun resetToDefaults() {
        preferencesDataStore.resetToDefaults()
    }

    companion object {
        @Volatile
        private var INSTANCE: IslandRepository? = null

        fun getInstance(context: Context): IslandRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: IslandRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}
