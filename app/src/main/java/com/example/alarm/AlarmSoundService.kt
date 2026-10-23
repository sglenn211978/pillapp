package com.example.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveAlarmState(
    val isRinging: Boolean = false,
    val medicationId: Long = -1L,
    val medicationName: String = "",
    val dosage: String = "",
    val scheduledDate: String = "",
    val scheduledTime: String = "",
    val instructions: String = "",
    val colorHex: Long = 0xFF00796B
)

class AlarmSoundService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var autoSilenceJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: return START_NOT_STICKY

        when (action) {
            ACTION_START_ALARM -> {
                val medId = intent.getLongExtra(EXTRA_MED_ID, -1L)
                val medName = intent.getStringExtra(EXTRA_MED_NAME) ?: "Medication Due"
                val dosage = intent.getStringExtra(EXTRA_DOSAGE) ?: ""
                val schedDate = intent.getStringExtra(EXTRA_SCHED_DATE) ?: ""
                val schedTime = intent.getStringExtra(EXTRA_SCHED_TIME) ?: ""
                val instructions = intent.getStringExtra(EXTRA_INSTRUCTIONS) ?: ""
                val colorHex = intent.getLongExtra(EXTRA_COLOR_HEX, 0xFF00796B)
                val customRingtone = intent.getStringExtra(EXTRA_RINGTONE_URI)

                startAlarm(
                    medId = medId,
                    medName = medName,
                    dosage = dosage,
                    schedDate = schedDate,
                    schedTime = schedTime,
                    instructions = instructions,
                    colorHex = colorHex,
                    customRingtoneUri = customRingtone
                )
            }
            ACTION_STOP_ALARM -> {
                stopAlarm()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    private fun startAlarm(
        medId: Long,
        medName: String,
        dosage: String,
        schedDate: String,
        schedTime: String,
        instructions: String,
        colorHex: Long,
        customRingtoneUri: String?
    ) {
        // Update live observable state for in-app emergency stop banner
        _activeAlarmState.value = ActiveAlarmState(
            isRinging = true,
            medicationId = medId,
            medicationName = medName,
            dosage = dosage,
            scheduledDate = schedDate,
            scheduledTime = schedTime,
            instructions = instructions,
            colorHex = colorHex
        )

        // Post foreground notification with full-screen intent & heads-up actions
        val notification = AlarmNotificationHelper.buildAlarmNotification(
            context = this,
            medicationId = medId,
            medicationName = medName,
            dosage = dosage,
            scheduledDate = schedDate,
            scheduledTime = schedTime,
            instructions = instructions
        )
        startForeground(NOTIFICATION_ID, notification)

        // 1. Audio Playback via MediaPlayer with USAGE_ALARM
        playLoopingAudio(customRingtoneUri)

        // 2. Rhythmic repeating vibration pattern
        startVibration()

        // 3. Failsafe 3-minute auto-silence timeout to prevent infinite blaring
        autoSilenceJob?.cancel()
        autoSilenceJob = serviceScope.launch {
            delay(3 * 60 * 1000L) // 3 minutes
            AlarmNotificationHelper.showMissedNotification(this@AlarmSoundService, medName, dosage)
            stopAlarm()
            stopSelf()
        }
    }

    private fun playLoopingAudio(customRingtoneUri: String?) {
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()

            val soundUri: Uri = if (!customRingtoneUri.isNullOrBlank()) {
                Uri.parse(customRingtoneUri)
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                    ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            }

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, soundUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startVibration() {
        try {
            val pattern = longArrayOf(0, 800, 400, 800, 400, 1200, 600)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0)) // 0 = loop forever
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun stopAlarm() {
        autoSilenceJob?.cancel()
        autoSilenceJob = null

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        _activeAlarmState.value = ActiveAlarmState(isRinging = false)
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    override fun onDestroy() {
        stopAlarm()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val NOTIFICATION_ID = 9001
        const val ACTION_START_ALARM = "com.example.action.START_ALARM"
        const val ACTION_STOP_ALARM = "com.example.action.STOP_ALARM"

        const val EXTRA_MED_ID = "extra_med_id"
        const val EXTRA_MED_NAME = "extra_med_name"
        const val EXTRA_DOSAGE = "extra_dosage"
        const val EXTRA_SCHED_DATE = "extra_sched_date"
        const val EXTRA_SCHED_TIME = "extra_sched_time"
        const val EXTRA_INSTRUCTIONS = "extra_instructions"
        const val EXTRA_COLOR_HEX = "extra_color_hex"
        const val EXTRA_RINGTONE_URI = "extra_ringtone_uri"

        private val _activeAlarmState = MutableStateFlow(ActiveAlarmState())
        val activeAlarmState: StateFlow<ActiveAlarmState> = _activeAlarmState.asStateFlow()

        fun stop(context: Context) {
            val intent = Intent(context, AlarmSoundService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }
}
