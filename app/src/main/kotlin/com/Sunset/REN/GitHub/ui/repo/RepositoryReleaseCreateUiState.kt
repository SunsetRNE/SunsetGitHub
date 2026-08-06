package com.Sunset.REN.GitHub.ui.repo

import android.net.Uri
import com.Sunset.REN.GitHub.domain.repo.RepositoryBranch

/** 创建发布版本页状态。 */
data class RepositoryReleaseCreateUiState(
    val owner: String = "",
    val repo: String = "",
    val isSubmitting: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val branches: List<RepositoryBranch> = emptyList(),
    val selectedBranchName: String = "",
    val isLoadingBranches: Boolean = false,
    val branchErrorMessage: String? = null,
    val previousTagName: String? = null,
    val isLoadingPreviousTag: Boolean = false,
    val assets: List<RepositoryReleaseAssetDraft> = emptyList(),
    /** 创建成功后的 tag 名，用于通知上一页并返回。 */
    val createdTagName: String? = null
)

data class RepositoryReleaseAssetDraft(
    val uri: Uri,
    val fileName: String,
    val mimeType: String,
    val sizeBytes: Long
)