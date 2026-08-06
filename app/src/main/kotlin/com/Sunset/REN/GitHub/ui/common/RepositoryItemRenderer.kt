package com.Sunset.REN.GitHub.ui.common

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.widget.ImageViewCompat
import com.Sunset.REN.GitHub.R
import com.Sunset.REN.GitHub.domain.repo.GitHubRepository
import com.Sunset.REN.GitHub.domain.repo.RepositoryLocalState
import com.Sunset.REN.GitHub.ui.applyMaterialBodyStyle
import com.Sunset.REN.GitHub.ui.applyMaterialMetaStyle
import com.Sunset.REN.GitHub.ui.applyMaterialRepositoryNameStyle
import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 仓库列表项渲染器。
 *
 * 把仓库卡片的视觉模板从具体页面里抽离，供仓库列表页（Dashboard）与全局搜索结果页共用，
 * 保证两处展示一致。点击与本地操作（置顶/收藏）通过回调注入：
 * - [onRepositoryClick] 必填，点击条目时回调。
 * - [onTogglePinned] / [onToggleFavorite] 可空；都为空时不渲染本地操作行（如全站搜索结果场景）。
 */
class RepositoryItemRenderer(
    private val context: Context,
    private val onRepositoryClick: (GitHubRepository) -> Unit,
    private val onTogglePinned: ((String) -> Unit)? = null,
    private val onToggleFavorite: ((String) -> Unit)? = null
) {

    fun createRepositoryItem(
        repository: GitHubRepository,
        localState: RepositoryLocalState = RepositoryLocalState()
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(
                dpToPx(RepositoryCardHorizontalPaddingDp),
                dpToPx(RepositoryCardVerticalPaddingDp),
                dpToPx(RepositoryCardHorizontalPaddingDp),
                dpToPx(RepositoryCardVerticalPaddingDp)
            )
            elevation = dpToPx(RepositoryCardElevationDp).toFloat()
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_repository_content_item_card)
            setOnClickListener { onRepositoryClick(repository) }
            addView(createRepositoryTitleView(repository))
            addView(createRepositoryDescriptionView(repository))
            if (isLocalActionsEnabled()) {
                addView(createRepositoryLocalActionsView(repository, localState))
            }
        }
    }
    fun createReusableRepositoryItem(): View {
        return ReusableRepositoryItemView().apply {
            if (!isLocalActionsEnabled()) {
                hideLocalActions()
            }
        }
    }

    fun bindReusableRepositoryItem(
        view: View,
        repository: GitHubRepository,
        localState: RepositoryLocalState = RepositoryLocalState(),
        currentAccountLogin: String = "",
        showFullNameTitle: Boolean = false,
        showOwnerInMeta: Boolean = true
    ) {
        (view as? ReusableRepositoryItemView)?.bind(
            repository = repository,
            localState = localState,
            currentAccountLogin = currentAccountLogin,
            showFullNameTitle = showFullNameTitle,
            showOwnerInMeta = showOwnerInMeta
        )
    }

    private fun isLocalActionsEnabled(): Boolean {
        return onTogglePinned != null || onToggleFavorite != null
    }


    private fun createRepositoryTitleView(repository: GitHubRepository): View {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                text = repository.fullName
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                applyMaterialRepositoryNameStyle()
            })
            addView(createRepositoryPillRow(buildRepositoryTitlePills(repository)))
        }
    }

    private fun createRepositoryDescriptionView(repository: GitHubRepository): TextView {
        return TextView(context).apply {
            text = buildRepositoryDescription(repository)
            applyMaterialBodyStyle()
            setPadding(0, dpToPx(RepositoryTextGapDp), 0, 0)
            minLines = RepositoryDescriptionLines
            maxLines = RepositoryDescriptionLines
            ellipsize = TextUtils.TruncateAt.END
        }
    }

    private fun createRepositoryLocalActionsView(
        repository: GitHubRepository,
        localState: RepositoryLocalState
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(RepositoryFooterGapDp), 0, 0)

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                buildRepositoryInlineMetaItems(repository).forEachIndexed { index, item ->
                    addView(createRepositoryInlineMetaItem(item, shouldExpand = index == 0))
                }
            })

            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                onTogglePinned?.let { toggle ->
                    addView(createRepositoryLocalActionButton(
                        iconRes = if (localState.isPinned) R.drawable.ic_pin_filled_24 else R.drawable.ic_pin_outline_24,
                        contentDescription = context.getString(
                            if (localState.isPinned) R.string.dashboard_unpin_action else R.string.dashboard_pin_action
                        ),
                        isActive = localState.isPinned,
                        onClick = { toggle(repository.fullName) }
                    ))
                }
                onToggleFavorite?.let { toggle ->
                    addView(createRepositoryLocalActionButton(
                        iconRes = if (localState.isFavorite) R.drawable.ic_star_filled_24 else R.drawable.ic_star_outline_24,
                        contentDescription = context.getString(
                            if (localState.isFavorite) R.string.dashboard_unfavorite_action else R.string.dashboard_favorite_action
                        ),
                        isActive = localState.isFavorite,
                        onClick = { toggle(repository.fullName) }
                    ))
                }
            })
        }
    }

    /**
     * 置顶/收藏图标按钮：矢量图标（实心=已选/描边=未选）+ 选中态浅色高亮背景表达状态。
     * 点击区域为正方形，避免误触。
     */
    private fun createRepositoryLocalActionButton(
        iconRes: Int,
        contentDescription: String,
        isActive: Boolean,
        onClick: () -> Unit
    ): ImageView {
        return ImageView(context).apply {
            setImageResource(iconRes)
            this.contentDescription = contentDescription
            isSelected = isActive
            setBackgroundResource(R.drawable.bg_local_action_button)
            val padding = dpToPx(RepositoryLocalActionButtonIconPaddingDp)
            setPadding(padding, padding, padding, padding)
            layoutParams = LinearLayout.LayoutParams(
                dpToPx(RepositoryLocalActionButtonSizeDp),
                dpToPx(RepositoryLocalActionButtonSizeDp)
            ).apply {
                marginEnd = dpToPx(RepositoryLocalActionButtonGapDp)
            }
            setOnClickListener { onClick() }
            ImageViewCompat.setImageTintList(
                this,
                ColorStateList.valueOf(
                    context.getColor(if (isActive) R.color.github_accent else R.color.github_text_secondary)
                )
            )
        }
    }

    private fun createRepositoryInlineMetaItem(
        item: RepositoryInlineMetaItem,
        shouldExpand: Boolean = false
    ): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                if (shouldExpand) 0 else LinearLayout.LayoutParams.WRAP_CONTENT,
                dpToPx(RepositoryLocalActionButtonSizeDp),
                if (shouldExpand) 1f else 0f
            ).apply {
                marginEnd = dpToPx(RepositoryInlineMetaGapDp)
            }
            addView(ImageView(context).apply {
                setImageResource(item.iconRes)
                ImageViewCompat.setImageTintList(this, ColorStateList.valueOf(item.tintColor))
                layoutParams = LinearLayout.LayoutParams(
                    dpToPx(RepositoryInlineMetaIconSizeDp),
                    dpToPx(RepositoryInlineMetaIconSizeDp)
                )
            })
            addView(TextView(context).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dpToPx(RepositoryInlineMetaTextGapDp) }
                text = item.label
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                applyMaterialMetaStyle()
            })
        }
    }

    private data class RepositoryInlineMetaItem(
        val iconRes: Int,
        val label: String,
        val tintColor: Int
    )

    private fun buildRepositoryTitlePills(repository: GitHubRepository): List<String> {
        return buildList {
            add(buildRepositoryVisibilityLabel(repository))
            buildRepositoryTypeLabels(repository).forEach(::add)
        }
    }

    private fun buildRepositoryInlineMetaItems(repository: GitHubRepository): List<RepositoryInlineMetaItem> {
        return buildList {
            buildRepositoryLanguageSummary(repository)?.let { language ->
                add(
                    RepositoryInlineMetaItem(
                        iconRes = R.drawable.ic_code_24,
                        label = language,
                        tintColor = LanguageColorPalette.colorFor(language.substringBefore(' '))
                    )
                )
            }
            add(
                RepositoryInlineMetaItem(
                    iconRes = R.drawable.ic_star_filled_24,
                    label = repository.stargazersCount.toString(),
                    tintColor = RepositoryStarTintColor
                )
            )
            add(
                RepositoryInlineMetaItem(
                    iconRes = R.drawable.ic_fork_24,
                    label = repository.forksCount.toString(),
                    tintColor = RepositoryMetaIconTintColor
                )
            )
        }
    }

    private fun createRepositoryPill(
        label: String,
        isProminent: Boolean,
        withStartMargin: Boolean = true
    ): TextView {
        return TextView(context).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                if (withStartMargin) marginStart = dpToPx(RepositoryPillGapDp)
            }
            setBackgroundResource(R.drawable.bg_pill)
            setPadding(dpToPx(RepositoryPillHorizontalPaddingDp), dpToPx(RepositoryPillVerticalPaddingDp), dpToPx(RepositoryPillHorizontalPaddingDp), dpToPx(RepositoryPillVerticalPaddingDp))
            text = label
            setTextColor(context.getColor(R.color.github_text_secondary))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, RepositoryPillTextSizeSp)
            includeFontPadding = false
            if (isProminent) {
                setTypeface(typeface, Typeface.BOLD)
            }
        }
    }

    private fun createRepositoryPillRow(labels: List<String>): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(RepositoryPillRowGapDp), 0, 0)
            labels.forEachIndexed { index, label ->
                addView(createRepositoryPill(label, isProminent = false, withStartMargin = index > 0))
            }
        }
    }

    fun buildRepositorySummaryMeta(repository: GitHubRepository): String {
        return buildString {
            append(buildRepositoryVisibilityLabel(repository))
            buildRepositoryTypeLabels(repository).forEach { label ->
                append(" · ")
                append(label)
            }
            val languageSummary = buildRepositoryLanguageSummary(repository)
            if (languageSummary != null) {
                append(" · 语言 ")
                append(languageSummary)
            }
            repository.defaultBranch.takeIf { it.isNotBlank() }?.let { defaultBranch ->
                append(" · 默认分支 ")
                append(defaultBranch)
            }
            append(" · Star ")
            append(repository.stargazersCount)
            append(" · 关注者 ")
            append(repository.watchersCount)
            append(" · 派生 ")
            append(repository.forksCount)
            append(" · 问题 ")
            append(repository.openIssuesCount)
        }
    }

    fun buildRepositoryLanguageSummary(repository: GitHubRepository): String? {
        if (repository.languages.isNotEmpty()) {
            return repository.languages
                .take(RepositoryLanguageSummaryLimit)
                .joinToString(separator = " / ") { language ->
                    "${language.name} ${language.percentage}%"
                }
        }
        return repository.language?.takeIf { it.isNotBlank() }
    }

    private fun buildRepositoryDescription(repository: GitHubRepository): String {
        return repository.description?.takeIf { it.isNotBlank() } ?: "暂无仓库描述。"
    }

    private fun buildRepositoryRelativeTimeLabel(repository: GitHubRepository): String {
        val timestamp = repository.pushedAt ?: repository.updatedAt
        return formatRelativeTime(timestamp)
    }

    private fun formatRelativeTime(timestamp: String?): String {
        val rawTimestamp = timestamp?.takeIf { it.isNotBlank() } ?: return "未知"
        return try {
            val eventTime = Instant.parse(rawTimestamp)
            val now = Instant.now()
            val zoneId = ZoneId.systemDefault()
            val elapsedMillis = ChronoUnit.MILLIS.between(eventTime, now)
            if (elapsedMillis < OneMinuteMillis) {
                return "刚刚"
            }

            val eventDate = eventTime.atZone(zoneId).toLocalDate()
            val today = LocalDate.now(zoneId)
            val days = ChronoUnit.DAYS.between(eventDate, today)
            when {
                days <= 0L -> "今天"
                days == 1L -> "昨天"
                days < DaysPerMonth -> "${days} 天前"
                days < DaysPerYear -> "${days / DaysPerMonth} 个月前"
                else -> "${days / DaysPerYear} 年前"
            }
        } catch (_: DateTimeException) {
            rawTimestamp
        }
    }

    fun buildRepositoryVisibilityLabel(repository: GitHubRepository): String {
        return if (repository.isPrivate) "私有" else "公开"
    }

    fun buildRepositoryTypeLabels(repository: GitHubRepository): List<String> {
        return buildList {
            if (repository.fork) add("派生仓库")
            if (repository.archived) add("已归档")
        }
    }

    fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            context.resources.displayMetrics
        ).toInt()
    }

    private fun resolveSelectableItemBackground(): Int {
        val typedValue = TypedValue()
        context.theme.resolveAttribute(android.R.attr.selectableItemBackground, typedValue, true)
        return typedValue.resourceId
    }

    private inner class ReusableRepositoryItemView : LinearLayout(context) {
        private val titleView = TextView(context).apply {
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f).apply {
                marginEnd = dpToPx(RepositoryWidgetTitleBadgeGapDp)
            }
            maxLines = 2
            ellipsize = TextUtils.TruncateAt.END
            applyMaterialRepositoryNameStyle()
        }
        private val starBadge = ReusableStarBadgeView()
        private val ownerMetaView = ReusableActionChipView(isPassive = true).apply {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                marginStart = dpToPx(RepositoryWidgetChipGapDp)
                marginEnd = dpToPx(RepositoryWidgetChipGapDp)
            }
        }
        private val chipSpacer = View(context).apply {
            layoutParams = LayoutParams(0, 1, 1f)
        }
        private val descriptionView = TextView(context).apply {
            applyMaterialBodyStyle()
            setPadding(0, dpToPx(RepositoryDescriptionGapDp), 0, 0)
            minLines = RepositoryDescriptionLines
            maxLines = RepositoryDescriptionLines
            ellipsize = TextUtils.TruncateAt.END
            setLineSpacing(dpToPx(RepositoryDescriptionLineSpacingDp).toFloat(), 1f)
        }
        private val languageBar = LinearLayout(context).apply {
            orientation = HORIZONTAL
            clipToOutline = true
            background = roundedDrawable(
                fillColor = context.getColor(R.color.github_language_other),
                strokeColor = context.getColor(R.color.github_language_other),
                radiusDp = RepositoryLanguageBarRadiusDp
            )
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, dpToPx(RepositoryLanguageBarHeightDp)).apply {
                topMargin = dpToPx(RepositoryLanguageBarTopGapDp)
            }
        }
        private val metaRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                topMargin = dpToPx(RepositoryWidgetMetaTopGapDp)
            }
        }
        private val languageMeta = ReusableTextMetaItemView(shouldExpand = true)
        private val forkMeta = ReusableTextMetaItemView()
        private val issueMeta = ReusableTextMetaItemView()
        private val chipRow = LinearLayout(context).apply {
            orientation = HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(RepositoryFooterGapDp), 0, 0)
        }
        private val pinChip = ReusableActionChipView()
        private val favoriteChip = ReusableActionChipView()
        private val updatedChip = ReusableActionChipView(isPassive = true)

        init {
            orientation = VERTICAL
            setPadding(
                dpToPx(RepositoryCardHorizontalPaddingDp),
                dpToPx(RepositoryCardVerticalPaddingDp),
                dpToPx(RepositoryCardHorizontalPaddingDp),
                dpToPx(RepositoryCardVerticalPaddingDp)
            )
            layoutParams = androidx.recyclerview.widget.RecyclerView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                bottomMargin = dpToPx(RepositoryCardSpacingDp)
            }
            elevation = dpToPx(RepositoryCardElevationDp).toFloat()
            isClickable = true
            isFocusable = true
            setBackgroundResource(R.drawable.bg_repository_content_item_card)

            addView(LinearLayout(context).apply {
                orientation = HORIZONTAL
                gravity = Gravity.TOP
                addView(titleView)
                addView(starBadge)
            })
            addView(descriptionView)
            addView(languageBar)
            metaRow.addView(languageMeta)
            metaRow.addView(forkMeta)
            metaRow.addView(issueMeta)
            addView(metaRow)
            chipRow.addView(pinChip)
            chipRow.addView(favoriteChip)
            chipRow.addView(ownerMetaView)
            chipRow.addView(chipSpacer)
            chipRow.addView(updatedChip)
            addView(chipRow)
        }

        fun hideLocalActions() {
            pinChip.visibility = GONE
            favoriteChip.visibility = GONE
            // 全站搜索等没有本地置顶/收藏语义的场景仍需要展示 owner/status/default branch。
            // 因此这里只隐藏本地动作 chip，而不是隐藏整行 chipRow。
        }

        fun bind(
            repository: GitHubRepository,
            localState: RepositoryLocalState,
            currentAccountLogin: String,
            showFullNameTitle: Boolean,
            showOwnerInMeta: Boolean
        ) {
            titleView.text = if (showFullNameTitle) {
                repository.fullName.ifBlank { repository.name }
            } else {
                repository.name.ifBlank { repository.fullName }
            }
            ownerMetaView.bind(
                label = buildWidgetOwnerMeta(repository, currentAccountLogin, showOwnerInMeta),
                isActive = false,
                onClick = null
            )
            descriptionView.text = buildRepositoryDescription(repository)
            starBadge.bind(repository.stargazersCount)
            bindLanguageBar(repository)
            bindMetaRow(repository)
            setOnClickListener { onRepositoryClick(repository) }

            if (isLocalActionsEnabled()) {
                chipRow.visibility = VISIBLE
                pinChip.visibility = VISIBLE
                favoriteChip.visibility = VISIBLE
                pinChip.bind(
                    label = if (localState.isPinned) "已置顶" else "置顶",
                    isActive = localState.isPinned,
                    onClick = { onTogglePinned?.invoke(repository.fullName) }
                )
                favoriteChip.bind(
                    label = if (localState.isFavorite) "已收藏" else "收藏",
                    isActive = localState.isFavorite,
                    onClick = { onToggleFavorite?.invoke(repository.fullName) }
                )
                updatedChip.bind(label = buildRepositoryRelativeTimeLabel(repository), isActive = false, onClick = null)
            }
        }

        private fun buildWidgetOwnerMeta(
            repository: GitHubRepository,
            currentAccountLogin: String,
            showOwnerInMeta: Boolean
        ): String {
            return buildList {
                if (showOwnerInMeta) {
                    val ownerLogin = repository.ownerLogin
                        .ifBlank { repository.fullName.substringBefore('/', "") }
                        .takeIf { owner ->
                            owner.isNotBlank() && !owner.equals(currentAccountLogin, ignoreCase = true)
                        }
                    ownerLogin?.let(::add)
                }
                add(if (repository.isPrivate) "private" else if (repository.fork) "fork" else "public")
                repository.defaultBranch.takeIf { it.isNotBlank() }?.let(::add)
            }.joinToString(separator = " · ")
        }

        private fun bindLanguageBar(repository: GitHubRepository) {
            languageBar.removeAllViews()
            val languages = repository.languages.filter { it.percentage > 0 }
            if (languages.isEmpty()) {
                val primaryLanguage = repository.language?.takeIf { it.isNotBlank() }
                if (primaryLanguage == null) {
                    languageBar.visibility = GONE
                    return
                }
                languageBar.visibility = VISIBLE
                addLanguageBarSegment(primaryLanguage, weight = 100f)
                return
            }
            languageBar.visibility = VISIBLE
            languages.take(RepositoryLanguageBarSegmentLimit).forEach { language ->
                addLanguageBarSegment(language.name, language.percentage.toFloat())
            }
        }

        private fun addLanguageBarSegment(languageName: String, weight: Float) {
            languageBar.addView(View(context).apply {
                layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight)
                setBackgroundColor(LanguageColorPalette.colorFor(languageName))
            })
        }


        private fun bindMetaRow(repository: GitHubRepository) {
            val primaryLanguage = repository.languages.firstOrNull()?.let { "${it.name} ${it.percentage}%" }
                ?: repository.language?.takeIf { it.isNotBlank() }
            if (primaryLanguage == null) {
                languageMeta.visibility = INVISIBLE
            } else {
                languageMeta.visibility = VISIBLE
                languageMeta.bind(
                    label = primaryLanguage,
                    tintColor = LanguageColorPalette.colorFor(primaryLanguage.substringBefore(' '))
                )
            }
            forkMeta.bind(label = "Fork ${repository.forksCount}", tintColor = context.getColor(R.color.github_text_muted))
            issueMeta.bind(label = "Issue ${repository.openIssuesCount}", tintColor = context.getColor(R.color.github_text_muted))
        }

        private fun roundedDrawable(fillColor: Int, strokeColor: Int, radiusDp: Int): GradientDrawable {
            return GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = dpToPx(radiusDp).toFloat()
                setColor(fillColor)
                setStroke(dpToPx(1), strokeColor)
            }
        }

        private inner class ReusableStarBadgeView : LinearLayout(context) {
            private val valueView = TextView(context).apply {
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(context.getColor(R.color.github_attention))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, RepositoryWidgetStarValueTextSizeSp)
            }
            private val labelView = TextView(context).apply {
                gravity = Gravity.CENTER
                text = "stars"
                maxLines = 1
                setTextColor(context.getColor(R.color.github_text_secondary))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, RepositoryWidgetStarLabelTextSizeSp)
            }

            init {
                orientation = VERTICAL
                gravity = Gravity.CENTER
                background = roundedDrawable(
                    fillColor = context.getColor(R.color.github_attention_soft),
                    strokeColor = context.getColor(R.color.github_attention_soft_border),
                    radiusDp = RepositoryWidgetStarBadgeRadiusDp
                )
                setPadding(dpToPx(RepositoryWidgetStarBadgePaddingDp), dpToPx(6), dpToPx(RepositoryWidgetStarBadgePaddingDp), dpToPx(6))
                layoutParams = LayoutParams(dpToPx(RepositoryWidgetStarBadgeWidthDp), LayoutParams.WRAP_CONTENT)
                addView(valueView)
                addView(labelView)
            }

            fun bind(stars: Int) {
                valueView.text = stars.toString()
            }
        }

        private inner class ReusableActionChipView(
            private val isPassive: Boolean = false
        ) : AppCompatTextView(context) {
            init {
                gravity = Gravity.CENTER
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setTextSize(TypedValue.COMPLEX_UNIT_SP, RepositoryWidgetChipTextSizeSp)
                setPadding(dpToPx(RepositoryWidgetChipHorizontalPaddingDp), dpToPx(RepositoryWidgetChipVerticalPaddingDp), dpToPx(RepositoryWidgetChipHorizontalPaddingDp), dpToPx(RepositoryWidgetChipVerticalPaddingDp))
                layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply {
                    marginEnd = dpToPx(RepositoryWidgetChipGapDp)
                }
            }

            fun bind(label: String, isActive: Boolean, onClick: (() -> Unit)?) {
                text = label
                isClickable = onClick != null
                isFocusable = onClick != null
                setOnClickListener(if (onClick == null) null else View.OnClickListener { onClick.invoke() })
                val active = isActive && !isPassive
                setTextColor(context.getColor(if (active) R.color.github_accent else R.color.github_text_secondary))
                background = roundedDrawable(
                    fillColor = context.getColor(if (active) R.color.github_accent_soft else R.color.github_chip_background),
                    strokeColor = context.getColor(if (active) R.color.github_accent_soft_border else R.color.github_divider),
                    radiusDp = RepositoryWidgetChipRadiusDp
                )
            }
        }

        private inner class ReusableTextMetaItemView(
            private val shouldExpand: Boolean = false
        ) : LinearLayout(context) {
            private val dotView = View(context).apply {
                layoutParams = LayoutParams(dpToPx(RepositoryWidgetMetaDotSizeDp), dpToPx(RepositoryWidgetMetaDotSizeDp))
            }
            private val labelView = TextView(context).apply {
                layoutParams = LayoutParams(
                    if (shouldExpand) 0 else LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                    if (shouldExpand) 1f else 0f
                ).apply {
                    marginStart = dpToPx(RepositoryInlineMetaTextGapDp)
                }
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                applyMaterialMetaStyle()
            }

            init {
                orientation = HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LayoutParams(
                    if (shouldExpand) 0 else LayoutParams.WRAP_CONTENT,
                    LayoutParams.WRAP_CONTENT,
                    if (shouldExpand) 1f else 0f
                ).apply {
                    marginEnd = dpToPx(RepositoryInlineMetaGapDp)
                }
                addView(dotView)
                addView(labelView)
            }

            fun bind(label: String, tintColor: Int) {
                labelView.text = label
                dotView.background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(tintColor)
                    setSize(dpToPx(RepositoryWidgetMetaDotSizeDp), dpToPx(RepositoryWidgetMetaDotSizeDp))
                }
            }
        }
    }

    companion object {
        const val RepositoryTextGapDp = 6
        const val RepositoryCardHorizontalPaddingDp = 18
        const val RepositoryCardVerticalPaddingDp = 16
        const val RepositoryCardSpacingDp = 8
        const val RepositoryCardElevationDp = 2
        const val RepositoryIconSizeDp = 48
        const val RepositoryIconTextGapDp = 12
        const val RepositoryIconTextSizeSp = 18f
        const val RepositoryOwnerMetaTopGapDp = 2
        const val RepositoryDescriptionGapDp = 10
        const val RepositoryDescriptionLineSpacingDp = 2
        const val RepositoryStatRowTopGapDp = 12
        const val RepositoryStatCellPaddingDp = 8
        const val RepositoryStatCellGapDp = 8
        const val RepositoryStatValueTextSizeSp = 16f
        const val RepositoryStatLabelTextSizeSp = 11f
        const val RepositoryFooterGapDp = 12
        const val RepositoryLocalActionButtonGapDp = 8
        const val RepositoryLocalActionButtonSizeDp = 34
        const val RepositoryLocalActionButtonIconPaddingDp = 7
        const val RepositoryInlineMetaGapDp = 10
        const val RepositoryInlineMetaIconSizeDp = 17
        const val RepositoryInlineMetaTextGapDp = 4
        const val RepositoryLanguageSummaryLimit = 2
        const val RepositoryDescriptionLines = 2
        const val RepositoryPillRowGapDp = 7
        const val RepositoryPillGapDp = 6
        const val RepositoryPillHorizontalPaddingDp = 8
        const val RepositoryPillVerticalPaddingDp = 3
        const val RepositoryPillTextSizeSp = 11f
        const val RepositoryWidgetTitleBadgeGapDp = 12
        const val RepositoryWidgetStarBadgeWidthDp = 72
        const val RepositoryWidgetStarBadgePaddingDp = 10
        const val RepositoryWidgetStarBadgeRadiusDp = 14
        const val RepositoryWidgetStarValueTextSizeSp = 20f
        const val RepositoryWidgetStarLabelTextSizeSp = 11f
        const val RepositoryLanguageBarHeightDp = 8
        const val RepositoryLanguageBarTopGapDp = 12
        const val RepositoryLanguageBarRadiusDp = 4
        const val RepositoryLanguageBarSegmentLimit = 4
        const val RepositoryWidgetMetaTopGapDp = 10
        const val RepositoryWidgetMetaDotSizeDp = 8
        const val RepositoryWidgetChipHorizontalPaddingDp = 10
        const val RepositoryWidgetChipVerticalPaddingDp = 4
        const val RepositoryWidgetChipGapDp = 8
        const val RepositoryWidgetChipRadiusDp = 14
        const val RepositoryWidgetChipTextSizeSp = 12f
        const val OneMinuteMillis = 60_000L
        const val DaysPerMonth = 30L
        const val DaysPerYear = 365L
        val RepositoryMetaIconTintColor = 0xFF6E7781.toInt()
        val RepositoryStarTintColor = 0xFF9A6700.toInt()
    }
}