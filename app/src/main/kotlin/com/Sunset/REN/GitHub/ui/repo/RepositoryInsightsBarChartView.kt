package com.Sunset.REN.GitHub.ui.repo

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import com.Sunset.REN.GitHub.R
import kotlin.math.max

class RepositoryInsightsBarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * resources.displayMetrics.density
    }
    private val trackRect = RectF()
    private val barRect = RectF()
    private val touchRects = MutableList(DefaultBarCount) { RectF() }
    private val barHeights = MutableList(DefaultBarCount) { DefaultHeightPercent }
    private val targetHeights = MutableList(DefaultBarCount) { DefaultHeightPercent }
    private val barKinds = MutableList(DefaultBarCount) { RepositoryInsightsChartBarKind.Blue }
    private val barLabels = MutableList(DefaultBarCount) { "" }
    private var animator: ValueAnimator? = null
    private var selectedBarIndex: Int? = null

    private val density: Float get() = resources.displayMetrics.density
    private val cornerRadius: Float get() = 14f * density
    private val barRadius: Float get() = 8f * density
    private val horizontalPadding: Float get() = 10f * density
    private val topPadding: Float get() = 12f * density
    private val bottomPadding: Float get() = 10f * density
    private val gap: Float get() = 6f * density

    init {
        isSaveEnabled = true
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        isClickable = true
        isFocusable = true
        trackPaint.color = ContextCompat.getColor(context, R.color.github_subtle_background)
        highlightPaint.color = ContextCompat.getColor(context, R.color.github_text_primary)
    }

    fun submitData(
        heights: List<Int>,
        kinds: List<RepositoryInsightsChartBarKind>,
        labels: List<String> = emptyList(),
        animate: Boolean = true
    ) {
        val normalizedHeights = heights.normalizedHeights()
        val normalizedKinds = kinds.normalizedKinds()
        val normalizedLabels = labels.normalizedLabels(heights)
        targetHeights.replaceFloatValues(normalizedHeights)
        barKinds.replaceKindValues(normalizedKinds)
        barLabels.replaceStringValues(normalizedLabels)
        contentDescription = normalizedLabels.joinToString(separator = "; ")
        animator?.cancel()
        if (!animate || !isLaidOut) {
            barHeights.replaceFloatValues(normalizedHeights)
            invalidate()
            return
        }
        val startHeights = barHeights.toList()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = ChartAnimationDurationMillis
            interpolator = DecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                val fraction = valueAnimator.animatedFraction
                barHeights.indices.forEach { index ->
                    val start = startHeights[index]
                    val end = targetHeights[index]
                    barHeights[index] = start + ((end - start) * fraction)
                }
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        animator?.cancel()
        animator = null
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val availableWidth = width.toFloat()
        val availableHeight = height.toFloat()
        if (availableWidth <= 0f || availableHeight <= 0f) return

        trackRect.set(0f, 0f, availableWidth, availableHeight)
        canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint)

        val chartLeft = horizontalPadding
        val chartRight = availableWidth - horizontalPadding
        val chartTop = topPadding
        val chartBottom = availableHeight - bottomPadding
        val chartHeight = max(1f, chartBottom - chartTop)
        val count = barHeights.size
        val totalGap = gap * (count - 1).coerceAtLeast(0)
        val barWidth = max(2f * density, (chartRight - chartLeft - totalGap) / count)

        barHeights.forEachIndexed { index, percent ->
            val left = chartLeft + index * (barWidth + gap)
            val right = left + barWidth
            val barHeight = chartHeight * (percent.coerceIn(MinHeightPercent, MaxHeightPercent) / 100f)
            val top = chartBottom - barHeight
            barPaint.color = colorFor(barKinds.getOrElse(index) { RepositoryInsightsChartBarKind.Blue })
            barRect.set(left, top, right, chartBottom)
            canvas.drawRoundRect(barRect, barRadius, barRadius, barPaint)
            touchRects[index].set(left - gap / 2f, chartTop, right + gap / 2f, chartBottom)
            if (selectedBarIndex == index) {
                val highlightInset = 2f * density
                barRect.inset(-highlightInset, -highlightInset)
                canvas.drawRoundRect(barRect, barRadius + highlightInset, barRadius + highlightInset, highlightPaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                true
            }
            MotionEvent.ACTION_UP -> {
                val index = touchRects.indexOfFirst { it.contains(event.x, event.y) }
                if (index >= 0) {
                    selectedBarIndex = index
                    showBarTooltip(index)
                    performClick()
                    invalidate()
                }
                parent?.requestDisallowInterceptTouchEvent(false)
                true
            }
            MotionEvent.ACTION_CANCEL -> {
                parent?.requestDisallowInterceptTouchEvent(false)
                true
            }
            else -> true
        }
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun showBarTooltip(index: Int) {
        val label = barLabels.getOrElse(index) { "" }.ifBlank { "Bar ${index + 1}" }
        Toast.makeText(context, label, Toast.LENGTH_SHORT).show()
        contentDescription = label
        sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_FOCUSED)
    }

    @ColorInt
    private fun colorFor(kind: RepositoryInsightsChartBarKind): Int = ContextCompat.getColor(
        context,
        when (kind) {
            RepositoryInsightsChartBarKind.Blue -> R.color.github_accent
            RepositoryInsightsChartBarKind.Green -> R.color.github_success
            RepositoryInsightsChartBarKind.Amber -> R.color.github_attention
        }
    )

    private fun List<Int>.normalizedHeights(): List<Float> = List(DefaultBarCount) { index ->
        getOrElse(index) { DefaultHeightPercent.toInt() }.toFloat().coerceIn(MinHeightPercent, MaxHeightPercent)
    }

    private fun List<RepositoryInsightsChartBarKind>.normalizedKinds(): List<RepositoryInsightsChartBarKind> =
        List(DefaultBarCount) { index -> getOrElse(index) { RepositoryInsightsChartBarKind.Blue } }

    private fun List<String>.normalizedLabels(heights: List<Int>): List<String> = List(DefaultBarCount) { index ->
        getOrElse(index) { "Bar ${index + 1}: ${heights.getOrElse(index) { DefaultHeightPercent.toInt() }}%" }
    }

    private fun MutableList<Float>.replaceFloatValues(values: List<Float>) {
        clear()
        addAll(values)
    }

    private fun MutableList<RepositoryInsightsChartBarKind>.replaceKindValues(values: List<RepositoryInsightsChartBarKind>) {
        clear()
        addAll(values)
    }

    private fun MutableList<String>.replaceStringValues(values: List<String>) {
        clear()
        addAll(values)
    }

    private companion object {
        private const val DefaultBarCount = 8
        private const val DefaultHeightPercent = 18f
        private const val MinHeightPercent = 4f
        private const val MaxHeightPercent = 100f
        private const val ChartAnimationDurationMillis = 360L
    }
}
