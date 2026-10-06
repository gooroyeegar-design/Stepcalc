package com.gooroyeegar.stepcalc

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.graphics.Color
import android.view.Gravity
import android.widget.*
import java.io.File

class CrashReportActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val reportFile = File(filesDir, StepCalcApplication.CRASH_FILE)
        val report = try {
            if (reportFile.exists()) reportFile.readText(Charsets.UTF_8) else "No crash report is available."
        } catch (_: Throwable) {
            "The crash report could not be read."
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            setBackgroundColor(Color.WHITE)
        }
        root.addView(TextView(this).apply {
            text = "StepCalc crashed last time"
            textSize = 24f
            setTextColor(Color.rgb(15, 23, 42))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, 0, 0, 16)
        })
        root.addView(TextView(this).apply {
            text = "The full offline crash report is below. Copy or share it so the problem can be diagnosed."
            textSize = 15f
            setTextColor(Color.rgb(71, 85, 105))
            setPadding(0, 0, 0, 12)
        })

        val reportView = TextView(this).apply {
            text = report
            textSize = 12f
            setTextColor(Color.rgb(15, 23, 42))
            setTextIsSelectable(true)
            setPadding(12, 12, 12, 12)
            setBackgroundColor(Color.rgb(241, 245, 249))
        }
        val scroll = ScrollView(this)
        scroll.addView(reportView)
        root.addView(scroll, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
        ))

        val buttons = LinearLayout(this).apply {
            gravity = Gravity.CENTER
            setPadding(0, 14, 0, 0)
        }
        fun button(label: String) = Button(this).apply {
            text = label
            isAllCaps = false
        }
        val copy = button("Copy")
        val share = button("Share")
        val continueButton = button("Continue")
        buttons.addView(copy)
        buttons.addView(share)
        buttons.addView(continueButton)
        root.addView(buttons)

        copy.setOnClickListener {
            val clipboard = getSystemService(ClipboardManager::class.java)
            clipboard.setPrimaryClip(ClipData.newPlainText("StepCalc crash report", report))
            Toast.makeText(this, "Crash report copied", Toast.LENGTH_SHORT).show()
        }
        share.setOnClickListener {
            startActivity(Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "StepCalc crash report")
                    putExtra(Intent.EXTRA_TEXT, report)
                },
                "Share crash report"
            ))
        }
        continueButton.setOnClickListener {
            runCatching { reportFile.delete() }
            startActivity(Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            })
            finish()
        }

        setContentView(root)
    }
}
