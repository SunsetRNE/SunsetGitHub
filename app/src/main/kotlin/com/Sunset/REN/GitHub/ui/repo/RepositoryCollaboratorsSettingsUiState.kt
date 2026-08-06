package com.Sunset.REN.GitHub.ui.repo

import com.Sunset.REN.GitHub.data.github.html.RepositoryCollaboratorInvitationItem
import com.Sunset.REN.GitHub.data.github.html.RepositoryCollaboratorPermission
import com.Sunset.REN.GitHub.data.github.html.RepositoryCollaboratorsSnapshot

data class RepositoryCollaboratorsSettingsUiState(
    val owner: String = "",
    val repo: String = "",
    val snapshot: RepositoryCollaboratorsSnapshot? = null,
    val selectedLogin: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val pendingMessage: String? = null,
    val sourceUrl: String? = null,
    val isShowingStaleContent: Boolean = false
) {
    val isInitialLoad: Boolean get() = snapshot == null && isLoading
    val collaborators: List<RepositoryCollaboratorRow> get() = snapshot?.toRows().orEmpty()
    val invitations: List<RepositoryCollaboratorInvitationRow> get() = snapshot?.invitations?.toRows().orEmpty()
}

data class RepositoryCollaboratorRow(
    val login: String,
    val permission: RepositoryCollaboratorPermission,
    val permissionLabel: String,
    val avatarUrl: String,
    val htmlUrl: String,
    val canEdit: Boolean
)

data class RepositoryCollaboratorInvitationRow(
    val id: Long,
    val displayName: String,
    val permissionLabel: String,
    val createdAt: String,
    val url: String
)

fun List<RepositoryCollaboratorInvitationItem>.toRows(): List<RepositoryCollaboratorInvitationRow> {
    return sortedWith(compareBy<RepositoryCollaboratorInvitationItem> { it.login.ifBlank { it.email }.lowercase() })
        .map { invitation ->
            RepositoryCollaboratorInvitationRow(
                id = invitation.id,
                displayName = invitation.login.ifBlank { invitation.email.ifBlank { "#${invitation.id}" } },
                permissionLabel = invitation.permission.displayName,
                createdAt = invitation.createdAt,
                url = invitation.url
            )
        }
}

fun RepositoryCollaboratorsSnapshot.toRows(): List<RepositoryCollaboratorRow> {
    return collaborators
        .sortedWith(compareByDescending<com.Sunset.REN.GitHub.data.github.html.RepositoryCollaboratorItem> { it.permission.rank }.thenBy { it.login.lowercase() })
        .map { collaborator ->
            RepositoryCollaboratorRow(
                login = collaborator.login,
                permission = collaborator.permission,
                permissionLabel = collaborator.permission.displayName,
                avatarUrl = collaborator.avatarUrl,
                htmlUrl = collaborator.htmlUrl,
                canEdit = canAdmin
            )
        }
}

private val RepositoryCollaboratorPermission.rank: Int
    get() = when (this) {
        RepositoryCollaboratorPermission.Admin -> 5
        RepositoryCollaboratorPermission.Maintain -> 4
        RepositoryCollaboratorPermission.Push -> 3
        RepositoryCollaboratorPermission.Triage -> 2
        RepositoryCollaboratorPermission.Pull -> 1
    }
