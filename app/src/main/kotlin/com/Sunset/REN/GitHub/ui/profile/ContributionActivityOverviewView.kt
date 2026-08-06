package com.Sunset.REN.GitHub.ui.profile

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.Sunset.REN.GitHub.domain.profile.GitHubContributionOverview
import com.Sunset.REN.GitHub.R
import kotlin.math.max

class ContributionActivityOverviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val density = resources.displayMetrics.density
    private val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(26, 127, 55)
        strokeWidth = 2.5f * density
        strokeCap = Paint.Cap.ROUND
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }
    private val markerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(26, 127, 55)
        style = Paint.Style.STROKE
        strokeWidth = 2.5f * density
    }
    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.rgb(87, 96, 106)
        textSize = 13f * density
        textAlign = Paint.Align.CENTER
    }
    private var overview: GitHubContributionOverview = GitHubContributionOverview()

    fun submitOverview(overview: GitHubContributionOverview) {
        this.overview = overview
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        setMeasuredDimension(resolveSize(260.dp, widthMeasureSpec), resolveSize(148.dp, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val labelMetrics = labelPaint.fontMetrics
        val topLabelBaseline = 2.dpFloat - labelMetrics.ascent
        val bottomLabelBaseline = height - 2.dpFloat - labelMetrics.descent
        val topGraphBound = topLabelBaseline + labelMetrics.descent + 10.dpFloat
        val bottomGraphBound = bottomLabelBaseline + labelMetrics.ascent - 12.dpFloat
        val availableRadius = ((bottomGraphBound - topGraphBound) / 2f).coerceAtLeast(18.dpFloat)
        val centerY = (topGraphBound + bottomGraphBound) / 2f
        val radius = minOf(width * 0.22f, availableRadius, 42.dpFloat)
        val total = max(overview.totalCategorizedContributions, 1)
        val commitRatio = overview.commitCount.toFloat() / total
        val issueRatio = overview.issueCount.toFloat() / total
        val prRatio = overview.pullRequestCount.toFloat() / total
        val reviewRatio = overview.pullRequestReviewCount.toFloat() / total

        canvas.drawLine(centerX, centerY, centerX, centerY - radius * max(reviewRatio, 0.22f), axisPaint)
        canvas.drawLine(centerX, centerY, centerX + radius * max(issueRatio, 0.22f), centerY, axisPaint)
        canvas.drawLine(centerX, centerY, centerX, centerY + radius * max(prRatio, 0.22f), axisPaint)
        canvas.drawLine(centerX, centerY, centerX - radius * max(commitRatio, 0.22f), centerY, axisPaint)

        val markerX = centerX - radius * commitRatio
        canvas.drawCircle(markerX, centerY, 4.5f * density, markerPaint)
        canvas.drawCircle(markerX, centerY, 4.5f * density, markerStrokePaint)

        val commitPercent = if (overview.totalCategorizedContributions > 0) (commitRatio * 100).toInt() else 0
        canvas.drawText(context.getString(R.string.profile_activity_overview_code_review), centerX, topLabelBaseline, labelPaint)
        canvas.drawText(context.getString(R.string.profile_activity_overview_issues), centerX + radius + 38.dpFloat, centerY + 5.dpFloat, labelPaint)
        canvas.drawText(context.getString(R.string.profile_activity_overview_pull_requests), centerX, bottomLabelBaseline, labelPaint)
        canvas.drawText("${commitPercent}%", centerX - radius - 38.dpFloat, centerY - 10.dpFloat, labelPaint)
        canvas.drawText(context.getString(R.string.profile_activity_overview_commits), centerX - radius - 38.dpFloat, centerY + 10.dpFloat, labelPaint)
    }

    private val Int.dp: Int get() = (this * density).toInt()
    private val Int.dpFloat: Float get() = this * density
}
