package com.example.autoclockin

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Path
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast

class AutoClockInService : AccessibilityService() {

    companion object {
        const val ACTION_TRIGGER = "com.example.autoclockin.ACTION_TRIGGER"
        private const val TAG = "AutoClockIn"
        private const val WECOM_PACKAGE = "com.tencent.wework"
        private const val MAX_ATTEMPTS = 4
        private const val ATTEMPT_DELAY_MS = 1500L
    }

    private val handler = Handler(Looper.getMainLooper())
    private var running = false
    private var attemptCount = 0

    private val triggerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_TRIGGER) {
                startClockInFlow()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "无障碍服务已连接")
        val filter = IntentFilter(ACTION_TRIGGER)
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(triggerReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(triggerReceiver, filter)
        }
    }

    private fun startClockInFlow() {
        if (running) {
            Log.i(TAG, "已有打卡流程在执行，忽略本次触发")
            return
        }
        running = true
        attemptCount = 0
        bringWecomToFront()
        handler.postDelayed({ tryClockIn() }, 2000L)
    }

    private fun bringWecomToFront() {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(WECOM_PACKAGE)
            if (launchIntent == null) {
                Log.e(TAG, "未检测到企业微信，请先安装并登录")
                return
            }
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e(TAG, "拉起企业微信失败", e)
        }
    }

    private fun tryClockIn() {
        val root = rootInActiveWindow
        if (root == null) {
            Log.w(TAG, "当前窗口根节点为空")
            retryOrFinish(false)
            return
        }

        val target = findClockInNode(root)
        if (target == null) {
            Log.w(TAG, "未找到打卡按钮，第 ${attemptCount + 1} 次")
            retryOrFinish(false)
            return
        }

        val bounds = Rect()
        target.getBoundsInScreen(bounds)
        val x = bounds.centerX()
        val y = bounds.centerY()
        Log.i(TAG, "找到打卡按钮：text=${target.text}, desc=${target.contentDescription}, bounds=$bounds")

        val clicked = if (target.isClickable) {
            target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
        } else {
            performGestureClick(x.toFloat(), y.toFloat())
        }

        val result = if (clicked) "打卡点击成功（$x, $y）" else "点击指令未生效"
        Prefs.setLastResult(this, result)
        Log.i(TAG, result)
        Toast.makeText(this, result, Toast.LENGTH_SHORT).show()
        running = false
    }

    private fun retryOrFinish(success: Boolean) {
        if (success) {
            running = false
            return
        }
        attemptCount++
        if (attemptCount >= MAX_ATTEMPTS) {
            val msg = "未找到打卡按钮，请确认企业微信停留在打卡页面"
            Prefs.setLastResult(this, msg)
            Log.w(TAG, msg)
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
            running = false
            return
        }
        handler.postDelayed({ tryClockIn() }, ATTEMPT_DELAY_MS)
    }

    private fun findClockInNode(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            val text = node.text?.toString().orEmpty()
            val desc = node.contentDescription?.toString().orEmpty()
            if ((text.contains("打卡") || desc.contains("打卡")) &&
                (node.isClickable || node.isEnabled)
            ) {
                return node
            }
            for (i in 0 until node.childCount) {
                node.getChild(i)?.let { queue.add(it) }
            }
        }
        return null
    }

    private fun performGestureClick(x: Float, y: Float): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val path = Path().apply {
            moveTo(x, y)
            lineTo(x, y)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, 100))
            .build()
        return dispatchGesture(gesture, object : GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                super.onCompleted(gestureDescription)
                Log.i(TAG, "手势点击完成")
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                super.onCancelled(gestureDescription)
                Log.w(TAG, "手势点击被取消")
            }
        }, null)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 触发依赖 AlarmManager 广播，事件回调留空
    }

    override fun onInterrupt() {
        Log.w(TAG, "无障碍服务被中断")
        running = false
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(triggerReceiver)
        } catch (_: Exception) {
            // 服务未注册时忽略
        }
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
