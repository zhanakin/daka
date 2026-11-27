package com.example.autoclockin

import android.content.Context

object Prefs {
    private const val NAME = "auto_clockin_prefs"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_LAST_RESULT = "last_result"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun isEnabled(ctx: Context): Boolean = sp(ctx).getBoolean(KEY_ENABLED, false)

    fun setEnabled(ctx: Context, value: Boolean) {
        sp(ctx).edit().putBoolean(KEY_ENABLED, value).apply()
    }

    fun hour(ctx: Context): Int = sp(ctx).getInt(KEY_HOUR, 9)

    fun minute(ctx: Context): Int = sp(ctx).getInt(KEY_MINUTE, 0)

    fun setTime(ctx: Context, hour: Int, minute: Int) {
        sp(ctx).edit().putInt(KEY_HOUR, hour).putInt(KEY_MINUTE, minute).apply()
    }

    fun lastResult(ctx: Context): String =
        sp(ctx).getString(KEY_LAST_RESULT, "尚未执行") ?: "尚未执行"

    fun setLastResult(ctx: Context, value: String) {
        sp(ctx).edit().putString(KEY_LAST_RESULT, value).apply()
    }
}
