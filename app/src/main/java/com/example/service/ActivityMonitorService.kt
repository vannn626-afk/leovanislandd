package com.example.service

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.telephony.PhoneStateListener
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.example.domain.model.BatteryActivityState
import com.example.domain.model.CallActivityState
import com.example.domain.model.CallStatus
import com.example.island.manager.IslandStateManager
import com.example.receiver.SystemEventReceiver
import com.example.utils.SystemPermissionHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Monitors real-time system activities using event-driven callbacks and receivers:
 * - Battery (percentage, charging, power source, temperature, low/full alerts)
 * - Phone Calls (incoming, ongoing, call duration via TelephonyCallback / PhoneStateListener)
 * - Connectivity (Wi-Fi, Cellular, Bluetooth, Airplane Mode)
 * - Flashlight (CameraManager.TorchCallback + real torch toggle)
 * - Alarms & Audio Volume / DND state
 */
class ActivityMonitorService : Service() {

    private val binder = LocalBinder()
    private var systemEventReceiver: SystemEventReceiver? = null
    private var connectivityManager: ConnectivityManager? = null
    private var cameraManager: CameraManager? = null
    private var primaryCameraId: String? = null
    private var isTorchCurrentlyOn: Boolean = false
    private var telephonyManager: TelephonyManager? = null
    private var telephonyCallbackApi31: Any? = null
    @Suppress("DEPRECATION")
    private var legacyPhoneStateListener: PhoneStateListener? = null

