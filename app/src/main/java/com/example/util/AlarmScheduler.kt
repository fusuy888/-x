package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.SettingsRepository
import com.example.receiver.AlarmReceiver

object AlarmScheduler {
    private const val TAG = "AlarmScheduler"
    private const val REQUEST_CODE = 4567

    fun scheduleNextAlarm(context: Context) {
        val settings = SettingsRepository(context)
        if (!settings.isEnabled) {
            cancelAlarm(context)
            return
        }

        val now = System.currentTimeMillis()
        val nextTime = calculateNextTriggerTime(now, settings.startTime, settings.intervalMinutes)
        settings.nextTriggerTime = nextTime

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTime,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled exact alarm for: $nextTime")
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextTime,
                        pendingIntent
                    )
                    Log.d(TAG, "Scheduled inexact alarm (exact permission missing) for: $nextTime")
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTime,
                    pendingIntent
                )
                Log.d(TAG, "Scheduled exact alarm on Pre-S for: $nextTime")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule alarm", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextTime,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Critical: Fallback alarm schedule failed", ex)
            }
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Alarm cancelled successfully.")
        }
    }

    fun calculateNextTriggerTime(now: Long, startTimeStr: String, intervalMinutes: Int): Long {
        val intervalMs = intervalMinutes * 60 * 1000L
        if (intervalMs <= 0) return now + 60000L

        val parts = startTimeStr.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val second = parts.getOrNull(2)?.toIntOrNull() ?: 0

        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = now
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, second)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        val baseMs = calendar.timeInMillis
        val diff = now - baseMs

        return if (diff >= 0) {
            val intervalsNeeded = (diff / intervalMs) + 1
            baseMs + intervalsNeeded * intervalMs
        } else {
            val diffAbs = -diff
            val intervalsToSubtract = diffAbs / intervalMs
            var nextTriggerMs = baseMs - intervalsToSubtract * intervalMs
            while (nextTriggerMs <= now) {
                nextTriggerMs += intervalMs
            }
            nextTriggerMs
        }
    }

    fun isTimeInDnd(timestamp: Long, dndStart: String, dndEnd: String): Boolean {
        val startParts = dndStart.split(":")
        val startHour = startParts.getOrNull(0)?.toIntOrNull() ?: 0
        val startMin = startParts.getOrNull(1)?.toIntOrNull() ?: 0

        val endParts = dndEnd.split(":")
        val endHour = endParts.getOrNull(0)?.toIntOrNull() ?: 0
        val endMin = endParts.getOrNull(1)?.toIntOrNull() ?: 0

        val calendar = java.util.Calendar.getInstance().apply {
            timeInMillis = timestamp
        }
        val currentHour = calendar.get(java.util.Calendar.HOUR_OF_DAY)
        val currentMin = calendar.get(java.util.Calendar.MINUTE)

        val currentMinutes = currentHour * 60 + currentMin
        val startMinutes = startHour * 60 + startMin
        val endMinutes = endHour * 60 + endMin

        return if (startMinutes <= endMinutes) {
            currentMinutes in startMinutes..endMinutes
        } else {
            currentMinutes >= startMinutes || currentMinutes <= endMinutes
        }
    }
}
