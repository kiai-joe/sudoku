package com.kiaijoe.sudoku.widget

import com.kiaijoe.sudoku.engine.tr
import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.kiaijoe.sudoku.MainActivity
import com.kiaijoe.sudoku.R
import com.kiaijoe.sudoku.data.GameRepository
import com.kiaijoe.sudoku.engine.GameStatus
import java.time.LocalDate
import java.time.ZoneId

/**
 * The home-screen widget: today's daily puzzle, the streak and this week.
 * Tapping the button opens today's puzzle; tapping anywhere else opens Sumi.
 *
 * It needs no permission. It is redrawn when the app starts or solves the
 * daily (see [refresh]), just after midnight by an inexact alarm, and at
 * least hourly by the system.
 */
class SumiWidget : AppWidgetProvider() {

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        val views = render(context)
        for (id in ids) manager.updateAppWidget(id, views)
        scheduleMidnight(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_REFRESH) refresh(context) else super.onReceive(context, intent)
    }

    override fun onDisabled(context: Context) = stopMidnightIfUnused(context)

    companion object {
        private const val ACTION_REFRESH = "com.kiaijoe.sudoku.widget.REFRESH"

        /** Extra on the launch intent that opens today's daily puzzle. */
        const val EXTRA_OPEN_DAILY = "open_daily"

        /** With [EXTRA_OPEN_DAILY], opens today's daily Killer instead. */
        const val EXTRA_KILLER = "killer"

        private val DAY_IDS = intArrayOf(
            R.id.widget_day0, R.id.widget_day1, R.id.widget_day2, R.id.widget_day3,
            R.id.widget_day4, R.id.widget_day5, R.id.widget_day6,
        )
        private val LETTER_IDS = intArrayOf(
            R.id.widget_letter0, R.id.widget_letter1, R.id.widget_letter2, R.id.widget_letter3,
            R.id.widget_letter4, R.id.widget_letter5, R.id.widget_letter6,
        )
        private val NUMBER_IDS = intArrayOf(
            R.id.widget_number0, R.id.widget_number1, R.id.widget_number2, R.id.widget_number3,
            R.id.widget_number4, R.id.widget_number5, R.id.widget_number6,
        )
        private val CHECK_IDS = intArrayOf(
            R.id.widget_check0, R.id.widget_check1, R.id.widget_check2, R.id.widget_check3,
            R.id.widget_check4, R.id.widget_check5, R.id.widget_check6,
        )

        /**
         * Redraws every Sumi widget on the home screen, the Daily puzzle and the
         * Streak ones. Cheap to call when there are none.
         */
        fun refresh(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val large = manager.getAppWidgetIds(ComponentName(context, SumiWidget::class.java))
            val small = manager.getAppWidgetIds(ComponentName(context, SumiStreakWidget::class.java))
            val killer = manager.getAppWidgetIds(ComponentName(context, SumiKillerWidget::class.java))
            if (large.isEmpty() && small.isEmpty() && killer.isEmpty()) return
            val model = model(context)
            if (large.isNotEmpty()) render(context, model).let { views -> for (id in large) manager.updateAppWidget(id, views) }
            if (small.isNotEmpty()) renderSmall(context, model).let { views -> for (id in small) manager.updateAppWidget(id, views) }
            if (killer.isNotEmpty()) {
                render(context, model(context, killer = true), killer = true).let { views ->
                    for (id in killer) manager.updateAppWidget(id, views)
                }
            }
            scheduleMidnight(context)
        }

        /** What the widgets show today, from what Sumi has saved: the classic daily, or the Killer one when [killer]. */
        fun model(context: Context, today: Long = LocalDate.now().toEpochDay(), killer: Boolean = false): DailyWidgetModel {
            val repo = GameRepository(context)
            val inProgress = repo.loadDailyGame(killer)
                ?.let { it.daily == today && it.status == GameStatus.PLAYING && it.isKiller == killer } == true
            return dailyWidgetModel(today, repo.loadDailyResults(killer), inProgress, killer = killer)
        }

        /** The Daily puzzle widget's views for today. */
        fun render(context: Context): RemoteViews = render(context, model(context))

        /** The 4×2 widget's views for [model]; the Killer one when [killer], whose button opens the daily Killer. */
        fun render(context: Context, model: DailyWidgetModel, killer: Boolean = false): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_daily)
            views.setTextViewText(R.id.widget_overline, model.overline)
            views.setTextViewText(R.id.widget_title, model.title)
            views.setViewVisibility(R.id.widget_streak_pill, if (model.streak > 0) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_streak, model.streak.toString())

            model.days.forEachIndexed { i, day ->
                views.setTextViewText(LETTER_IDS[i], day.letter)
                views.setTextViewText(NUMBER_IDS[i], day.number.toString())
                val solved = day.state == DayState.SOLVED
                views.setViewVisibility(NUMBER_IDS[i], if (solved) View.GONE else View.VISIBLE)
                views.setViewVisibility(CHECK_IDS[i], if (solved) View.VISIBLE else View.GONE)
                views.setInt(
                    DAY_IDS[i],
                    "setBackgroundResource",
                    when (day.state) {
                        DayState.SOLVED -> R.drawable.widget_day_solved
                        DayState.TODAY -> R.drawable.widget_day_today
                        else -> R.drawable.widget_day_other
                    },
                )
                views.setTextColor(
                    NUMBER_IDS[i],
                    context.getColor(if (day.state == DayState.TODAY) R.color.widget_ai else R.color.widget_ink_muted),
                )
            }

            views.setTextViewText(R.id.widget_action_text, model.action)
            if (model.solved) {
                // Done: a matcha pill with a tick and the solve time.
                views.setInt(R.id.widget_action, "setBackgroundResource", R.drawable.widget_pill_matcha_soft)
                views.setTextColor(R.id.widget_action_text, context.getColor(R.color.widget_matcha))
                views.setImageViewResource(R.id.widget_action_icon, R.drawable.ic_widget_check_matcha)
            } else {
                views.setInt(R.id.widget_action, "setBackgroundResource", R.drawable.widget_pill_ai)
                views.setTextColor(R.id.widget_action_text, context.getColor(R.color.widget_on_ai))
                views.setImageViewResource(R.id.widget_action_icon, R.drawable.ic_widget_play)
            }

            views.setContentDescription(android.R.id.background, model.description)
            views.setOnClickPendingIntent(android.R.id.background, launch(context, openDaily = false))
            // Once today is solved the button just opens Sumi, where the result is.
            views.setOnClickPendingIntent(R.id.widget_action, launch(context, openDaily = !model.solved, killer = killer))
            return views
        }

        /**
         * The 2×1 Streak widget: the streak with a flame (or, with no streak yet,
         * today's level), and a round Play button that becomes a tick once today
         * is solved.
         */
        fun renderSmall(context: Context, model: DailyWidgetModel): RemoteViews {
            val views = RemoteViews(context.packageName, R.layout.widget_streak)
            val streak = model.streak > 0
            views.setViewVisibility(R.id.widget_small_flame, if (streak) View.VISIBLE else View.GONE)
            views.setTextViewText(R.id.widget_small_value, if (streak) model.streak.toString() else model.title)
            views.setTextViewTextSize(R.id.widget_small_value, TypedValue.COMPLEX_UNIT_SP, if (streak) 22f else 17f)
            views.setTextViewText(R.id.widget_small_caption, if (streak) tr("day streak", "siku mfululizo") else tr("Daily puzzle", "Fumbo la leo"))
            if (model.solved) {
                views.setInt(R.id.widget_small_action, "setBackgroundResource", R.drawable.widget_circle_matcha_soft)
                views.setImageViewResource(R.id.widget_small_icon, R.drawable.ic_widget_check_matcha)
            } else {
                views.setInt(R.id.widget_small_action, "setBackgroundResource", R.drawable.widget_circle_ai)
                views.setImageViewResource(R.id.widget_small_icon, R.drawable.ic_widget_play)
            }
            views.setContentDescription(android.R.id.background, model.description)
            views.setContentDescription(
                R.id.widget_small_action,
                if (model.solved) {
                    tr("Open Sumi", "Fungua Sumi")
                } else {
                    tr("${model.action} today's puzzle", "${model.action} fumbo la leo")
                },
            )
            views.setOnClickPendingIntent(android.R.id.background, launch(context, openDaily = false))
            views.setOnClickPendingIntent(R.id.widget_small_action, launch(context, openDaily = !model.solved))
            return views
        }

        /** Stops the midnight redraw once neither kind of widget is on the home screen. */
        internal fun stopMidnightIfUnused(context: Context) {
            val manager = AppWidgetManager.getInstance(context) ?: return
            val any = manager.getAppWidgetIds(ComponentName(context, SumiWidget::class.java)).isNotEmpty() ||
                manager.getAppWidgetIds(ComponentName(context, SumiStreakWidget::class.java)).isNotEmpty() ||
                manager.getAppWidgetIds(ComponentName(context, SumiKillerWidget::class.java)).isNotEmpty()
            if (!any) context.getSystemService(AlarmManager::class.java)?.cancel(midnightIntent(context))
        }

        private fun launch(context: Context, openDaily: Boolean, killer: Boolean = false): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .setAction(
                    when {
                        !openDaily -> Intent.ACTION_MAIN
                        killer -> "com.kiaijoe.sudoku.OPEN_KILLER_DAILY"
                        else -> "com.kiaijoe.sudoku.OPEN_DAILY"
                    },
                )
                .putExtra(EXTRA_OPEN_DAILY, openDaily)
                .putExtra(EXTRA_KILLER, killer)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(
                context,
                when {
                    !openDaily -> 0
                    killer -> 2
                    else -> 1
                },
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private fun midnightIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, SumiWidget::class.java).setAction(ACTION_REFRESH),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        /**
         * Redraws a minute after the next midnight, so the new day shows. Inexact,
         * so it needs no alarm permission; the hourly update covers any delay.
         */
        internal fun scheduleMidnight(context: Context) {
            val alarms = context.getSystemService(AlarmManager::class.java) ?: return
            val zone = ZoneId.systemDefault()
            val next = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).plusMinutes(1).toInstant().toEpochMilli()
            alarms.set(AlarmManager.RTC, next, midnightIntent(context))
        }
    }
}
