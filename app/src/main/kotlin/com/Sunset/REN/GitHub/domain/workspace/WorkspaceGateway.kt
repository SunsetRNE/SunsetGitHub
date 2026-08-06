package com.Sunset.REN.GitHub.domain.workspace

/**
 * 应用内部工作区边界。
 * 具体实现负责把文件限制在 App 私有目录，避免 UI 或同步层直接操作外部路径。
 */
interface WorkspaceGateway {
    suspend fun listWorkspaces(): List<WorkspaceProject>

    suspend fun getWorkspace(workspaceId: String): WorkspaceProject?

    suspend fun createWorkspace(name: String): WorkspaceProject

    suspend fun renameWorkspace(workspaceId: String, name: String): WorkspaceProject

    suspend fun deleteWorkspace(workspaceId: String)

    suspend fun bindRemote(workspaceId: String, binding: WorkspaceRemoteBinding): WorkspaceProject

    suspend fun clearRemoteBinding(workspaceId: String): WorkspaceProject

    suspend fun importIntoWorkspace(request: WorkspaceImportRequest): WorkspaceImportResult

    suspend fun scanWorkspace(
        workspaceId: String,
        options: WorkspaceScanOptions = WorkspaceScanOptions()
    ): WorkspaceScanResult

    suspend fun getLatestSnapshot(workspaceId: String): WorkspaceSnapshot?

    suspend fun saveSnapshot(snapshot: WorkspaceSnapshot)
}