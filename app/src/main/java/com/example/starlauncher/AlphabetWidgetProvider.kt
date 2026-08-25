package com.example.starlauncher

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.widget.RemoteViews
import kotlin.math.abs

class AlphabetWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (widgetId in appWidgetIds) {
            updateWidget(context, appWidgetManager, widgetId, null)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_LETTER_TOUCHED) {
            val widgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val letter = intent.getStringExtra("EXTRA_TOUCHED_LETTER") ?: return
            val appWidgetManager = AppWidgetManager.getInstance(context)

            if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateWidget(context, appWidgetManager, widgetId, letter)

                val modalIntent = Intent(context, SketchbookModalActivity::class.java).apply {
                    putExtra("EXTRA_FILTER_LETTER", letter)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }
                context.startActivity(modalIntent)
            }
        }
    }

    companion object {
        const val ACTION_LETTER_TOUCHED = "com.example.starlauncher.ACTION_LETTER_TOUCHED"

        fun updateWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            widgetId: Int,
            activeLetter: String?
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_alphabet_sketchbook)
            val validLetters = getInstalledLetters(context)

            val bitmap = renderAlphabetBitmap(context, validLetters, activeLetter)
            views.setImageViewBitmap(R.id.alphabet_canvas_view, bitmap)

            val clickIntent = Intent(context, SketchbookModalActivity::class.java).apply {
                putExtra("EXTRA_FILTER_LETTER", activeLetter ?: "•")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                widgetId,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, pendingIntent)

            appWidgetManager.updateAppWidget(widgetId, views)
        }

        private fun getInstalledLetters(context: Context): List<String> {
            val pm = context.packageManager
            val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
            val apps = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
            } else {
                @Suppress("DEPRECATION")
                pm.queryIntentActivities(intent, 0)
            }

            val present = mutableSetOf<Char>()
            for (app in apps) {
                val label = app.loadLabel(pm)?.toString() ?: ""
                val first = label.firstOrNull()?.uppercaseChar()
                if (first != null && first in 'A'..'Z') {
                    present.add(first)
                }
            }

            val list = mutableListOf("•")
            for (c in 'A'..'Z') {
                if (present.contains(c)) list.add(c.toString())
            }
            return list
        }

        private fun renderAlphabetBitmap(
            context: Context,
            letters: List<String>,
            activeLetter: String?
        ): Bitmap {
            val density = context.resources.displayMetrics.density
            val widthPx = (54 * density).toInt()
            val heightPx = (440 * density).toInt()

            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
            }

            val trianglePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#111113")
                style = Paint.Style.FILL
            }

            val total = letters.size
            val stepY = heightPx.toFloat() / total
            val activeIdx = if (activeLetter != null) letters.indexOf(activeLetter) else 0

            for (i in letters.indices) {
                val item = letters[i]
                val centerY = (stepY * i) + (stepY / 2f)
                val dist = if (activeIdx >= 0) abs(i - activeIdx) else 10

                // Parabolic magnification for active and adjacent letters
                val scale = (1.0f - (dist * 0.28f)).coerceIn(0f, 1f)
                val textSize = (13.5f + (scale * 10.5f)) * density
                textPaint.textSize = textSize

                if (i == activeIdx && activeLetter != null) {
                    textPaint.color = Color.parseColor("#111113")
                    textPaint.isFakeBoldText = true

                    // Left-side selection triangle pointer
                    val trianglePath = Path().apply {
                        val triWidth = 14f * density
                        val triHeight = 12f * density
                        val startX = 4f * density
                        moveTo(startX, centerY - (triHeight / 2f))
                        lineTo(startX + triWidth, centerY)
                        lineTo(startX, centerY + (triHeight / 2f))
                        close()
                    }
                    canvas.drawPath(trianglePath, trianglePaint)
                } else {
                    textPaint.color = Color.parseColor("#71717A")
                    textPaint.alpha = (120 + (scale * 120)).toInt()
                    textPaint.isFakeBoldText = false
                }

                val textX = (widthPx * 0.65f)
                val fontMetrics = textPaint.fontMetrics
                val textBaseline = centerY - (fontMetrics.ascent + fontMetrics.descent) / 2f
                canvas.drawText(item, textX, textBaseline, textPaint)
            }

            return bitmap
        }
    }
}
