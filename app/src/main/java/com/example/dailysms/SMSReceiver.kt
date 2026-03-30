package com.example.dailysms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.widget.Toast
import java.util.*

class SMSReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("SMSPrefs", Context.MODE_PRIVATE)
        val phone = prefs.getString("phone", "") ?: ""
        val message = prefs.getString("message", "") ?: ""
        val hour = prefs.getInt("hour", 9)
        val minute = prefs.getInt("minute", 0)

        if (phone.isNotEmpty() && message.isNotEmpty()) {
            sendSMS(context, phone, message)
            reschedule(context, hour, minute)
        }
    }

    private fun sendSMS(context: Context, phone: String, message: String) {
        try {
            val smsManager = SmsManager.getDefault()
            val parts = smsManager.divideMessage(message)
            
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(phone, null, message, null, null)
            }
            Toast.makeText(context, "📨 تم إرسال الرسالة اليومية", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(context, "❌ فشل الإرسال", Toast.LENGTH_LONG).show()
        }
    }

    private fun reschedule(context: Context, hour: Int, minute: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val intent = Intent(context, SMSReceiver::class.java)
        val pendingIntent = android.app.PendingIntent.getBroadcast(
            context, 0, intent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 1)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
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
