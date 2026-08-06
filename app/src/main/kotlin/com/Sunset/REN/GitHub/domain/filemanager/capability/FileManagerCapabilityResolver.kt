package com.Sunset.REN.GitHub.domain.filemanager.capability

import com.Sunset.REN.GitHub.domain.filemanager.FileManagerEntry
import com.Sunset.REN.GitHub.domain.filemanager.path.FileManagerPath
import com.Sunset.REN.GitHub.domain.filemanager.root.RootAccessState

class FileManagerCapabilityResolver {
    fun resolveDirectoryActions(
        currentPath: FileManagerPath,
        rootState: RootAccessState,
        isDualPaneEnabled: Boolean,
        showHiddenFiles: Boolean
    ): FileManagerActionSet {
        val isArchive = currentPath is FileManagerPath.Archive
        val canWrite = currentPath !is FileManagerPath.Archive
        val rootGranted = rootState is RootAccessState.Granted
        fun action(id: FileManagerActionId, title: String, visible: Boolean = true, enabled: Boolean = true, reason: String? = null) =
            FileManagerActionUiModel(id = id, title = title, visible = visible, enabled = enabled, disabledReason = reason)
        return FileManagerActionSet(
            primary = listOf(
                action(FileManagerActionId.CreateDirectory, "新建", visible = canWrite, enabled = canWrite),
                action(FileManagerActionId.Search, "搜索", enabled = true),
                action(FileManagerActionId.DualPane, if (isDualPaneEnabled) "关闭双栏" else "开启双栏")
            ),
            overflow = listOf(
                action(FileManagerActionId.Refresh, "刷新"),
                action(FileManagerActionId.JumpPath, "跳转路径"),
                action(FileManagerActionId.Sort, "排序方式"),
                action(FileManagerActionId.ToggleHidden, if (showHiddenFiles) "隐藏隐藏文件" else "显示隐藏文件", visible = !isArchive),
                action(FileManagerActionId.AuthorizeSaf, "授权目录", visible = !isArchive),
                action(
                    FileManagerActionId.EditPermission,
                    "权限修改",
                    visible = rootGranted,
                    enabled = false,
                    reason = "Root 权限修改需要专用安全确认流程，当前版本仅提供风险说明。"
                ),
                action(
                    FileManagerActionId.EditOwner,
                    "所有者修改",
                    visible = rootGranted,
                    enabled = false,
                    reason = "Root 所有者修改需要专用安全确认流程，当前版本仅提供风险说明。"
                )
            ),
            selection = emptyList()
        )
    }

    fun resolveSelectionActions(entries: List<FileManagerEntry>, rootState: RootAccessState): List<FileManagerActionUiModel> {
        val caps = FileActionVisibilityPolicy.selectionCapabilities(entries)
        fun action(id: FileManagerActionId, title: String, visible: Boolean, enabled: Boolean = visible, reason: String? = null) =
            FileManagerActionUiModel(id = id, title = title, visible = visible, enabled = enabled, disabledReason = reason)
        return listOf(
            action(FileManagerActionId.Open, "打开", entries.size == 1 && caps.canOpen),
            action(FileManagerActionId.Extract, "解压到…", caps.canExtract),
            action(FileManagerActionId.Rename, "重命名", caps.canRename),
            action(FileManagerActionId.Copy, "复制", caps.canCopy),
            action(FileManagerActionId.Move, "移动到…", caps.canMove),
            action(FileManagerActionId.Compress, "压缩", caps.canCompress),
            action(FileManagerActionId.ConvertToText, "转换为 TXT", caps.canPreview && !FileActionVisibilityPolicy.isArchiveContext(entries)),
            action(FileManagerActionId.Delete, "删除", caps.canDelete),
            action(
                FileManagerActionId.EditPermission,
                "权限修改",
                rootState is RootAccessState.Granted && entries.isNotEmpty(),
                enabled = false,
                reason = "Root 权限修改尚未开放执行，仅可查看风险说明。"
            ),
            action(
                FileManagerActionId.EditOwner,
                "所有者修改",
                rootState is RootAccessState.Granted && entries.isNotEmpty(),
                enabled = false,
                reason = "Root 所有者修改尚未开放执行，仅可查看风险说明。"
            ),
            action(FileManagerActionId.Properties, "属性", entries.size == 1)
        )
    }
}
