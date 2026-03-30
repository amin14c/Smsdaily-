package com.example.dailysms

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.SmsManager
import android.widget.Button
import android.widget.EditText
import android.widget.TimePicker
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {
    private val PERMISSIONS_REQUEST_CODE = 100
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val phoneInput = findViewById<EditText>(R.id.phoneInput)
        val messageInput = findViewById<EditText>(R.id.messageInput)
        val timePicker = findViewById<TimePicker>(R.id.timePicker)
        val scheduleButton = findViewById<Button>(R.id.scheduleButton)
        val sendNowButton = findViewById<Button>(R.id.sendNowButton)

        checkPermissions()

        scheduleButton.setOnClickListener {
            val phone = phoneInput.text.toString()
            val message = messageInput.text.toString()
            
            if (phone.isEmpty() || message.isEmpty()) {
                Toast.makeText(this, "أدخل الرقم والرسالة", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveAndSchedule(phone, message, timePicker.hour, timePicker.minute)
            Toast.makeText(this, "✅ تم الجدولة يومياً الساعة ${timePicker.hour}:${timePicker.minute}", Toast.LENGTH_LONG).show()
        }

        sendNowButton.setOnClickListener {
            sendSMSNow(phoneInput.text.toString(), messageInput.text.toString())
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(Manifest.permission.SEND_SMS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.SCHEDULE_EXACT_ALARM)
        }

        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
            }
        }
    }

    private fun saveAndSchedule(phone: String, message: String, hour: Int, minute: Int) {
        getSharedPreferences("SMSPrefs", Context.MODE_PRIVATE).edit().apply {
            putString("phone", phone)
            putString("message", message)
            putInt("hour", hour)
            putInt("minute", minute)
            putBoolean("scheduled", true)
            apply()
        }

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, SMSReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    private fun sendSMSNow(phone: String, message: String) {
        if (phone.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "أدخل الرقم والرسالة أولاً", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            SmsManager.getDefault().sendTextMessage(phone, null, message, null, null)
            Toast.makeText(this, "📤 تم الإرسال!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "❌ فشل: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
