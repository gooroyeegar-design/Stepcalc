package com.gooroyeegar.stepcalc

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        Log.d("StepCalcBoot", "boot/app-replaced receiver: $action")
        val prefs = context.getSharedPreferences("stepcalc", 0)
        val ready = prefs.getBoolean("setup", false)
        val permission = ContextCompat.checkSelfPermission(
            context, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED

        if (!ready || !permission) {
            Log.d("StepCalcBoot", "tracking not eligible at boot")
            return
        }

        try {
            ContextCompat.startForegroundService(context, Intent(context, StepService::class.java))
            Log.d("StepCalcBoot", "boot service start requested")
        } catch (t: Throwable) {
            Log.e("StepCalcBoot", "boot service start rejected", t)
        }
    }
}
