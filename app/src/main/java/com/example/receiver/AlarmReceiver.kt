package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.NotificationDatabase
import com.example.data.NotificationHistory
import com.example.data.SettingsRepository
import com.example.util.AlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "AlarmReceiver"
        private const val CHANNEL_ID = "interval_alert_channel"
        private const val NOTIFICATION_ID = 9876
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Alarm triggered!")

        val settings = SettingsRepository(context)
        if (!settings.isEnabled) {
            Log.d(TAG, "Notification is disabled in settings. Skipping.")
            return
        }

        val currentTime = System.currentTimeMillis()

        // 1. Check DND
        val inDnd = settings.isDndEnabled && AlarmScheduler.isTimeInDnd(
            currentTime,
            settings.dndStartTime,
            settings.dndEndTime
        )

        if (inDnd) {
            Log.d(TAG, "Currently in DND period. Skipping notification.")
            // Still schedule the next alarm in the sequence
            AlarmScheduler.scheduleNextAlarm(context)
            return
        }

        val text = settings.notificationText
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val formattedTime = sdf.format(Date(currentTime))

        // 2. Insert into Room Database (Local History)
        val database = NotificationDatabase.getDatabase(context)
        val historyDao = database.notificationDao
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                historyDao.insertHistory(
                    NotificationHistory(
                        text = text,
                        timestamp = currentTime,
                        formattedTime = formattedTime
                    )
                )
                Log.d(TAG, "Logged to local database history successfully.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to log to database", e)
            }
        }

        // 3. Show Heads-up notification
        showNotification(context, text)

        // 4. Schedule next alarm
        AlarmScheduler.scheduleNextAlarm(context)
    }

    private fun showNotification(context: Context, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        
        // Setup ringtone sound
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Create channel for Oreo+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            var channel = notificationManager.getNotificationChannel(CHANNEL_ID)
            if (channel == null) {
                val name = "盯盘提醒通知"
                val descriptionText = "用于循环闹钟和悬浮提示音通知"
                val importance = NotificationManager.IMPORTANCE_HIGH
                channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                    enableLights(true)
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 500, 250, 500)
                    
                    // Set sound attributes
                    val audioAttributes = AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    setSound(soundUri, audioAttributes)
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        // Setup notification open intent
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("notification_text", message)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            1234, // Unique request code to avoid overlapping caching
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Notification builder with high importance to trigger Heads-up
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Standard status bar icon
            .setContentTitle("盯盘提醒")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(longArrayOf(0, 500, 250, 500))
            .setContentIntent(openPendingIntent)
            .setFullScreenIntent(openPendingIntent, true) // Required to force full heads-up banner on some overlays

        notificationManager.notify(NOTIFICATION_ID, builder.build())

        // Trigger safe vibration for newer devices as well
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                val vibrator = vibratorManager?.defaultVibrator
                vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500), -1))
            } else {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(longArrayOf(0, 500, 250, 500), -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate", e)
        }
    }
}
