package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.repository.IslandRepository
import com.example.service.IslandOverlayService
import com.example.utils.SystemPermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context ?: return
        val action = intent?.action ?: return
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val config = IslandRepository.getInstance(ctx).configurationFlow.first()
                    if (config.autoStartOnBoot && config.isIslandEnabled && SystemPermissionHelper.canDrawOverlays(ctx)) {
                        IslandOverlayService.start(ctx)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
