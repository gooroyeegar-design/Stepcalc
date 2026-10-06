package com.gooroyeegar.stepcalc

import android.app.Application
import android.os.Build
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StepCalcApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "app start")
        installCrashHandler()
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                val writer = StringWriter()
                throwable.printStackTrace(PrintWriter(writer))
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z", Locale.US).format(Date())
                val report = buildString {
                    appendLine("StepCalc crash report")
                    appendLine("Timestamp: $timestamp")
                    appendLine("App version: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                    appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                    appendLine("Thread: ${thread.name}")
                    appendLine()
                    append(writer.toString())
                }
                File(filesDir, CRASH_FILE).writeText(report, Charsets.UTF_8)
                Log.e(TAG, "uncaught crash saved to $CRASH_FILE")
            } catch (saveError: Throwable) {
                Log.e(TAG, "could not save crash report", saveError)
            } finally {
                previous?.uncaughtException(thread, throwable)
            }
        }
    }

    companion object {
        const val CRASH_FILE = "stepcalc_last_crash.txt"
        private const val TAG = "StepCalcBoot"
    }
}
