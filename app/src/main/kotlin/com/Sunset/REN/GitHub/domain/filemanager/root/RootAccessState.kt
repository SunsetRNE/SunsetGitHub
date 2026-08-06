package com.Sunset.REN.GitHub.domain.filemanager.root

sealed class RootAccessState {
    data object Unknown : RootAccessState()
    data object NotAvailable : RootAccessState()
    data object AvailableButNotGranted : RootAccessState()
    data object Requesting : RootAccessState()
    data object Granted : RootAccessState()
    data class Denied(val reason: String) : RootAccessState()
    data class Error(val message: String) : RootAccessState()
}

fun RootAccessState.isGranted(): Boolean = this is RootAccessState.Granted
fun RootAccessState.canShowEnableEntry(): Boolean = this is RootAccessState.AvailableButNotGranted || this is RootAccessState.Denied || this is RootAccessState.Error
