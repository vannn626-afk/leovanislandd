package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import com.example.island.manager.IslandStateManager
import com.example.service.ActivityMonitorService
import com.example.utils.SystemPermissionHelper

class SystemEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        val ctx = context ?: return
        val action = intent?.action ?: return

        when (action) {
            Intent.ACTION_BATTERY_CHANGED -> {
                val prev = IslandStateManager.state.value.batteryState
                val parsed = ActivityMonitorService.parseBatteryIntent(intent, isAlert = false)
                val chargingChanged = prev.isCharging != parsed.isCharging
                val fullTriggered = !prev.isFull && parsed.isFull
                val lowTriggered = !prev.isLow && parsed.isLow
                val showExpand = chargingChanged || fullTriggered || lowTriggered
                val stateWithAlert = if (showExpand) {
                    parsed.copy(lastAlertTimestamp = System.currentTimeMillis())
                } else {
                    parsed.copy(lastAlertTimestamp = prev.lastAlertTimestamp)
                }
                IslandStateManager.updateBatteryState(stateWithAlert, showTransientExpand = showExpand)
            }

            Intent.ACTION_POWER_CONNECTED -> {
                val cur = IslandStateManager.state.value.batteryState
                IslandStateManager.updateBatteryState(
                    cur.copy(
                        isCharging = true,
                        lastAlertTimestamp = System.currentTimeMillis()
                    ),
                    showTransientExpand = true
                )
            }

            Intent.ACTION_POWER_DISCONNECTED -> {
                val cur = IslandStateManager.state.value.batteryState
                IslandStateManager.updateBatteryState(
                    cur.copy(
                        isCharging = false,
                        isFull = false,
                        lastAlertTimestamp = System.currentTimeMillis()
                    ),
                    showTransientExpand = false
                )
            }

            Intent.ACTION_BATTERY_LOW -> {
                val cur = IslandStateManager.state.value.batteryState
                IslandStateManager.updateBatteryState(
                    cur.copy(
                        isLow = true,
                        lastAlertTimestamp = System.currentTimeMillis()
                    ),
                    showTransientExpand = true
                )
            }

            Intent.ACTION_AIRPLANE_MODE_CHANGED -> {
                val isAirplane = Settings.Global.getInt(
                    ctx.contentResolver,
                    Settings.Global.AIRPLANE_MODE_ON,
                    0
                ) != 0
                IslandStateManager.updateConnectivityState(
                    transform = {
                        it.copy(
                            isAirplaneMode = isAirplane,
                            networkLabel = if (isAirplane) "Airplane Mode" else "Online"
                        )
                    },
                    transientHeadline = if (isAirplane) "Airplane Mode On" else "Airplane Mode Off"
                )
            }

            BluetoothDevice.ACTION_ACL_CONNECTED -> {
                val deviceName = runCatching {
                    if (SystemPermissionHelper.hasBluetoothConnectPermission(ctx)) {
                        @Suppress("DEPRECATION")
                        val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                        device?.name ?: "Bluetooth Audio"
                    } else {
                        "Bluetooth Device"
                    }
                }.getOrDefault("Bluetooth Device")

                IslandStateManager.updateConnectivityState(
                    transform = {
                        it.copy(
                            isBluetoothConnected = true,
                            bluetoothDeviceName = deviceName
                        )
                    },
                    transientHeadline = "Connected: $deviceName"
                )
            }

            BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                IslandStateManager.updateConnectivityState(
                    transform = {
                        it.copy(isBluetoothConnected = false)
                    },
                    transientHeadline = "Bluetooth Disconnected"
                )
            }

            BluetoothAdapter.ACTION_STATE_CHANGED -> {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                if (state == BluetoothAdapter.STATE_OFF) {
                    IslandStateManager.updateConnectivityState(
                        transform = { it.copy(isBluetoothConnected = false) }
                    )
                }
            }

            AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED,
            AudioManager.RINGER_MODE_CHANGED_ACTION,
            NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED -> {
                ActivityMonitorService.instance?.refreshAudioAndAlarmState(showVolumeBanner = false)
            }

            "android.media.VOLUME_CHANGED_ACTION" -> {
                ActivityMonitorService.instance?.refreshAudioAndAlarmState(showVolumeBanner = true)
            }
        }
    }
}
