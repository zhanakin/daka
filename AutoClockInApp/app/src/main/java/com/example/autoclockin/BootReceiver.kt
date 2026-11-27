package com.example.autoclockin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.isEnabled(context)) {
            AlarmScheduler.schedule(context, Prefs.hour(context), Prefs.minute(context))
        }
    }
}
