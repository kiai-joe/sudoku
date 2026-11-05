package com.kiaijoe.sudoku.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * The 2×1 Streak widget. It shows the same daily record as [SumiWidget],
 * which draws, refreshes and schedules both.
 */
class SumiStreakWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = SumiWidget.renderSmall(context, SumiWidget.model(context))
        for (id in ids) manager.updateAppWidget(id, views)
        SumiWidget.scheduleMidnight(context)
    }

    override fun onDisabled(context: Context) = SumiWidget.stopMidnightIfUnused(context)
}
