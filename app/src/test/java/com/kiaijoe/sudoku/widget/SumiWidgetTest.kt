package com.kiaijoe.sudoku.widget

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.github.takahirom.roborazzi.captureRoboImage
import com.kiaijoe.sudoku.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate
import java.util.Locale

/** Inflates the real widget the way a launcher does, checks it, and renders it to a screenshot. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class SumiWidgetTest {

    private val thursday = LocalDate.parse("2026-11-05").toEpochDay()
    private val results = mapOf(thursday - 3 to 300L, thursday - 2 to 320L, thursday - 1 to 290L)

    /**
     * The widget on a margin of wallpaper, at a typical home-screen size: 320 by
     * 140dp for the 4×2 Daily puzzle widget, 150 by 56dp for the 2×1 Streak one.
     */
    private fun show(model: DailyWidgetModel, small: Boolean = false): ViewGroup {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val density = activity.resources.displayMetrics.density
        val frame = FrameLayout(activity)
        frame.setPadding((16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt(), (16 * density).toInt())
        val (w, h) = if (small) 182 to 88 else 352 to 172
        activity.setContentView(frame, ViewGroup.LayoutParams((w * density).toInt(), (h * density).toInt()))
        val views = if (small) SumiWidget.renderSmall(activity, model) else SumiWidget.render(activity, model)
        val widget = views.apply(activity, frame)
        frame.addView(widget, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        return frame
    }

    private fun ViewGroup.text(id: Int) = findViewById<TextView>(id).text.toString()

    @Test
    fun playToday() {
        val frame = show(dailyWidgetModel(thursday, results, inProgress = false, locale = Locale.UK))
        assertEquals("DAILY · THURSDAY 5 NOVEMBER", frame.text(R.id.widget_overline))
        assertEquals("Medium", frame.text(R.id.widget_title))
        assertEquals("3", frame.text(R.id.widget_streak))
        assertEquals("Play", frame.text(R.id.widget_action_text))
        // Solved days show a tick instead of their number.
        assertEquals(View.VISIBLE, frame.findViewById<View>(R.id.widget_check0).visibility)
        assertEquals(View.GONE, frame.findViewById<View>(R.id.widget_number0).visibility)
        assertEquals("5", frame.text(R.id.widget_number3))
        frame.captureRoboImage("src/test/screenshots/widget_light.png")
    }

    @Test
    @Config(qualifiers = "+night")
    fun solvedTodayInTheDark() {
        val frame = show(dailyWidgetModel(thursday, results + (thursday to 312L), inProgress = false, locale = Locale.UK))
        assertEquals("05:12", frame.text(R.id.widget_action_text))
        assertEquals(View.VISIBLE, frame.findViewById<View>(R.id.widget_action_icon).visibility)
        frame.captureRoboImage("src/test/screenshots/widget_dark.png")
    }

    @Test
    fun noStreakHidesThePill() {
        val frame = show(dailyWidgetModel(thursday, emptyMap(), inProgress = true, locale = Locale.UK))
        assertEquals(View.GONE, frame.findViewById<View>(R.id.widget_streak_pill).visibility)
        assertEquals("Continue", frame.text(R.id.widget_action_text))
    }

    // --- The 2×1 Streak widget ------------------------------------------------

    @Test
    fun smallShowsTheStreakAndPlay() {
        val frame = show(dailyWidgetModel(thursday, results, inProgress = false, locale = Locale.UK), small = true)
        assertEquals("3", frame.text(R.id.widget_small_value))
        assertEquals("day streak", frame.text(R.id.widget_small_caption))
        assertEquals(View.VISIBLE, frame.findViewById<View>(R.id.widget_small_flame).visibility)
        assertEquals("Play today's puzzle", frame.findViewById<View>(R.id.widget_small_action).contentDescription)
        frame.captureRoboImage("src/test/screenshots/widget_small_light.png")
    }

    @Test
    @Config(qualifiers = "+night")
    fun smallSolvedInTheDark() {
        val frame = show(dailyWidgetModel(thursday, results + (thursday to 312L), inProgress = false, locale = Locale.UK), small = true)
        assertEquals("4", frame.text(R.id.widget_small_value))
        assertEquals("Open Sumi", frame.findViewById<View>(R.id.widget_small_action).contentDescription)
        frame.captureRoboImage("src/test/screenshots/widget_small_dark.png")
    }

    /** With no streak yet it shows today's level instead, and no flame. */
    @Test
    fun smallWithNoStreakShowsTodaysLevel() {
        val frame = show(dailyWidgetModel(thursday, emptyMap(), inProgress = false, locale = Locale.UK), small = true)
        assertEquals("Medium", frame.text(R.id.widget_small_value))
        assertEquals("Daily puzzle", frame.text(R.id.widget_small_caption))
        assertEquals(View.GONE, frame.findViewById<View>(R.id.widget_small_flame).visibility)
        frame.captureRoboImage("src/test/screenshots/widget_small_new_light.png")
    }
}
