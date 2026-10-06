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
import java.util.Locale

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

    private var heroSteps: TextView? = null
    private var heroGoal: TextView? = null
    private var stepsValue: TextView? = null
    private var kcalValue: TextView? = null
    private var kgValue: TextView? = null
    private var targetValue: TextView? = null
    private val uiHandler = Handler(Looper.getMainLooper())
    private val uiRefresh = object : Runnable {
        override fun run() {
            refreshDashboardValues()
            uiHandler.postDelayed(this, 700)
        }
    }

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

    override fun onResume() {
        super.onResume()
        if (prefs.getBoolean("setup", false)) {
            uiHandler.removeCallbacks(uiRefresh)
            uiHandler.post(uiRefresh)
        }
    }

    override fun onPause() {
        uiHandler.removeCallbacks(uiRefresh)
        super.onPause()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    private fun rounded(color: Int, radius: Int = 20, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radius).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun cyanGradient(): GradientDrawable =
        GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(Color.rgb(34, 211, 238), cyan, Color.rgb(14, 165, 185))
        ).apply { cornerRadius = dp(28).toFloat() }

    private fun tv(value: String, size: Float, color: Int = textColor, bold: Boolean = false): TextView =
        TextView(this).apply {
            text = value
            textSize = size
            setTextColor(color)
            includeFontPadding = true
            gravity = Gravity.CENTER_VERTICAL
            if (bold) typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

    private fun showSetup() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(28))
        }

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = cyanGradient()
        }
        hero.addView(tv("StepCalc", 38f, Color.WHITE, true))
        hero.addView(tv("Your daily walking companion", 16f, Color.WHITE).apply {
            alpha = 0.92f
            setPadding(0, dp(8), 0, 0)
        })
        hero.minimumHeight = dp(132)
        root.addView(hero, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        root.addView(tv("Set up your ideal day", 25f, textColor, true).apply {
            setPadding(dp(4), dp(24), dp(4), dp(6))
        })
        root.addView(tv(
            "Choose your target, then enter your current body measurements.",
            14f, muted
        ).apply {
            setPadding(dp(4), 0, dp(4), dp(18))
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(Color.WHITE, 22, Color.rgb(207, 238, 243))
        }

        goal = field(card, "Ideal daily steps", "10,000", false)
        weight = field(card, "Current weight", "", true)
        height = field(card, "Height", "", true)

        val start = Button(this).apply {
            text = "GET STARTED"
            textSize = 17f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            isAllCaps = false
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            minHeight = 0
            minimumHeight = 0
            includeFontPadding = true
            setPadding(dp(12), 0, dp(12), 0)
            background = rounded(cyan, 18)
            stateListAnimator = null
        }

        card.addView(start, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(64)
        ).apply { topMargin = dp(8) })
        root.addView(card, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(18) })

        val info = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(12), dp(16), dp(12))
            background = rounded(Color.rgb(224, 247, 250), 18)
        }
        info.addView(tv("1,000–100,000", 15f, cyanDark, true))
        info.addView(tv("  steps/day target", 14f, muted))
        info.minimumHeight = dp(58)
        root.addView(info, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        scroll.addView(root)
        setContentView(scroll)

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
        parent.addView(tv(label, 13f, muted, true).apply {
            setPadding(dp(2), dp(4), dp(2), dp(7))
        })

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
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = true
            inputType = InputType.TYPE_CLASS_NUMBER or
                    if (decimal) InputType.TYPE_NUMBER_FLAG_DECIMAL else 0
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(
                Color.rgb(248, 250, 252), 16,
                Color.rgb(203, 213, 225)
            )
        }

        parent.addView(input, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(58)
        ).apply { bottomMargin = dp(14) })
        return input
    }

    private fun animateStart() {
        val root = FrameLayout(this).apply { setBackgroundColor(bg) }
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(32), dp(32), dp(32))
            background = cyanGradient()
        }
        card.addView(tv("Let's get moving", 32f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
        })
        card.addView(tv("Step by step.", 16f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            alpha = .9f
            setPadding(0, dp(10), 0, 0)
        })
        root.addView(card, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dp(20)
            rightMargin = dp(20)
            minimumHeight = dp(190)
            gravity = Gravity.CENTER
        })
        setContentView(root)
        card.scaleX = .82f
        card.scaleY = .82f
        card.alpha = 0f
        card.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(650)
            .withEndAction { requestPermissionsAndStart() }
            .start()
    }

    private fun requestPermissionsAndStart() {
        val permissions = mutableListOf(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun showDashboard() {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(bg)
            isFillViewport = true
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(28))
        }

        val goalValue = prefs.getInt("goal", 10000)
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = cyanGradient()
        }
        hero.addView(tv("TODAY", 14f, Color.WHITE, true))
        heroSteps = tv("0 steps", 38f, Color.WHITE, true).apply {
            setPadding(0, dp(6), 0, dp(4))
            minHeight = dp(52)
        }
        hero.addView(heroSteps)
        heroGoal = tv("Goal: " + String.format(Locale.US, "%,d", goalValue) + " steps", 16f, Color.WHITE)
        hero.addView(heroGoal)
        hero.minimumHeight = dp(155)
        root.addView(hero, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        root.addView(tv("Your progress", 25f, textColor, true).apply {
            setPadding(dp(4), dp(22), dp(4), dp(12))
        })

        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val row1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        row1.addView(
            statCard("👟", "Steps", "0").also { stepsValue = it.findViewWithTag("value") },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(7) }
        )
        row1.addView(
            statCard("🔥", "Calories", "0 kcal").also { kcalValue = it.findViewWithTag("value") },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(7) }
        )
        grid.addView(row1)

        val row2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }
        row2.addView(
            statCard("⚖", "Loss eq.", "0.00 kg").also { kgValue = it.findViewWithTag("value") },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(7)
                topMargin = dp(12)
            }
        )
        row2.addView(
            statCard("🎯", "Target", String.format(Locale.US, "%,d", goalValue))
                .also { targetValue = it.findViewWithTag("value") },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                leftMargin = dp(7)
                topMargin = dp(12)
            }
        )
        grid.addView(row2)
        root.addView(grid)

        val note = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(17), dp(18), dp(17))
            background = rounded(Color.WHITE, 20, Color.rgb(207, 238, 243))
        }
        note.addView(tv("LIVE TRACKING", 13f, cyanDark, true))
        note.addView(tv(
            "Your steps stay active through the notification bar, even while you use other apps.",
            15f, textColor
        ).apply {
            setPadding(0, dp(7), 0, 0)
        })
        root.addView(note, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(14) })

        val spacer = Space(this)
        root.addView(spacer, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
        ).apply { topMargin = dp(18) })

        scroll.addView(root)
        setContentView(scroll)

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            ContextCompat.startForegroundService(this, Intent(this, StepService::class.java))
        }
        refreshDashboardValues()
    }

    private fun statCard(icon: String, label: String, value: String): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            minimumHeight = dp(128)
            background = rounded(Color.WHITE, 20, Color.rgb(226, 232, 240))
        }
        box.addView(tv(icon, 21f, cyanDark).apply { includeFontPadding = true })
        box.addView(tv(label, 13f, muted, true).apply {
            setPadding(0, dp(5), 0, dp(2))
        })
        box.addView(tv(value, 21f, textColor, true).apply {
            tag = "value"
            setPadding(0, dp(1), 0, 0)
        })
        return box
    }

    private fun refreshDashboardValues() {
        if (!prefs.getBoolean("setup", false)) return
        val steps = prefs.getInt("steps", 0)
        val goal = prefs.getInt("goal", 10000)
        val weight = prefs.getFloat("weight", 60f)
        val height = prefs.getFloat("height", 165f)
        val distanceKm = steps * (height * 0.413f / 100000f)
        val kcal = distanceKm * weight * 0.75f
        val kg = kcal / 7700f

        heroSteps?.text = String.format(Locale.US, "%,d steps", steps)
        heroGoal?.text = String.format(Locale.US, "Goal: %,d steps", goal)
        stepsValue?.text = String.format(Locale.US, "%,d", steps)
        kcalValue?.text = String.format(Locale.US, "%.1f kcal", kcal)
        kgValue?.text = String.format(Locale.US, "%.3f kg", kg)
        targetValue?.text = String.format(Locale.US, "%,d", goal)
    }
}