    inner class LocalBinder : Binder() {
        fun getService(): ActivityMonitorService = this@ActivityMonitorService
    }

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            refreshNetworkState(showTransient = false)
        }

        override fun onLost(network: Network) {
            refreshNetworkState(showTransient = true)
        }

        override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
            refreshNetworkState(showTransient = false)
        }
    }

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (primaryCameraId == null || cameraId == primaryCameraId) {
                isTorchCurrentlyOn = enabled
                IslandStateManager.updateConnectivityState(
                    transform = { it.copy(isFlashlightOn = enabled, isFlashlightAvailable = true) },
                    transientHeadline = if (enabled) "Flashlight Active" else null
                )
            }
        }

        override fun onTorchModeUnavailable(cameraId: String) {
            if (cameraId == primaryCameraId) {
                IslandStateManager.updateConnectivityState(
                    transform = { it.copy(isFlashlightOn = false) }
                )
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        instance = this
        registerSystemReceiver()
        registerNetworkCallbacks()
        registerTorchCallbacks()
        registerCallStateCallbacks()
        refreshInitialStates()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        refreshCallMonitoringIfPermitted()
        refreshInitialStates()
        return START_STICKY
    }

    private fun registerSystemReceiver() {
        val receiver = SystemEventReceiver()
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(Intent.ACTION_AIRPLANE_MODE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(AlarmManager.ACTION_NEXT_ALARM_CLOCK_CHANGED)
            addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
            addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
            addAction("android.media.VOLUME_CHANGED_ACTION")
        }
        ContextCompat.registerReceiver(
            this,
            receiver,
            filter,
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        systemEventReceiver = receiver
    }

    private fun registerNetworkCallbacks() {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return
        connectivityManager = cm
        runCatching {
            cm.registerDefaultNetworkCallback(networkCallback)
        }
    }

    private fun registerTorchCallbacks() {
        val camMgr = getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return
        cameraManager = camMgr
        runCatching {
            primaryCameraId = camMgr.cameraIdList.firstOrNull { id ->
                camMgr.getCameraCharacteristics(id)
                    .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            camMgr.registerTorchCallback(torchCallback, null)
        }

        IslandStateManager.flashlightToggleAction = {
            toggleFlashlight()
        }
    }

    fun toggleFlashlight() {
        val camMgr = cameraManager
        val camId = primaryCameraId
        if (camMgr != null && camId != null) {
            try {
                val target = !isTorchCurrentlyOn
                camMgr.setTorchMode(camId, target)
                isTorchCurrentlyOn = target
                IslandStateManager.updateConnectivityState(
                    transform = { it.copy(isFlashlightOn = target) },
                    transientHeadline = if (target) "Flashlight On" else "Flashlight Off"
                )
            } catch (_: Exception) {
                // Fallback state toggle if hardware torch is unavailable (e.g., emulator)
                val target = !IslandStateManager.state.value.connectivityState.isFlashlightOn
                IslandStateManager.updateConnectivityState(
                    transform = { it.copy(isFlashlightOn = target) },
                    transientHeadline = if (target) "Flashlight On" else "Flashlight Off"
                )
            }
        } else {
            val target = !IslandStateManager.state.value.connectivityState.isFlashlightOn
            IslandStateManager.updateConnectivityState(
                transform = { it.copy(isFlashlightOn = target) },
                transientHeadline = if (target) "Flashlight On" else "Flashlight Off"
            )
        }
    }

    fun refreshCallMonitoringIfPermitted() {
        registerCallStateCallbacks()
    }

    private fun registerCallStateCallbacks() {
        if (!SystemPermissionHelper.hasPhoneStatePermission(this)) return
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager ?: return
        telephonyManager = tm

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (telephonyCallbackApi31 != null) return
                val callback = object : TelephonyCallback(), TelephonyCallback.CallStateListener {
                    override fun onCallStateChanged(state: Int) {
                        handleTelephonyCallState(state)
                    }
                }
                tm.registerTelephonyCallback(mainExecutor, callback)
                telephonyCallbackApi31 = callback
            } else {
                @Suppress("DEPRECATION")
                if (legacyPhoneStateListener != null) return
                @Suppress("DEPRECATION")
                val listener = object : PhoneStateListener() {
                    @Deprecated("Deprecated in Java")
                    override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                        handleTelephonyCallState(state, phoneNumber)
                    }
                }
                @Suppress("DEPRECATION")
                tm.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
                legacyPhoneStateListener = listener
            }
        } catch (_: SecurityException) {
        } catch (_: Exception) {
        }
    }

    private fun handleTelephonyCallState(state: Int, number: String? = null) {
        when (state) {
            TelephonyManager.CALL_STATE_RINGING -> {
                IslandStateManager.updateCallState(
                    CallActivityState(
                        status = CallStatus.INCOMING,
                        callerName = "Incoming Call",
                        callerNumber = number?.takeIf { it.isNotBlank() } ?: "Cellular Line",
                        callStartTimestampMs = System.currentTimeMillis(),
                        durationSeconds = 0L
                    )
                )
            }
            TelephonyManager.CALL_STATE_OFFHOOK -> {
                IslandStateManager.updateCallState(
                    CallActivityState(
                        status = CallStatus.ONGOING,
                        callerName = "Active Call",
                        callerNumber = number?.takeIf { it.isNotBlank() } ?: "Connected",
                        callStartTimestampMs = System.currentTimeMillis(),
                        durationSeconds = 0L
                    )
                )
            }
            TelephonyManager.CALL_STATE_IDLE -> {
                IslandStateManager.updateCallState(CallActivityState(status = CallStatus.NONE))
            }
        }
    }

    private fun refreshInitialStates() {
        // Initial battery check via sticky broadcast
        val batteryIntent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        if (batteryIntent != null) {
            val batteryState = parseBatteryIntent(batteryIntent, isAlert = false)
            IslandStateManager.updateBatteryState(batteryState, showTransientExpand = false)
        }
        refreshNetworkState(showTransient = false)
        refreshAudioAndAlarmState()
    }

    fun refreshNetworkState(showTransient: Boolean) {
        val cm = connectivityManager ?: (getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager)
        val activeNet = cm?.activeNetwork
        val caps = if (activeNet != null) cm.getNetworkCapabilities(activeNet) else null

        val wifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true
        val cell = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true
        val isAirplane = Settings.Global.getInt(contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

        val btConnected = runCatching {
            if (SystemPermissionHelper.hasBluetoothConnectPermission(this)) {
                val btMgr = getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
                btMgr?.adapter?.isEnabled == true
            } else {
                false
            }
        }.getOrDefault(false)

        val netLabel = when {
            isAirplane -> "Airplane Mode"
            wifi -> "Wi-Fi Connected"
            cell -> "Cellular Data"
            else -> "Offline"
        }

        IslandStateManager.updateConnectivityState(
            transform = {
                it.copy(
                    isWifiConnected = wifi,
                    wifiSsidOrStatus = if (wifi) "Wi-Fi Active" else "Wi-Fi Disconnected",
                    isCellularConnected = cell,
                    networkLabel = netLabel,
                    isAirplaneMode = isAirplane,
                    isBluetoothConnected = it.isBluetoothConnected || btConnected
                )
            },
            transientHeadline = if (showTransient) netLabel else null
        )
    }

    fun refreshAudioAndAlarmState(showVolumeBanner: Boolean = false) {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val maxVol = (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)
        val curVol = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 10
        val volPercent = ((curVol.toFloat() / maxVol.toFloat()) * 100f).toInt().coerceIn(0, 100)
        val ringerMuted = audioManager?.ringerMode != AudioManager.RINGER_MODE_NORMAL
        val dndActive = SystemPermissionHelper.isDoNotDisturbActive(this)

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val nextAlarmClock = alarmManager?.nextAlarmClock
        val formattedAlarm = nextAlarmClock?.triggerTime?.let { triggerMs ->
            SimpleDateFormat("EEE HH:mm", Locale.getDefault()).format(Date(triggerMs))
        }

        IslandStateManager.updateConnectivityState(
            transform = {
                it.copy(
                    currentVolumePercent = volPercent,
                    isMutedOrDnd = ringerMuted || dndActive,
                    nextAlarmFormatted = formattedAlarm
                )
            },
            transientHeadline = if (showVolumeBanner) "Volume $volPercent%" else null
        )
    }

    override fun onDestroy() {
        systemEventReceiver?.let {
            runCatching { unregisterReceiver(it) }
        }
        systemEventReceiver = null
        connectivityManager?.let {
            runCatching { it.unregisterNetworkCallback(networkCallback) }
        }
        cameraManager?.let {
            runCatching { it.unregisterTorchCallback(torchCallback) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val cb = telephonyCallbackApi31 as? TelephonyCallback
            if (cb != null) {
                runCatching { telephonyManager?.unregisterTelephonyCallback(cb) }
            }
        } else {
            @Suppress("DEPRECATION")
            legacyPhoneStateListener?.let {
                runCatching { telephonyManager?.listen(it, PhoneStateListener.LISTEN_NONE) }
            }
        }
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
    }

    companion object {
        @Volatile
        var instance: ActivityMonitorService? = null
            private set

        fun parseBatteryIntent(intent: Intent, isAlert: Boolean): BatteryActivityState {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val percentage = if (level >= 0 && scale > 0) {
                ((level.toFloat() / scale.toFloat()) * 100f).toInt().coerceIn(0, 100)
            } else {
                100
            }
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
            val isFull = status == BatteryManager.BATTERY_STATUS_FULL || (isCharging && percentage >= 100)
            val isLow = !isCharging && percentage <= 20

            val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
            val source = when (plugged) {
                BatteryManager.BATTERY_PLUGGED_AC -> "Fast AC Charger"
                BatteryManager.BATTERY_PLUGGED_USB -> "USB Port"
                BatteryManager.BATTERY_PLUGGED_WIRELESS -> "MagSafe / Wireless"
                else -> "Battery"
            }
            val tempTenths = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 280)
            val tempCelsius = tempTenths / 10f

            return BatteryActivityState(
                percentage = percentage,
                isCharging = isCharging,
                isFull = isFull,
                isLow = isLow,
                powerSource = source,
                temperatureCelsius = tempCelsius,
                lastAlertTimestamp = if (isAlert || isCharging || isLow || isFull) {
                    System.currentTimeMillis()
                } else {
                    0L
                }
            )
        }
    }
}
