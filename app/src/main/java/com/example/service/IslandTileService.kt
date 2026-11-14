package com.example.service

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.example.MainActivity
import com.example.island.manager.IslandStateManager
import com.example.utils.SystemPermissionHelper

/**
 * Android Quick Settings Tile allowing instant toggle of the LEOVNISLAND overlay
 * directly from the system notification shade.
 */
class IslandTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        syncTileState()
    }

    override fun onClick() {
        super.onClick()
        if (!SystemPermissionHelper.canDrawOverlays(this)) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
            return
        }

        val isRunning = IslandStateManager.state.value.isOverlayServiceRunning
        if (isRunning) {
            IslandOverlayService.stop(this)
        } else {
            IslandOverlayService.start(this)
        }
        syncTileState(!isRunning)
    }

    private fun syncTileState(overrideActive: Boolean? = null) {
        val tile = qsTile ?: return
        val isActive = overrideActive ?: IslandStateManager.state.value.isOverlayServiceRunning
        tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "LEOVNISLAND"
        tile.subtitle = if (isActive) "Active" else "Off"
        tile.updateTile()
    }
}
