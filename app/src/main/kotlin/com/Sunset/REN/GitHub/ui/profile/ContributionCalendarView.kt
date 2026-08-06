package com.Sunset.REN.GitHub.ui.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import com.Sunset.REN.GitHub.domain.profile.GitHubContributionCalendar
import java.util.Locale

class ContributionCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(87, 96, 106)
        textSize = 12f * density
    }
    private val squarePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val calendarBounds = RectF()
    private var calendar: GitHubContributionCalendar? = null

    fun submitCalendar(calendar: GitHubContributionCalendar?) {
        this.calendar = calendar
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val weeks = calendar?.weeks?.size ?: DefaultWeekCount
        val desiredWidth = (LabelWidthDp + weeks * (CellSizeDp + CellGapDp) + 8).dp
        val desiredHeight = 122.dp
        setMeasuredDimension(
            resolveSize(desiredWidth, widthMeasureSpec),
            resolveSize(desiredHeight, heightMeasureSpec)
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val currentCalendar = calendar ?: return
        val weeks = currentCalendar.weeks
        if (weeks.isEmpty()) return

        drawMonths(canvas, currentCalendar)
        drawWeekdayLabels(canvas)
        weeks.forEachIndexed { weekIndex, week ->
            val x = LabelWidthDp.dpFloat + weekIndex * (CellSizeDp + CellGapDp).dpFloat
            week.days.forEach { day ->
                val y = MonthLabelHeightDp.dpFloat + day.weekday * (CellSizeDp + CellGapDp).dpFloat
                squarePaint.color = resolveDayColor(day.color, day.contributionCount)
                calendarBounds.set(x, y, x + CellSizeDp.dpFloat, y + CellSizeDp.dpFloat)
                canvas.drawRoundRect(calendarBounds, 3.dpFloat, 3.dpFloat, squarePaint)
            }
        }
    }

    private fun drawMonths(canvas: Canvas, calendar: GitHubContributionCalendar) {
        var weekOffset = 0
        calendar.months.forEach { month ->
            if (month.totalWeeks <= 0) return@forEach
            val x = LabelWidthDp.dpFloat + weekOffset * (CellSizeDp + CellGapDp).dpFloat
            val label = month.name.take(3).replaceFirstChar { char ->
                if (char.isLowerCase()) char.titlecase(Locale.getDefault()) else char.toString()
            }
            canvas.drawText(label, x, 12.dpFloat, textPaint)
            weekOffset += month.totalWeeks
        }
    }

    private fun drawWeekdayLabels(canvas: Canvas) {
        val labels = mapOf(1 to "Mon", 3 to "Wed", 5 to "Fri")
        labels.forEach { (weekday, label) ->
            val y = MonthLabelHeightDp.dpFloat + weekday * (CellSizeDp + CellGapDp).dpFloat + 11.dpFloat
            canvas.drawText(label, 0f, y, textPaint)
        }
    }

    private fun resolveDayColor(color: String?, count: Int): Int {
        if (!color.isNullOrBlank()) {
            runCatching { Color.parseColor(color) }.getOrNull()?.let { return it }
        }
        return when {
            count <= 0 -> Color.rgb(235, 237, 240)
            count <= 2 -> Color.rgb(155, 233, 168)
            count <= 5 -> Color.rgb(64, 196, 99)
            count <= 10 -> Color.rgb(48, 161, 78)
            else -> Color.rgb(33, 110, 57)
        }
    }

    private val Int.dp: Int get() = (this * density).toInt()
    private val Int.dpFloat: Float get() = this * density

    private companion object {
        const val DefaultWeekCount = 53
        const val LabelWidthDp = 36
        const val MonthLabelHeightDp = 22
        const val CellSizeDp = 11
        const val CellGapDp = 4
    }
}