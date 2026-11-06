package com.kiaijoe.sudoku.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * The 4×2 Daily Killer widget: today's Killer, its streak and this week,
 * drawn like [SumiWidget] (which also refreshes and schedules it) from the
 * daily Killer's record. Its button opens straight into today's Killer.
 */
class SumiKillerWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = SumiWidget.render(context, SumiWidget.model(context, killer = true), killer = true)
        for (id in ids) manager.updateAppWidget(id, views)
        SumiWidget.scheduleMidnight(context)
    }

    override fun onDisabled(context: Context) = SumiWidget.stopMidnightIfUnused(context)
}
