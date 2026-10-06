package com.gooroyeegar.stepcalc

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.*
import android.text.InputType
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

    private val cyan = Color.rgb(6, 182, 212)
    private val cyanDark = Color.rgb(8, 145, 168)
    private val bg = Color.rgb(239, 252, 254)
    private val textColor = Color.rgb(15, 23, 42)
    private val muted = Color.rgb(100, 116, 139)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { showDashboard() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (!prefs.getBoolean("setup", false)) showSetup() else showDashboard()
    }

    private fun rounded(color: Int, radius: Float = 20f, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = radius
            if (stroke != null) setStroke(1, stroke)
        }

    private fun cyanGradient(): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(34, 211, 238), cyan, Color.rgb(14, 165, 185))
        ).apply { cornerRadius = 28f }

    private fun tv(value: String, size: Float, color: Int = textColor, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = false
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

    private fun showSetup() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
        }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 28)
        }

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            background = cyanGradient()
        }
        hero.addView(tv("StepCalc", 38f, Color.WHITE, true))
        hero.addView(tv("Your daily walking companion", 16f, Color.WHITE).apply {
            alpha = 0.92f
            setPadding(0, 8, 0, 0)
        })
        root.addView(hero, LinearLayout.LayoutParams(-1, 132))

        root.addView(tv("Set up your ideal day", 25f, textColor, true).apply {
            setPadding(4, 24, 4, 6)
        })
        root.addView(tv("Choose your target, then enter your current body measurements.", 14f, muted).apply {
            setPadding(4, 0, 4, 18)
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 18)
            background = rounded(Color.WHITE, 22f, Color.rgb(207, 238, 243))
        }

        goal = field(card, "Ideal daily steps", "10,000", false)
        weight = field(card, "Current weight", "", true)
        height = field(card, "Height", "", true)

        val start = Button(this).apply {
            text = "GET STARTED"
            textSize = 16f
            setTextColor(Color.WHITE)
            isAllCaps = false
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            minHeight = 60
            minimumHeight = 60
            background = rounded(cyan, 18f)
            stateListAnimator = null
            includeFontPadding = false
        }

        card.addView(start, LinearLayout.LayoutParams(-1, 60).apply { topMargin = 8 })
        root.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 18 })

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(16, 16, 16, 16)
            background = rounded(Color.rgb(224, 247, 250), 18f)
        }
        info.addView(tv("1,000–100,000", 15f, cyanDark, true))
        info.addView(tv("  steps/day target", 14f, muted))
        root.addView(info, LinearLayout.LayoutParams(-1, 54))

        setContentView(scroll)
        scroll.addView(root)

        start.setOnClickListener {
            val g = goal.text.toString().replace(",", "").toIntOrNull()
            val w = weight.text.toString().toFloatOrNull()
            val h = height.text.toString().toFloatOrNull()
            if (g == null || g !in 1000..100000 || w == null || w <= 0 || h == null || h <= 0) {
                Toast.makeText(this, "Please enter valid values.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            prefs.edit()
                .putBoolean("setup", true)
                .putInt("goal", g)
                .putFloat("weight", w)
                .putFloat("height", h)
                .apply()
            animateStart()
        }
    }

    private fun field(parent: LinearLayout, label: String, value: String, decimal: Boolean): EditText {
        parent.addView(tv(label, 13f, muted, true).apply { setPadding(2, 4, 2, 7) })
        val input = EditText(this).apply {
            setText(value)
            hint = when (label) {
                "Ideal daily steps" -> "10,000"
                "Current weight" -> "e.g. 60"
                else -> "e.g. 165"
            }
            textSize = 18f
            setTextColor(this@MainActivity.textColor)
            setHintTextColor(Color.rgb(148, 163, 184))
            setSingleLine(true)
            includeFontPadding = false
            inputType = InputType.TYPE_CLASS_NUMBER or
                    if (decimal) InputType.TYPE_NUMBER_FLAG_DECIMAL else 0
            setPadding(16, 0, 16, 0)
            background = rounded(Color.rgb(248, 250, 252), 16f, Color.rgb(203, 213, 225))
        }
        parent.addView(input, LinearLayout.LayoutParams(-1, 58).apply { bottomMargin = 14 })
        return input
    }

    private fun animateStart() {
        val root = FrameLayout(this).apply { setBackgroundColor(bg) }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(32, 32, 32, 32)
            background = cyanGradient()
        }
        card.addView(tv("Let's get moving", 32f, Color.WHITE, true).apply { gravity = Gravity.CENTER })
        card.addView(tv("Step by step.", 16f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            alpha = .9f
            setPadding(0, 10, 0, 0)
        })
        root.addView(card, FrameLayout.LayoutParams(-1, 190).apply {
            leftMargin = 20
            rightMargin = 20
            gravity = Gravity.CENTER
        })
        setContentView(root)
        card.scaleX = .82f
        card.scaleY = .82f
        card.alpha = 0f
        card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(650).withEndAction {
            requestPermissionsAndStart()
        }.start()
    }

    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun showDashboard() {
        val scroll = ScrollView(this).apply { setBackgroundColor(bg) }
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 22, 20, 28)
        }

        val goalValue = prefs.getInt("goal", 10000)
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(22, 22, 22, 22)
            background = cyanGradient()
        }
        hero.addView(tv("TODAY", 14f, Color.WHITE, true))
        hero.addView(tv("0 steps", 38f, Color.WHITE, true).apply { setPadding(0, 7, 0, 2) })
        hero.addView(tv("Goal: " + String.format("%,d", goalValue) + " steps", 16f, Color.WHITE))
        root.addView(hero, LinearLayout.LayoutParams(-1, 155))

        root.addView(tv("Your progress", 25f, textColor, true).apply { setPadding(4, 22, 4, 12) })

        val grid = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(statCard("👟", "Steps", "0"), LinearLayout.LayoutParams(0, 128, 1f).apply { rightMargin = 7 })
        row1.addView(statCard("🔥", "Calories", "0 kcal"), LinearLayout.LayoutParams(0, 128, 1f).apply { leftMargin = 7 })
        grid.addView(row1)
        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(statCard("⚖", "Loss eq.", "0.00 kg"), LinearLayout.LayoutParams(0, 128, 1f).apply { rightMargin = 7; topMargin = 12 })
        row2.addView(statCard("🎯", "Target", String.format("%,d", goalValue)), LinearLayout.LayoutParams(0, 128, 1f).apply { leftMargin = 7; topMargin = 12 })
        grid.addView(row2)
        root.addView(grid)

        val note = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 17, 18, 17)
            background = rounded(Color.WHITE, 20f, Color.rgb(207, 238, 243))
        }
        note.addView(tv("LIVE TRACKING", 13f, cyanDark, true))
        note.addView(tv("Your step counter stays active through the notification bar.", 15f, textColor).apply {
            setPadding(0, 7, 0, 0)
        })
        root.addView(note, LinearLayout.LayoutParams(-1, 88).apply { topMargin = 14 })

        setContentView(scroll)
        scroll.addView(root)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
            ContextCompat.startForegroundService(this, Intent(this, StepService::class.java))
    }

    private fun statCard(icon: String, label: String, value: String): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 14, 16, 12)
            background = rounded(Color.WHITE, 20f, Color.rgb(226, 232, 240))
        }
        box.addView(tv(icon, 21f, cyanDark))
        box.addView(tv(label, 13f, muted, true).apply { setPadding(0, 6, 0, 2) })
        box.addView(tv(value, 21f, textColor, true))
        return box
    }
}
