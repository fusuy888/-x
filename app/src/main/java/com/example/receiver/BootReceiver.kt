package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.SettingsRepository
import com.example.util.AlarmScheduler

class BootReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.d(TAG, "Received action: $action")
        
        if (action == Intent.ACTION_BOOT_COMPLETED || 
            action == Intent.ACTION_MY_PACKAGE_REPLACED || 
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            val settings = SettingsRepository(context)
            if (settings.isEnabled) {
                Log.d(TAG, "Rescheduling alarm on system reboot/app update...")
                AlarmScheduler.scheduleNextAlarm(context)
            } else {
                Log.d(TAG, "Notification loop is disabled. Boot scheduler skipped.")
            }
        }
    }
}
