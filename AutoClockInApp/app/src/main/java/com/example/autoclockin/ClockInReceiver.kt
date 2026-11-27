package com.example.autoclockin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager

class ClockInReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != AlarmScheduler.ACTION_CLOCK_IN) return

        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "AutoClockIn:alarm")
        wakeLock.acquire(60_000L)

        val trigger = Intent(AutoClockInService.ACTION_TRIGGER).setPackage(context.packageName)
        context.sendBroadcast(trigger)
    }
}
