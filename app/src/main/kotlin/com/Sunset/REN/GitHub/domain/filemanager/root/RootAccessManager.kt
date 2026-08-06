package com.Sunset.REN.GitHub.domain.filemanager.root

class RootAccessManager(
    private val runner: RootCommandRunner = RootCommandRunner()
) {
    private var cachedState: RootAccessState = RootAccessState.Unknown

    fun currentState(): RootAccessState = cachedState

    fun updateSettings(settings: RootAccessSettings) {
        runner.updateSuCommand(settings.normalized().suCommand)
        cachedState = RootAccessState.Unknown
    }

    suspend fun detect(): RootAccessState {
        cachedState = if (runner.hasSuBinary()) {
            RootAccessState.AvailableButNotGranted
        } else {
            RootAccessState.NotAvailable
        }
        return cachedState
    }

    suspend fun verifyGranted(): RootAccessState {
        if (!runner.hasSuBinary()) {
            cachedState = RootAccessState.NotAvailable
            return cachedState
        }
        cachedState = runner.run("id", timeoutMillis = 1500L).fold(
            onSuccess = { output ->
                if (output.contains("uid=0") || output.contains("root")) {
                    RootAccessState.Granted
                } else {
                    RootAccessState.AvailableButNotGranted
                }
            },
            onFailure = { RootAccessState.AvailableButNotGranted }
        )
        return cachedState
    }

    suspend fun requestAccess(): RootAccessState {
        cachedState = RootAccessState.Requesting
        if (!runner.hasSuBinary()) {
            cachedState = RootAccessState.NotAvailable
            return cachedState
        }
        cachedState = runner.run("id", timeoutMillis = 8000L).fold(
            onSuccess = { output ->
                if (output.contains("uid=0") || output.contains("root")) RootAccessState.Granted else RootAccessState.Denied(output.ifBlank { "Root authorization was not granted" })
            },
            onFailure = { error -> RootAccessState.Denied(error.message ?: "Root authorization failed") }
        )
        return cachedState
    }

    fun revokeForUiSession() {
        cachedState = RootAccessState.AvailableButNotGranted
    }
}
