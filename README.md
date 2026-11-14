# LEOVNISLAND — Dynamic Island System for Android

**Brand:** LEOVAN  
**Platform:** Android Native (Kotlin + Jetpack Compose)  
**Architecture:** MVVM + Repository + Reactive Priority Queue Engine + Android System Services  
**Minimum SDK:** Android 10 (API 29) | **Target SDK:** Android 15/16 (API 36)

---

## 1. Overview

**LEOVNISLAND** is a full-featured native Android Dynamic Island replacement engineered with real Android system integrations (`WindowManager` with `TYPE_APPLICATION_OVERLAY`, `MediaSessionManager`, `NotificationListenerService`, `TelephonyCallback`, `BatteryManager`, `ConnectivityManager`, `CameraManager.TorchCallback`, `TileService`, and Jetpack `DataStore`).

### Key Capabilities
1. **Core Dynamic Island System**:
   - Floating overlay across all apps via `WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY`.
   - Non-focusable (`FLAG_NOT_FOCUSABLE`, `FLAG_NOT_TOUCH_MODAL`) so it never steals keyboard focus or blocks underlying app navigation.
   - Supports **Minimal**, **Compact**, **Expanded**, and **Quick Control Hub** modes.
   - Smooth spring morphing animations (`spring(dampingRatio, stiffness)`) and Display Cutout awareness (Center Punch-Hole, Left Corner Punch-Hole, Waterdrop, Auto-Detect, and Fullscreen No-Notch).
2. **Real MediaSession Mini Player**:
   - Connects to `MediaSessionManager` and `MediaController` to display real-time track title, artist, album artwork (`Bitmap`), playback progress, animated equalizer bars, and interactive `TransportControls` (`Play`, `Pause`, `SkipToNext`, `SkipToPrevious`, `SeekTo`) for Spotify, YouTube Music, Apple Music, and any compatible media player.
3. **Smart Notification System**:
   - `IslandNotificationListener` intercepts notifications from WhatsApp, Telegram, Instagram, Messenger, SMS, Gmail, Discord, and other Android apps.
   - Respects private/redacted notifications (`VISIBILITY_PRIVATE` / `VISIBILITY_SECRET`) and stores nothing permanently on disk.
   - Supports multi-notification queueing, automatic grouping (`+N`), per-app category filtering, and tap-to-open `PendingIntent`.
4. **Live Activities & 8-Level Priority Queue**:
   - Strict priority arbitration:
     1. `Incoming Call`
     2. `Ongoing Call`
     3. `Active Music`
     4. `Important Notification`
     5. `Timer & Stopwatch`
     6. `Battery Status`
     7. `Connectivity & System Telemetry (Wi-Fi, Bluetooth, Flashlight, Volume, Next Alarm)`
     8. `Idle`
5. **Liquid Glass Customization Studio**:
   - Themes: **Liquid Glass**, **Premium Dark**, **Classic iOS**, **Minimal Black**, and **Custom Studio**.
   - Live real-time preview with sliders for Width, Height, X/Y Offset, Corner Radius, Opacity, Glass Blur, Border Intensity, Spring Speed, and Auto-Collapse duration, persisted via Jetpack `DataStore`.

---

## 2. Project Structure

```text
app/src/main/java/com/example/
├── data/
│   ├── preferences/IslandPreferencesDataStore.kt
│   └── repository/IslandRepository.kt
├── domain/
│   ├── manager/IslandPriorityEngine.kt
│   └── model/
│       ├── IslandConfiguration.kt
│       └── IslandModels.kt
├── island/
│   ├── animation/IslandAnimations.kt
│   ├── components/DynamicIslandOverlayContent.kt
│   ├── manager/IslandStateManager.kt
│   └── state/IslandUiState.kt
├── receiver/
│   ├── BootCompletedReceiver.kt
│   └── SystemEventReceiver.kt
├── service/
│   ├── ActivityMonitorService.kt
│   ├── IslandNotificationListener.kt
│   ├── IslandOverlayService.kt
│   ├── IslandTileService.kt
│   └── MediaSessionService.kt
├── ui/
│   ├── customization/CustomizationStudioScreen.kt
│   ├── dashboard/
│   │   ├── DashboardScreen.kt
│   │   └── IslandViewModel.kt
│   ├── permissions/PermissionsAndTroubleshootingScreen.kt
│   ├── settings/SettingsScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
├── utils/SystemPermissionHelper.kt
└── MainActivity.kt
```

---

## 3. Setup & Build Instructions

### Prerequisites
- Android Studio Ladybug / Meerkat or newer
- JDK 11+
- Android SDK 36

### Building & Running Locally
1. Open the project root directory in **Android Studio**.
2. Assemble the debug APK from terminal:
   ```bash
   gradle :app:assembleDebug
   ```
3. Run the 18-point automated Robolectric & Compose verification suite:
   ```bash
   gradle :app:testDebugUnitTest
   ```

### Building APK Automatically on GitHub (GitHub Actions)
This project includes a ready-to-use GitHub Actions workflow at `.github/workflows/build-apk.yml`:
1. Click the **GitHub / Export** icon in AI Studio to push this project to your GitHub repository.
2. Open your repository on **GitHub** and click the **Actions** tab.
3. The **Build LEOVNISLAND Android APK** workflow runs automatically on every push (or you can trigger it manually via **Run workflow**).
4. Once the workflow finishes, scroll down to the **Artifacts** section of the workflow run and download **`LEOVNISLAND-debug-apk`** (`LEOVNISLAND-v1.0.0-debug.apk`).
5. *(Optional)* Pushing a version tag (e.g., `git tag v1.0.0 && git push origin v1.0.0`) automatically publishes a GitHub Release with `LEOVNISLAND-v1.0.0-debug.apk` attached.

### First-Run Device Permissions Setup
1. Open **LEOVNISLAND** and navigate to the **System** tab.
2. Grant **Draw Over Other Apps** (`SYSTEM_ALERT_WINDOW`).
3. Grant **Notification & Media Session Access** (`BIND_NOTIFICATION_LISTENER_SERVICE`).
   - *Note for Android 13, 14, and 15*: If sideloading the APK triggers Android's "Restricted setting" dialog, open **Settings → Apps → LEOVNISLAND → 3-dot menu → Allow restricted settings**, then enable Notification Access.
4. Toggle **Floating WindowManager Overlay** ON in the **Dashboard** tab or via the **LEOVNISLAND Quick Settings Tile** in your Android notification shade.
