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
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Calendar
import java.util.Locale

class MainActivity : androidx.appcompat.app.AppCompatActivity() {
    private lateinit var goal: EditText
    private lateinit var weight: EditText
    private lateinit var height: EditText
    private val prefs by lazy { getSharedPreferences("stepcalc", MODE_PRIVATE) }

    private fun themeColor(light: String, dark: String): Int =
        Color.parseColor(if (isDarkMode()) dark else light)

    private val cyan get() = themeColor("#06B6D4", "#22D3EE")
    private val cyanDark get() = themeColor("#0891A8", "#67E8F9")
    private val bg get() = themeColor("#EFFCFE", "#071419")
    private val surface get() = themeColor("#FFFFFF", "#10242A")
    private val fieldBg get() = themeColor("#F8FAFC", "#132A31")
    private val textColor get() = themeColor("#0F172A", "#F2FBFC")
    private val muted get() = themeColor("#64748B", "#A7BCC1")
    private val border get() = themeColor("#E2E8F0", "#29434A")
    private val infoBg get() = themeColor("#E0F7FA", "#12353D")
    private val infoBorder get() = themeColor("#CFEFF3", "#25515A")
    private val hintColor get() = themeColor("#94A3B8", "#78929A")

    private var heroSteps: TextView? = null
    private var heroGoal: TextView? = null
    private var stepsValue: TextView? = null
    private var kcalValue: TextView? = null
    private var kgValue: TextView? = null
    private var targetValue: TextView? = null
    private var liveBody: TextView? = null
    private val uiHandler = Handler(Looper.getMainLooper())
    private val uiRefresh = object : Runnable {
        override fun run() {
            refreshDashboardValues()
            uiHandler.postDelayed(this, 700)
        }
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        showDashboard()
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Toast.makeText(this, getString(R.string.permission_needed), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d("StepCalcBoot", "permissions check")
        val crashFile = java.io.File(filesDir, StepCalcApplication.CRASH_FILE)
        if (crashFile.exists()) {
            Log.d("StepCalcBoot", "crash report found; opening recovery screen")
            startActivity(Intent(this, CrashReportActivity::class.java))
            finish()
            return
        }
        Log.d("StepCalcBoot", "database open: SharedPreferences (no database)")
        applySystemBars()
        if (!prefs.getBoolean("setup", false)) showSetup() else showDashboard()
    }

    override fun onResume() {
        super.onResume()
        applySystemBars()
        if (prefs.getBoolean("setup", false)) {
            uiHandler.removeCallbacks(uiRefresh)
            uiHandler.post(uiRefresh)
        }
    }

    override fun onPause() {
        uiHandler.removeCallbacks(uiRefresh)
        super.onPause()
    }

    private fun applySystemBars() {
        window.statusBarColor = bg
        window.navigationBarColor = bg
        window.decorView.systemUiVisibility =
            if (isDarkMode()) 0 else View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
    }

    private fun isDarkMode(): Boolean =
        (resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) ==
                android.content.res.Configuration.UI_MODE_NIGHT_YES

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
            intArrayOf(
                themeColor("#22D3EE", "#67E8F9"),
                cyan,
                cyanDark
            )
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

    private fun languageButton(): TextView =
        tv(getString(R.string.language_button), 14f, cyanDark, true).apply {
            gravity = Gravity.CENTER
            background = rounded(infoBg, 16, infoBorder)
            setOnClickListener { showLanguagePicker() }
            contentDescription = getString(R.string.choose_language)
        }

    private fun showLanguagePicker() {
        val tags = arrayOf("en", "ar", "fr", "es", "de", "it", "pt", "tr", "ru", "zh")
        val names = resources.getStringArray(R.array.language_names)
        val current = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: "en"
        val checked = tags.indexOf(current).coerceAtLeast(0)
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.choose_language))
            .setSingleChoiceItems(names, checked) { dialog, which ->
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tags[which]))
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
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
        root.addView(languageButton(), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(44)
        ).apply { bottomMargin = dp(10) })

        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
            background = cyanGradient()
        }
        hero.addView(tv(getString(R.string.app_name), 38f, Color.WHITE, true))
        hero.addView(tv(getString(R.string.setup_subtitle), 16f, Color.WHITE).apply {
            alpha = 0.92f
            setPadding(0, dp(8), 0, 0)
        })
        hero.minimumHeight = dp(132)
        root.addView(hero, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        root.addView(tv(getString(R.string.setup_title), 25f, textColor, true).apply {
            setPadding(dp(4), dp(24), dp(4), dp(6))
        })
        root.addView(tv(
            getString(R.string.setup_description), 14f, muted
        ).apply {
            setPadding(dp(4), 0, dp(4), dp(18))
        })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(18), dp(18), dp(18))
            background = rounded(surface, 22, infoBorder)
        }

        goal = field(card, getString(R.string.ideal_daily_steps), getString(R.string.hint_steps), false)
        weight = field(card, getString(R.string.current_weight), "", true)
        height = field(card, getString(R.string.height), "", true)

        val start = Button(this).apply {
            text = getString(R.string.get_started)
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
            background = rounded(infoBg, 18)
        }
        info.addView(tv(getString(R.string.target_range), 15f, cyanDark, true))
        info.addView(tv(getString(R.string.steps_per_day_target), 14f, muted))
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
                Toast.makeText(this, getString(R.string.valid_values), Toast.LENGTH_SHORT).show()
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
                getString(R.string.ideal_daily_steps) -> getString(R.string.hint_steps)
                getString(R.string.current_weight) -> getString(R.string.hint_weight)
                else -> getString(R.string.hint_height)
            }
            textSize = 18f
            setTextColor(this@MainActivity.textColor)
            setHintTextColor(this@MainActivity.hintColor)
            setSingleLine(true)
            gravity = Gravity.CENTER_VERTICAL
            includeFontPadding = true
            inputType = InputType.TYPE_CLASS_NUMBER or
                    if (decimal) InputType.TYPE_NUMBER_FLAG_DECIMAL else 0
            setPadding(dp(16), 0, dp(16), 0)
            background = rounded(fieldBg, 16, border)
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
        card.addView(tv(getString(R.string.start_moving), 32f, Color.WHITE, true).apply {
            gravity = Gravity.CENTER
        })
        card.addView(tv(getString(R.string.step_by_step), 16f, Color.WHITE).apply {
            gravity = Gravity.CENTER
            alpha = .9f
            setPadding(0, dp(10), 0, 0)
        })
        card.minimumHeight = dp(190)
        root.addView(card, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            leftMargin = dp(20)
            rightMargin = dp(20)
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
        root.addView(languageButton(), LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, dp(44)
        ).apply { bottomMargin = dp(10) })

        val goalValue = prefs.getInt("goal", 10000)
        val hero = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(20), dp(22), dp(20))
            background = cyanGradient()
        }
        hero.addView(tv(getString(R.string.today), 14f, Color.WHITE, true))
        heroSteps = tv(getString(R.string.today_steps, 0), 38f, Color.WHITE, true).apply {
            setPadding(0, dp(6), 0, dp(4))
            minimumHeight = dp(52)
        }
        hero.addView(heroSteps)
        heroGoal = tv(getString(R.string.goal_steps, goalValue), 16f, Color.WHITE)
        hero.addView(heroGoal)
        hero.minimumHeight = dp(155)
        root.addView(hero, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ))

        root.addView(tv(getString(R.string.your_progress), 25f, textColor, true).apply {
            setPadding(dp(4), dp(22), dp(4), dp(12))
        })

        val grid = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        val row1 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row1.addView(
            statCard(getString(R.string.icon_steps), getString(R.string.steps), getString(R.string.zero)).also { stepsValue = it.findViewWithTag("value") },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { rightMargin = dp(7) }
        )
        row1.addView(
            statCard(getString(R.string.icon_calories), getString(R.string.calories), getString(R.string.zero_kcal)).also { kcalValue = it.findViewWithTag("value") },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(7) }
        )
        grid.addView(row1)

        val row2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        row2.addView(
            statCard(getString(R.string.icon_loss), getString(R.string.loss_equivalent), getString(R.string.zero_kg)).also { kgValue = it.findViewWithTag("value") },
            LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                rightMargin = dp(7)
                topMargin = dp(12)
            }
        )
        row2.addView(
            statCard(getString(R.string.icon_target), getString(R.string.target), String.format(Locale.US, "%,d", goalValue))
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
            background = rounded(surface, 20, infoBorder)
        }
        note.addView(tv(getString(R.string.live_tracking), 13f, cyanDark, true))
        liveBody = tv("", 15f, textColor).apply {
            setPadding(0, dp(7), 0, 0)
        }
        note.addView(liveBody)
        root.addView(note, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(14) })

        val nudge = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(16), dp(18), dp(16))
            background = rounded(infoBg, 20)
        }
        nudge.addView(tv(getString(R.string.daily_nudge), 13f, cyanDark, true))
        val quotes = resources.getStringArray(R.array.daily_quotes)
        val quoteIndex = (Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - 1) % quotes.size
        nudge.addView(tv(quotes[quoteIndex], 15f, textColor).apply {
            setPadding(0, dp(6), 0, 0)
        })
        root.addView(nudge, LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = dp(12) })

        scroll.addView(root)
        Log.d("StepCalcBoot", "first UI render")
        setContentView(scroll)

        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            Log.d("StepCalcBoot", "service start")
            try {
                ContextCompat.startForegroundService(this, Intent(this, StepService::class.java))
            } catch (t: Throwable) {
                Log.e("StepCalcBoot", "service start failed", t)
            }
        }
        refreshDashboardValues()
    }

    private fun statCard(icon: String, label: String, value: String): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(14))
            minimumHeight = dp(128)
            background = rounded(surface, 20, border)
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

        heroSteps?.text = getString(R.string.today_steps, steps)
        heroGoal?.text = getString(R.string.goal_steps, goal)
        stepsValue?.text = String.format(Locale.US, "%,d", steps)
        kcalValue?.text = getString(R.string.calories_value, kcal)
        kgValue?.text = getString(R.string.kg_value, kg)
        targetValue?.text = String.format(Locale.US, "%,d", goal)

        val permission = ContextCompat.checkSelfPermission(
            this, Manifest.permission.ACTIVITY_RECOGNITION
        ) == PackageManager.PERMISSION_GRANTED
        liveBody?.text = when {
            !permission -> getString(R.string.permission_needed)
            !prefs.getBoolean("sensor_available", true) -> getString(R.string.no_step_sensor)
            else -> getString(R.string.live_tracking_body)
        }
    }
}
