package com.Sunset.REN.GitHub.domain.filemanager.root

object RootPromptPolicy {
    fun shouldShowRootEntry(state: RootAccessState): Boolean = when (state) {
        RootAccessState.Unknown -> false
        RootAccessState.NotAvailable -> false
        RootAccessState.Requesting -> true
        is RootAccessState.AvailableButNotGranted,
        is RootAccessState.Granted,
        is RootAccessState.Denied,
        is RootAccessState.Error -> true
    }
}
