# DosePulse 💊
### Lock-Screen Medication Adherence Protocol

A reliable, lock-screen-ready medication reminder app built with Kotlin & Jetpack Compose. DosePulse enforces medication adherence through **persistent, unignorable full-screen alarms, continuous looping audio alerts, slide-to-confirm dosage verification**, and a comprehensive daily compliance dashboard.

---

## 📦 Package & Release Information
- **Application Name**: DosePulse
- **Package ID / Application ID**: `com.sglenn.dosepulse`
- **Initial Release Version**: `0.1` (`versionCode = 1`, `versionName = "0.1"`)
- **Target SDK**: Android 15 (API 35) | **Min SDK**: Android 8.0 (API 26)

---

## 🌟 Core Features

### 1. 🚨 Zero-Ignorance Lock-Screen Alarm (`AlarmAlertActivity.kt`)
- **Full-Screen Display Over Lock Screen**: Automatically wakes display, turns screen on, and bypasses the keyguard (`setShowWhenLocked(true)`, `setTurnScreenOn(true)`).
- **Physical Slide-to-Confirm Gesture**: Accidental dismissal prevention via `SlideToConfirm` drag slider with haptic feedback to prevent groggy or accidental taps.
- **Pulsing Visual Alert**: High-visibility clinical card displaying pill shape, color badge, dose strength, and intake instructions (e.g., *"Take with food and a full glass of water"*).
- **Snooze & Skip Protocols**: Quick action buttons for Snooze (10m), Snooze (15m), and Skip with clinical reason.

### 2. 🔊 Continuous Looping Audio & Ringtone Engine (`AlarmSoundService.kt`)
- Dedicated foreground alarm service utilizing `AudioAttributes.USAGE_ALARM` with looping playback and rhythmic vibration patterns.
- Custom ringtone selection (System Alarm, Gentle Chime, Clinical Beep, Urgent Siren).
- **3-Minute Safety Auto-Silence**: Automatically silences audio and posts a "Missed Dose" notification after 3 minutes to prevent infinite blaring.
- **Live In-App Emergency Silence Banner**: Real-time banner on the dashboard allowing 1-tap silence/take controls if the alarm is currently ringing.

### 3. 🕒 12-Hour AM / PM Selection & Time Presets
- Native 12-Hour time picker with dedicated **AM / PM buttons**.
- Interactive `[ AM ]` / `[ PM ]` quick toggle pill on each dose time item.
- Quick preset chips: `🌅 Morning (8:00 AM)`, `☀️ Noon (12:00 PM)`, `🌆 Evening (6:00 PM)`, `🌙 Bedtime (10:00 PM)`.

### 4. 🎨 Theme & Appearance Customization (`ThemeSelectionDialog.kt`)
- Tap the **Palette 🎨 icon** in the top navigation bar to open the Theme Customization Modal.
- Supports **Light Mode**, **Dark Mode**, and **System Default**.
- 7 Clinical Accent Palettes: Medical Teal, Ocean Cyan, Emerald Health, Royal Indigo, Amethyst Purple, Rose Coral, Warm Amber.

### 5. 📊 Daily Compliance Dashboard (`DashboardScreen.kt`)
- **Compliance Ring Gauge**: Real-time daily adherence percentage (e.g. *80% Adherence Today*) and active streak counter.
- **One-Tap Alarm Simulator**: Immediate **"Test Alarm (5s)"** button to test the lockscreen wakeup and looping audio without waiting.
- **Pill Inventory Tracker**: Bottle count with low-supply refill warnings (e.g. *5 pills left - Refill soon*).
- **Chronological Timeline**: Cards color-coded by medical status (Emerald for Taken, Amber for Snoozed, Vibrant Coral Red for Urgent/Due).

### 6. 📚 50+ Clinical Medication Database (`MedicationDatabaseScreen.kt`)
- Pre-seeded database covering 9 clinical specialties (Cardiology, Diabetes, Mental Health, Respiratory, Antibiotics, Pain, GI, Vitamins).
- 1-tap **"Add to My Medications"** automatically configures dosages and food/intake guidance.

---

## 🚀 How to Build & Publish

### In Android Studio:
1. Open Android Studio -> **Open** -> Select `/home/sglenn21/Downloads/medicine`.
2. Select **Build > Generate Signed Bundle / APK...**
3. Choose **Android App Bundle (.aab)** for Google Play Console or **APK** for direct device installation.

### Build via Command Line:
```bash
./gradlew assembleDebug
```
Output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`
