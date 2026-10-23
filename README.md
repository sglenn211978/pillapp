# PillEase 💊

A frictionless, ad-free, 100% offline Native Android medication tracker built with Kotlin & Jetpack Compose.

Built specifically to replace bloated, ad-ridden pill reminder apps with a fast, privacy-respecting experience.

---

## ✨ Key Features

1. **⚡ 0-Click / 1-Click Logging from Lockscreen & Notification Shade**:
   - Every reminder notification includes direct action buttons: `[ ✓ Taken ]`, `[ ⏰ Snooze 15m ]`, and `[ Skip ]`.
   - Your wife never has to unlock the phone, launch the app, or sit through ads just to log a pill.
2. **📱 1-Tap In-App Dashboard**:
   - **"Take All Due"** banner: When multiple pills are due at once, one tap logs all of them simultaneously.
   - Individual one-touch checkmarks on each medication card with immediate visual feedback.
3. **🗓️ Flexible Scheduling**:
   - **Daily**: Fixed times (e.g., 8:00 AM, 8:00 PM)
   - **Specific Days**: Custom days of the week (e.g., Mon, Wed, Fri)
   - **Cyclic Regimens**: E.g. birth control cycles (21 days on / 7 days off)
   - **As Needed (PRN)**: For pain relief, allergy meds, etc. with safety cooldown intervals.
4. **🔒 100% Offline & Private**:
   - Powered by a local SQLite database (Room).
   - Zero login accounts, zero cloud servers, zero ads, zero tracking.
5. **📋 Export for Doctor Visits**:
   - Single-tap export of recent medication adherence history copied cleanly to the clipboard.

---

## 🚀 How to Run & Install on Android

### Option A: Open in Android Studio (Recommended)
1. Open **Android Studio**.
2. Select **Open** and choose this directory: `/home/sglenn21/Downloads/medicine`.
3. Connect your wife's Android phone via USB (or an emulator) with Developer Options / USB Debugging enabled.
4. Click the green **Run ▶** button to install directly on her device.

### Option B: Build APK via Command Line
Run in this directory:
```bash
./gradlew assembleDebug
```
The output APK will be generated at:
```
app/build/outputs/apk/debug/app-debug.apk
```
You can send this APK directly to her phone (via Quick Share, WhatsApp, Google Drive, or `adb install app-debug.apk`).

---

## 🛠️ Architecture & Tech Stack
- **Language**: Kotlin 2.x
- **UI Toolkit**: Jetpack Compose + Material 3 (Dynamic Color & High Contrast support)
- **Database**: Room Database (SQLite with Flow & Coroutines)
- **Background Alarms**: `AlarmManager` (`setExactAndAllowWhileIdle`) + `BroadcastReceiver`
- **Architecture**: MVVM with Repository Pattern
