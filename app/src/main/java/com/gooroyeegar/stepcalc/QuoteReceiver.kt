package com.gooroyeegar.stepcalc
import android.app.*
import android.content.*
import androidx.core.app.NotificationCompat

class QuoteReceiver: BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val quotes = arrayOf(
            "One step at a time is still progress.","Your future self will thank you for this walk.",
            "Small walks become big habits.","You don't need perfect. Just keep moving.",
            "A little farther today.","Keep going — you've already started.",
            "Your goal is waiting for you.","Progress loves consistency.","Walk it out.",
            "Today counts. So do you.","Ten more minutes can change the day.","Keep your promise to yourself.",
            "Movement is a vote for the person you want to become.","You are closer than yesterday.",
            "Start where you are. Keep moving.","Consistency beats intensity.","Make today count."
        )
        val day = java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_YEAR)
        val msg = quotes[(day - 1) % quotes.size]
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("daily","Daily motivation",NotificationManager.IMPORTANCE_DEFAULT))
        nm.notify(9000, NotificationCompat.Builder(context,"daily").setSmallIcon(R.drawable.ic_launcher).setContentTitle("StepCalc").setContentText(msg).setAutoCancel(true).build())
    }
}
