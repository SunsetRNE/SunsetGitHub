package com.Sunset.REN.GitHub.domain.repo

/**
 * 文件写入提交前校验结果。
 */
data class PreSubmitValidationResult(
    val canSubmit: Boolean,
    val errors: List<String> = emptyList()
) {
    companion object {
        val Success = PreSubmitValidationResult(canSubmit = true)
    }
}

/**
 * 提交前校验器。第一阶段在真正请求 GitHub API 前统一拦截明显错误。
 */
interface PreSubmitValidator {
    fun validate(session: FileWriteSession): PreSubmitValidationResult
}

class DefaultPreSubmitValidator : PreSubmitValidator {
    override fun validate(session: FileWriteSession): PreSubmitValidationResult {
        val errors = buildList {
            val normalizedTargetPath = session.targetPath.trim()
            val pathSegments = normalizedTargetPath.split('/')
            if (session.owner.isBlank()) add("仓库 owner 为空，无法提交。")
            if (session.repo.isBlank()) add("仓库名称为空，无法提交。")
            if (normalizedTargetPath.isBlank()) add("目标路径为空，无法提交。")
            if (normalizedTargetPath.startsWith('/')) add("目标路径不能以 / 开头。")
            if (normalizedTargetPath.endsWith('/')) add("目标路径不能以 / 结尾。")
            if (normalizedTargetPath.contains('\\')) add("目标路径不能包含反斜杠。")
            if (pathSegments.any { it.isBlank() }) add("目标路径不能包含连续的 /。")
            if (pathSegments.any { it == "." || it == ".." }) add("目标路径不能包含 . 或 .. 路径片段。")
            if (session.commitMessage.isBlank()) add("提交说明为空，无法提交。")
            if (session.selectedFiles.size > 1) add("第一阶段一次只能提交一个文件。")
            session.selectedFiles.firstOrNull()?.sizeBytes?.let { sizeBytes ->
                if (sizeBytes > GitHubContentApiLimits.KnownSingleContentMaxBytes) {
                    add("文件大小超过 GitHub Contents API 当前已知限制，无法通过该接口提交。")
                } else if (
                    session.operation == FileWriteOperation.Upload &&
                    sizeBytes > GitHubContentApiLimits.RecommendedDirectUploadMaxBytes
                ) {
                    add("文件超过 50 MiB，不建议通过应用内直接上传。请改用 GitHub Release 附件或 Git LFS 上传大型软件包。")
                }
            }

            when (session.operation) {
                FileWriteOperation.Edit, FileWriteOperation.Overwrite -> {
                    if (session.baseSha.isNullOrBlank()) add("缺少远端文件 sha，无法安全更新。")
                    if (!session.capability.canEdit && session.operation == FileWriteOperation.Edit) {
                        add(session.capability.reason ?: "当前文件不可在线编辑。")
                    }
                }

                FileWriteOperation.Create -> {
                    if (!session.capability.canCreate) add(session.capability.reason ?: "当前文件不可新建。")
                }

                FileWriteOperation.Upload -> {
                    if (!session.capability.canUpload) add(session.capability.reason ?: "当前文件不可上传。")
                    if (session.selectedFiles.isEmpty() && session.content == null) add("缺少待上传文件内容。")
                }
            }
        }
        return if (errors.isEmpty()) {
            PreSubmitValidationResult.Success
        } else {
            PreSubmitValidationResult(canSubmit = false, errors = errors)
        }
    }
}

fun PreSubmitValidationResult.toDisplayMessage(): String {
    return errors.joinToString(separator = "\n")
}