# PillPulse 💊
### Lock-Screen-Ready Medication Adherence Protocol (Pillo-Inspired)

A reliable, lock-screen-ready medication reminder app built with Kotlin & Jetpack Compose. PillPulse enforces medication adherence through **persistent, unignorable full-screen alarms, continuous looping audio alerts, slide-to-confirm dosage verification**, and a comprehensive daily compliance dashboard.

---

## 🌟 Core Architecture & Key Features

### 1. 🚨 Zero-Ignorance Lock-Screen Alarm (`AlarmAlertActivity.kt`)
- **Full-Screen Display Over Lock Screen**: Automatically wakes display, turns screen on, and bypasses the keyguard (`setShowWhenLocked(true)`, `setTurnScreenOn(true)`).
- **Physical Slide-to-Confirm Gesture**: Accidental dismissal prevention via `SlideToConfirm` drag slider with haptic feedback to prevent groggy or accidental taps.
- **Pulsing Visual Alert**: High-visibility clinical card displaying pill shape, color badge, dose strength, and intake instructions (e.g., *"Take with food and a full glass of water"*).
- **Snooze & Skip Protocols**: Quick action buttons for Snooze (10m), Snooze (15m), and Skip with clinical reason.

### 2. 🔊 Continuous Looping Audio & Haptic Service (`AlarmSoundService.kt`)
- Dedicated foreground alarm service utilizing `AudioAttributes.USAGE_ALARM` with looping playback and rhythmic vibration patterns.
- Custom ringtone selection (System Alarm, Gentle Chime, Clinical Beep, Urgent Siren).
- **3-Minute Safety Auto-Silence**: Automatically silences audio and posts a "Missed Dose" notification after 3 minutes to prevent infinite blaring.
- **In-App Emergency Silence Banner**: Real-time banner on the dashboard allowing 1-tap silence/take controls if the alarm is currently ringing.

### 3. 📊 Daily Compliance Dashboard (`DashboardScreen.kt`)
- **Compliance Ring Gauge**: Real-time daily adherence percentage (e.g. *80% Adherence Today*) and active streak counter.
- **One-Tap Alarm Simulator**: Immediate **"Test Alarm (5s)"** button to test the lockscreen wakeup and looping audio without waiting.
- **Pill Inventory Tracker**: Bottle count with low-supply refill warnings (e.g. *5 pills left - Refill soon*).
- **Chronological Timeline**: Cards color-coded by medical status (Emerald for Taken, Amber for Snoozed, Vibrant Coral Red for Urgent/Due).

### 4. 📚 50+ Clinical Medication Database (`MedicationDatabaseScreen.kt`)
- Pre-seeded database covering 9 clinical specialties:
  - Cardiology & Blood Pressure (Lisinopril, Amlodipine, Losartan, Metoprolol, HCTZ, Carvedilol)
  - Cholesterol & Lipids (Atorvastatin, Rosuvastatin, Simvastatin)
  - Diabetes & Endocrine (Metformin, Levothyroxine, Glipizide, Jardiance)
  - Mental Health & Neurology (Sertraline, Lexapro, Wellbutrin, Cymbalta, Gabapentin)
  - Gastrointestinal (Omeprazole, Pantoprazole, Pepcid, Zofran)
  - Respiratory & Allergy (Albuterol, Flonase, Singulair, Zyrtec)
  - Antibiotics (Amoxicillin, Azithromycin, Keflex, Doxycycline)
  - Pain & Inflammation (Ibuprofen, Tylenol, Aleve, Mobic)
  - Vitamins & Supplements (Vitamin D3, B12, Iron, Omega-3, Magnesium)
- 1-tap **"Add to My Medications"** automatically configures dosages and food/intake guidance.

### 5. 🔒 100% Offline & Private
- Local Room SQLite database.
- Zero login accounts, zero telemetry, zero ads.
- Export adherence log report for physician appointments in 1 tap.

---

## 🚀 How to Run & Build

### In Android Studio:
1. Open Android Studio -> **Open** -> Select `/home/sglenn21/Downloads/medicine`.
2. Connect your Android phone via USB (with Developer Mode & USB Debugging enabled).
3. Click **Run ▶**.

### Build APK via CLI:
```bash
./gradlew assembleDebug
```
Output APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`
