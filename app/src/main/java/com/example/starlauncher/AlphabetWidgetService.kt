package com.example.starlauncher

import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService

class AlphabetWidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return AlphabetRemoteViewsFactory(this.applicationContext)
    }
}

class AlphabetRemoteViewsFactory(private val context: Context) : RemoteViewsService.RemoteViewsFactory {

    private var activeLetters: List<String> = emptyList()

    override fun onCreate() {
        loadLetters()
    }

    override fun onDataSetChanged() {
        loadLetters()
    }

    private fun loadLetters() {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply { addCategory(Intent.CATEGORY_LAUNCHER) }
        val apps = pm.queryIntentActivities(intent, 0)

        val presentLetters = mutableSetOf<Char>()
        for (app in apps) {
            val label = app.loadLabel(pm)?.toString() ?: ""
            val first = label.firstOrNull()?.uppercaseChar()
            if (first != null && first in 'A'..'Z') {
                presentLetters.add(first)
            }
        }

        val list = mutableListOf("•")
        for (c in 'A'..'Z') {
            if (presentLetters.contains(c)) {
                list.add(c.toString())
            }
        }
        activeLetters = list
    }

    override fun onDestroy() {}
    override fun getCount(): Int = activeLetters.size

    override fun getViewAt(position: Int): RemoteViews {
        val letter = activeLetters[position]
        val views = RemoteViews(context.packageName, R.layout.item_alphabet_letter)
        views.setTextViewText(R.id.letter_text, letter)

        val fillInIntent = Intent().apply {
            putExtra("EXTRA_FILTER_LETTER", letter)
        }
        views.setOnClickFillInIntent(R.id.letter_text, fillInIntent)
        return views
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
}
