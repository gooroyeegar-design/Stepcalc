package com.gooroyeegar.stepcalc
import android.content.*
import androidx.core.content.ContextCompat
class BootReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED && context.getSharedPreferences("stepcalc",0).getBoolean("setup",false))
            ContextCompat.startForegroundService(context, Intent(context, StepService::class.java))
    }
}
