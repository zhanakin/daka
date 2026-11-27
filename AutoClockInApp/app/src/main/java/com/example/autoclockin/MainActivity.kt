package com.example.autoclockin

import android.app.Activity
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.TimePicker
import android.widget.Toast

class MainActivity : Activity() {

    private lateinit var timePicker: TimePicker
    private lateinit var enableBox: CheckBox
    private lateinit var statusText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        timePicker = findViewById(R.id.timePicker)
        enableBox = findViewById(R.id.enableBox)
        statusText = findViewById(R.id.statusText)

        timePicker.hour = Prefs.hour(this)
        timePicker.minute = Prefs.minute(this)
        enableBox.isChecked = Prefs.isEnabled(this)
        refreshStatus()

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnExactAlarm).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
                if (!alarmManager.canScheduleExactAlarms()) {
                    startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                } else {
                    toast("精确闹钟权限已开启")
                }
            } else {
                toast("当前系统无需单独授权精确闹钟")
            }
        }

        findViewById<Button>(R.id.btnTest).setOnClickListener {
            if (!isAccessibilityEnabled()) {
                toast("请先在无障碍设置中开启本应用")
                return@setOnClickListener
            }
            val trigger = Intent(AutoClockInService.ACTION_TRIGGER).setPackage(packageName)
            sendBroadcast(trigger)
            toast("已触发一次测试，请观察企业微信是否被打开并点击")
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            Prefs.setTime(this, timePicker.hour, timePicker.minute)
            if (enableBox.isChecked) checkExactAlarmAndSchedule()
            toast("时间已保存")
            refreshStatus()
        }

        enableBox.setOnCheckedChangeListener { _, checked ->
            Prefs.setEnabled(this, checked)
            Prefs.setTime(this, timePicker.hour, timePicker.minute)
            if (checked) {
                if (!isAccessibilityEnabled()) {
                    toast("请先在无障碍设置中开启本应用，否则无法自动点击")
                }
                checkExactAlarmAndSchedule()
            } else {
                AlarmScheduler.cancel(this)
                toast("已关闭每日自动打卡")
            }
            refreshStatus()
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun checkExactAlarmAndSchedule() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                toast("请先授予精确闹钟权限")
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                return
            }
        }
        AlarmScheduler.schedule(this, timePicker.hour, timePicker.minute)
        val minute = String.format("%02d", timePicker.minute)
        toast("已设定每日 ${timePicker.hour}:$minute 自动打卡")
    }

    private fun isAccessibilityEnabled(): Boolean {
        val enabled = Settings.Secure.getInt(
            contentResolver,
            Settings.Secure.ACCESSIBILITY_ENABLED,
            0
        ) == 1
        if (!enabled) return false

        val services = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val serviceName = AutoClockInService::class.java.name
        return services.split(":").any { it.contains(packageName) && it.contains(serviceName) }
    }

    private fun refreshStatus() {
        val accessibility = if (isAccessibilityEnabled()) "已开启" else "未开启"
        val exactAlarm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (alarmManager.canScheduleExactAlarms()) "已授权" else "未授权"
        } else {
            "无需授权"
        }
        statusText.text = "无障碍服务：$accessibility\n精确闹钟：$exactAlarm\n上次结果：${Prefs.lastResult(this)}"
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
