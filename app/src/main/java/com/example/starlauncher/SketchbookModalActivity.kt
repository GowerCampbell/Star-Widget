package com.example.starlauncher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.*

class SketchbookModalActivity : Activity() {

    data class AppItem(val name: String, val packageName: String, val icon: Drawable)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val displayMetrics = resources.displayMetrics
        window.setLayout(
            (displayMetrics.widthPixels * 0.88).toInt(),
            (displayMetrics.heightPixels * 0.62).toInt()
        )
        window.setGravity(Gravity.CENTER)
        window.setBackgroundDrawableResource(android.R.color.transparent)
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.attributes.dimAmount = 0.45f
        setFinishOnTouchOutside(true)

        val selectedChar = intent.getStringExtra("EXTRA_FILTER_LETTER") ?: "•"
        val apps = loadInstalledApps()
        val filtered = if (selectedChar == "•") apps else apps.filter { it.name.startsWith(selectedChar, ignoreCase = true) }

        val rootCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                setColor(Color.parseColor("#FAFBFD"))
                cornerRadius = 38f
                setStroke(3, Color.parseColor("#26111113"))
            }
            elevation = 50f
            setPadding(32, 28, 32, 28)
        }

        val headerRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(8, 4, 8, 18)
        }

        val title = TextView(this).apply {
            text = if (selectedChar == "•") "ALL APPLICATIONS" else "INDEX [$selectedChar]"
            setTextColor(Color.parseColor("#111113"))
            textSize = 15f
            letterSpacing = 0.12f
            typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }
        val closeBtn = TextView(this).apply {
            text = "[ DISMISS ]"
            setTextColor(Color.parseColor("#71717A"))
            textSize = 11.5f
            typeface = Typeface.MONOSPACE
            setOnClickListener { finish() }
        }
        headerRow.addView(title)
        headerRow.addView(closeBtn)
        rootCard.addView(headerRow)

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            isVerticalScrollBarEnabled = false
        }

        val listContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }

        if (filtered.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "No applications under '$selectedChar'"
                setTextColor(Color.parseColor("#71717A"))
                textSize = 13.5f
                typeface = Typeface.MONOSPACE
                setPadding(12, 40, 0, 0)
            }
            listContainer.addView(emptyTv)
        } else {
            for (app in filtered) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(12, 14, 12, 14)
                    isClickable = true
                    isFocusable = true
                }

                val iv = ImageView(this).apply {
                    setImageDrawable(app.icon)
                    val size = 96
                    layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(0, 0, 20, 0) }
                }
                row.addView(iv)

                val name = TextView(this).apply {
                    text = app.name
                    setTextColor(Color.parseColor("#111113"))
                    textSize = 16f
                    typeface = Typeface.DEFAULT_BOLD
                }
                row.addView(name)

                row.setOnClickListener {
                    finish()
                    val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                    if (launchIntent != null) startActivity(launchIntent)
                }

                listContainer.addView(row)
            }
        }

        scroll.addView(listContainer)
        rootCard.addView(scroll)

        setContentView(rootCard)
    }

    private fun loadInstalledApps(): List<AppItem> {
        val list = mutableListOf<AppItem>()
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }

        for (info in resolved) {
            val pName = info.activityInfo?.packageName ?: continue
            if (pName == packageName) continue
            val label = info.loadLabel(packageManager)?.toString() ?: pName
            val icon = info.loadIcon(packageManager)
            list.add(AppItem(label, pName, icon))
        }
        return list.sortedBy { it.name.lowercase() }
    }
}

