package com.gooroyeegar.stepcalc
import android.app.*
import android.content.*
import android.hardware.*
import android.os.*
import androidx.core.app.NotificationCompat
import java.text.SimpleDateFormat
import java.util.*

class StepService : Service(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private val prefs by lazy { getSharedPreferences("stepcalc", MODE_PRIVATE) }
    private val channelId = "stepcalc_live"
    private var base = -1f

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(7, notification(0,0f,0f))
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL) }
        scheduleDailyQuote()
    }

    override fun onSensorChanged(e: SensorEvent) {
        val total = e.values[0]
        val today = dayKey()
        val savedDay = prefs.getString("day", today)
        if (savedDay != today) {
            base = total
            prefs.edit().putString("day", today).putFloat("sensor_base", base).putInt("steps",0).apply()
        } else if (base < 0f) base = prefs.getFloat("sensor_base", total)
        val steps = maxOf(0, (total - base).toInt())
        prefs.edit().putInt("steps", steps).putFloat("sensor_base", base).putString("day",today).apply()
        update(steps)
    }

    private fun update(steps: Int) {
        val weight = prefs.getFloat("weight", 60f)
        val height = prefs.getFloat("height", 165f)
        val distanceKm = steps * (height * 0.413f / 100000f)
        val kcal = distanceKm * weight * 0.75f
        val kg = kcal / 7700f
        getSystemService(NotificationManager::class.java).notify(7, notification(steps,kcal,kg))
    }

    private fun notification(steps:Int,kcal:Float,kg:Float): Notification {
        val goal = prefs.getInt("goal",10000)
        val text = steps.toString() + " / " + goal + " steps  •  " + kcal.toInt() + " kcal  •  " + String.format(Locale.US,"%.2f",kg) + " kg"
        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.gooroyeegar.stepcalc.R.drawable.ic_launcher)
            .setContentTitle("StepCalc")
            .setContentText(text)
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(goal, minOf(steps,goal), false).build()
    }

    private fun createChannels() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel(channelId,"Live steps",NotificationManager.IMPORTANCE_LOW))
        nm.createNotificationChannel(NotificationChannel("daily","Daily motivation",NotificationManager.IMPORTANCE_DEFAULT))
    }

    private fun scheduleDailyQuote() {
        val am = getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(this,99,Intent(this,QuoteReceiver::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val c = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY,9); set(Calendar.MINUTE,0); set(Calendar.SECOND,0); set(Calendar.MILLISECOND,0); if(timeInMillis<=System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR,1) }
        am.setInexactRepeating(AlarmManager.RTC_WAKEUP,c.timeInMillis,AlarmManager.INTERVAL_DAY,intent)
    }
    private fun dayKey() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    override fun onBind(intent: Intent?) = null
}
