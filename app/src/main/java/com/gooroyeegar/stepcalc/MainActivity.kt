package com.gooroyeegar.stepcalc

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.*
import android.view.*
import android.widget.*
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private lateinit var goal: EditText
    private lateinit var weight: EditText
    private lateinit var height: EditText
    private val prefs by lazy { getSharedPreferences("stepcalc", MODE_PRIVATE) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { showDashboard() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!prefs.getBoolean("setup", false)) showSetup() else showDashboard()
    }

    private fun showSetup() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(28, 40, 28, 28)
            setBackgroundColor(0xFFF7F5EF.toInt())
        }
        val title = TextView(this).apply { text = "StepCalc"; textSize = 34f; setTextColor(0xFF20352A.toInt()) }
        val sub = TextView(this).apply { text = "Build your ideal walking day."; textSize = 17f; setPadding(0,8,0,28) }
        goal = field("Ideal daily steps (1,000–100,000)", "10000")
        weight = field("Current weight (kg)", "")
        height = field("Height (cm)", "")
        val start = Button(this).apply { text = "Get started"; textSize = 16f }
        root.addView(title); root.addView(sub); root.addView(goal); root.addView(weight); root.addView(height)
        root.addView(start, LinearLayout.LayoutParams(-1, 58).apply { topMargin = 22 })
        setContentView(root)
        start.setOnClickListener {
            val g = goal.text.toString().toIntOrNull()
            val w = weight.text.toString().toFloatOrNull()
            val h = height.text.toString().toFloatOrNull()
            if (g == null || g !in 1000..100000 || w == null || w <= 0 || h == null || h <= 0) {
                Toast.makeText(this, "Please enter valid values.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit().putBoolean("setup", true).putInt("goal", g).putFloat("weight", w).putFloat("height", h).apply()
            animateStart()
        }
    }

    private fun field(hint: String, value: String): EditText = EditText(this).apply {
        this.hint = hint; setText(value); textSize = 16f; setSingleLine()
        inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL
        layoutParams = LinearLayout.LayoutParams(-1, 58).apply { bottomMargin = 10 }
    }

    private fun animateStart() {
        val v = TextView(this).apply {
            text = "Let's get moving"
            textSize = 30f
            gravity = Gravity.CENTER
            setTextColor(0xFF20352A.toInt())
            setBackgroundColor(0xFFF7F5EF.toInt())
        }
        setContentView(v)
        v.scaleX = .75f; v.scaleY = .75f; v.alpha = 0f
        v.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(650).withEndAction {
            requestPermissionsAndStart()
        }.start()
    }

    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun showDashboard() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24,36,24,24)
            setBackgroundColor(0xFFF7F5EF.toInt())
        }
        val title = TextView(this).apply { text = "Today"; textSize = 32f; setTextColor(0xFF20352A.toInt()) }
        val stats = TextView(this).apply { textSize = 20f; setPadding(0,20,0,0) }
        stats.text = "Goal: " + prefs.getInt("goal",10000) + " steps\n\nYour steps, calories and estimated weight-loss equivalent are tracked automatically.\n\nThe live notification stays visible while you walk."
        root.addView(title); root.addView(stats)
        setContentView(root)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
            ContextCompat.startForegroundService(this, Intent(this, StepService::class.java))
    }
}
