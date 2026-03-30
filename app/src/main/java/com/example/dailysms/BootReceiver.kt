package com.example.dailysms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val prefs = context.getSharedPreferences("SMSPrefs", Context.MODE_PRIVATE)
            if (prefs.getBoolean("scheduled", false)) {
                val hour = prefs.getInt("hour", 9)
                val minute = prefs.getInt("minute", 0)
                
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
                val alarmIntent = Intent(context, SMSReceiver::class.java)
                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context, 0, alarmIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val calendar = java.util.Calendar.getInstance().apply {
                    set(java.util.Calendar.HOUR_OF_DAY, hour)
                    set(java.util.Calendar.MINUTE, minute)
                    set(java.util.Calendar.SECOND, 0)
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(java.util.Calendar.DAY_OF_YEAR, 1)
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        calendar.timeInMillis,
                        pendingIntent
                    )
                }
            }
        }
    }
}
