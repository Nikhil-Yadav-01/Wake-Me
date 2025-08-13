package com.nikhil.wakeme.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings

object BatteryOptHelper {
    fun needsIgnoreBatteryOptimizations(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            return !pm.isIgnoringBatteryOptimizations(context.packageName)
        }
        return false
    }

    fun buildRequestIntent(): Intent {
        return Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
    }
}
