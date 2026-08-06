package com.Sunset.REN.GitHub.ui.common

import androidx.annotation.DrawableRes

/** 文件来源项，用于自定义选择弹窗。 */
data class FileSourceUiModel(
    val label: String,
    val packageName: String,
    val intent: android.content.Intent,
    @DrawableRes val iconRes: Int,
    val isRecommended: Boolean,
    val groupLabel: String
)