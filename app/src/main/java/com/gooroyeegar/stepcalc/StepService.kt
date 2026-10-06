package com.gooroyeegar.stepcalc

import android.app.*
import android.content.*
import android.hardware.*
import android.os.*
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class StepService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private val prefs by lazy { getSharedPreferences("stepcalc", MODE_PRIVATE) }
    private val channelId = "stepcalc_live"
    private var base = -1f
    private var detectorMode = false

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(NOTIFICATION_ID, notification(prefs.getInt("steps", 0)))
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager

        val counter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (counter != null) {
            detectorMode = false
            sensorManager.registerListener(this, counter, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            val detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            if (detector != null) {
                detectorMode = true
                sensorManager.registerListener(this, detector, SensorManager.SENSOR_DELAY_NORMAL)
            }
        }
        scheduleDailyQuote()
    }

    override fun onSensorChanged(e: SensorEvent) {
        val today = dayKey()

        if (detectorMode) {
            val savedDay = prefs.getString("day", today)
            val current = if (savedDay == today) prefs.getInt("steps", 0) else 0
            val steps = current + maxOf(1, e.values.firstOrNull()?.toInt() ?: 1)
            prefs.edit().putString("day", today).putInt("steps", steps).apply()
            update(steps)
            return
        }

        val total = e.values[0]
        val savedDay = prefs.getString("day", null)
        if (savedDay != today) {
            base = total
            prefs.edit().putString("day", today).putFloat("sensor_base", base).putInt("steps", 0).apply()
        } else if (base < 0f) {
            base = prefs.getFloat("sensor_base", total)
        }

        if (total < base) {
            base = total
            prefs.edit().putFloat("sensor_base", base).putInt("steps", 0).apply()
        }

        val steps = maxOf(0, (total - base).toInt())
        prefs.edit().putInt("steps", steps).putFloat("sensor_base", base).putString("day", today).apply()
        update(steps)
    }

    private fun update(steps: Int) {
        val weight = prefs.getFloat("weight", 60f)
        val height = prefs.getFloat("height", 165f)
        val distanceKm = steps * (height * 0.413f / 100000f)
        val kcal = distanceKm * weight * 0.75f
        val kg = kcal / 7700f
        val percent = prefs.getInt("goal", 10000).let { goal ->
            if (goal > 0) (steps.toFloat() / goal * 100f).coerceIn(0f, 100f) else 0f
        }
        prefs.edit().putFloat("kcal", kcal).putFloat("kg", kg).apply()
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(steps, kcal, percent))
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
            setTextViewText(R.id.notification_stats, String.format(Locale.US, "👟 %,d    🔥 %.1f", steps, displayKcal))
            setTextViewText(R.id.notification_goal, String.format(Locale.US, "🎯 %.0f%%  •  %,d / %,d steps", goalPercent, steps, goal))
            setProgressBar(R.id.notification_progress, 100, goalPercent.toInt(), false)
            setOnClickPendingIntent(R.id.notification_open, openPending)
        }
        val large = RemoteViews(packageName, R.layout.notification_large).apply {
            setTextViewText(R.id.notification_stats, String.format(Locale.US, "👟 %,d    🔥 %.1f kcal", steps, displayKcal))
            setTextViewText(R.id.notification_goal, String.format(Locale.US, "🎯 %.0f%% of %,d steps", goalPercent, goal))
            setProgressBar(R.id.notification_progress, 100, goalPercent.toInt(), false)
            setOnClickPendingIntent(R.id.notification_open, openPending)
        }

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle("StepCalc")
            .setContentText(String.format(Locale.US, "👟 %,d steps  •  🔥 %.1f kcal", steps, displayKcal))
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
            NotificationChannel(channelId, "Live steps", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Live StepCalc steps, calories and goal progress"
                setShowBadge(false)
            }
        )
        nm.createNotificationChannel(
            NotificationChannel("daily", "Daily motivation", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }

    private fun scheduleDailyQuote() {
        val am = getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            this, 99, Intent(this, QuoteReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val c = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP, c.timeInMillis, AlarmManager.INTERVAL_DAY, intent)
    }

    private fun dayKey() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    override fun onDestroy() {
        sensorManager.unregisterListener(this)
        super.onDestroy()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?) = null

    companion object {
        private const val NOTIFICATION_ID = 7
    }
}
