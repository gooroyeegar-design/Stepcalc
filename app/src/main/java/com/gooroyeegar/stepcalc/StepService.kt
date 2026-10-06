package com.gooroyeegar.stepcalc

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.hardware.*
import android.os.*
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

class StepService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private val prefs by lazy { getSharedPreferences("stepcalc", MODE_PRIVATE) }
    private val channelId = "stepcalc_live"
    private var base = -1f
    private var detectorMode = false

    override fun onCreate() {
        super.onCreate()
        createChannels()
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        val counter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (counter != null) {
            detectorMode = false
            prefs.edit().putBoolean("sensor_available", true).apply()
            sensorManager.registerListener(this, counter, Sensor.SENSOR_DELAY_NORMAL)
        } else {
            val detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            if (detector != null) {
                detectorMode = true
                prefs.edit().putBoolean("sensor_available", true).apply()
                sensorManager.registerListener(this, detector, Sensor.SENSOR_DELAY_NORMAL)
            } else {
                prefs.edit().putBoolean("sensor_available", false).apply()
            }
        }
        scheduleDailyQuote()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (Build.VERSION.SDK_INT >= 29 &&
            checkSelfPermission(Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED
        ) {
            stopSelf()
            return START_NOT_STICKY
        }
        startForeground(NOTIFICATION_ID, notification(prefs.getInt("steps", 0)))
        return START_STICKY
    }

    override fun onSensorChanged(e: SensorEvent) {
        val today = dayKey()

        if (detectorMode) {
            val savedDay = prefs.getString("day", today)
            val current = if (savedDay == today) prefs.getInt("steps", 0) else 0
            val steps = current + maxOf(1, e.values.firstOrNull()?.toInt() ?: 1)
            prefs.edit()
                .putString("day", today)
                .putInt("steps", steps)
                .apply()
            update(steps)
            return
        }

        val total = e.values.firstOrNull() ?: return
        val savedDay = prefs.getString("day", null)
        var lastTotal = prefs.getFloat("sensor_last", -1f)
        var steps = prefs.getInt("steps", 0).coerceAtLeast(0)

        if (savedDay != today) {
            base = total
            lastTotal = total
            steps = 0
        } else if (lastTotal < 0f) {
            base = prefs.getFloat("sensor_base", total)
            lastTotal = prefs.getFloat("sensor_last", base)
        }

        // TYPE_STEP_COUNTER is defined as steps since the last device reboot.
        // A reboot resets the sensor value. Keep today's accumulated steps,
        // then start counting again from the new post-reboot sensor value.
        if (lastTotal >= 0f && total < lastTotal) {
            base = total
            lastTotal = total
        }

        val delta = maxOf(0, (total - lastTotal).toInt())
        steps += delta
        lastTotal = total

        prefs.edit()
            .putInt("steps", steps)
            .putFloat("sensor_base", base)
            .putFloat("sensor_last", lastTotal)
            .putString("day", today)
            .putString("timezone", ZoneId.systemDefault().id)
            .apply()
        update(steps)
    }

    private fun update(steps: Int) {
        val weight = prefs.getFloat("weight", 60f)
        val height = prefs.getFloat("height", 165f)
        val distanceKm = steps * (height * 0.413f / 100000f)
        val kcal = distanceKm * weight * 0.75f
        val kg = kcal / 7700f
        val goal = prefs.getInt("goal", 10000)
        val percent = if (goal > 0) (steps.toFloat() / goal * 100f).coerceIn(0f, 100f) else 0f
        prefs.edit().putFloat("kcal", kcal).putFloat("kg", kg).apply()
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, notification(steps, kcal, percent))
    }

    private fun notification(steps: Int, kcal: Float = 0f, percent: Float = 0f): Notification {
        val goal = prefs.getInt("goal", 10000)
        val displayKcal = if (kcal > 0f) kcal else {
            val weight = prefs.getFloat("weight", 60f)
            val height = prefs.getFloat("height", 165f)
            steps * (height * 0.413f / 100000f) * weight * 0.75f
        }
        val goalPercent = if (percent > 0f) percent else {
            if (goal > 0) (steps.toFloat() / goal * 100f).coerceIn(0f, 100f) else 0f
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            this, 701, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val small = RemoteViews(packageName, R.layout.notification_small).apply {
            setTextViewText(R.id.notification_stats, getString(R.string.notification_stats, steps, displayKcal))
            setTextViewText(R.id.notification_goal, getString(R.string.notification_goal, goalPercent, steps, goal))
            setProgressBar(R.id.notification_progress, 100, goalPercent.toInt(), false)
            setOnClickPendingIntent(R.id.notification_open, openPending)
        }
        val large = RemoteViews(packageName, R.layout.notification_large).apply {
            setTextViewText(R.id.notification_stats, getString(R.string.notification_stats_large, steps, displayKcal))
            setTextViewText(R.id.notification_goal, getString(R.string.notification_goal_large, goalPercent, goal))
            setProgressBar(R.id.notification_progress, 100, goalPercent.toInt(), false)
            setOnClickPendingIntent(R.id.notification_open, openPending)
        }

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_steps)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(getString(R.string.notification_stats_large, steps, displayKcal))
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(small)
            .setCustomBigContentView(large)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setShowWhen(false)
            .setColor(0xFFFF8A3D.toInt())
            .build()
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(
                channelId,
                getString(R.string.notification_channel_live),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_live_description)
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                "daily",
                getString(R.string.notification_channel_daily),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        )
    }

    private fun scheduleDailyQuote() {
        val am = getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            this, 99, Intent(this, QuoteReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val c = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 9)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.timeInMillis, AlarmManager.INTERVAL_DAY, intent)
    }

    private fun dayKey(): String =
        LocalDate.now(ZoneId.systemDefault()).toString() + "|" + ZoneId.systemDefault().id

    override fun onDestroy() {
        if (::sensorManager.isInitialized) sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val NOTIFICATION_ID = 7
    }
}
