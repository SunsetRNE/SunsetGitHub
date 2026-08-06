package com.Sunset.REN.GitHub.domain.filemanager.operation

import com.Sunset.REN.GitHub.domain.filemanager.path.FileManagerPath
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import java.util.UUID

@JvmInline
value class FileOperationId(val value: String) {
    companion object { fun newId(): FileOperationId = FileOperationId(UUID.randomUUID().toString()) }
}

data class FileOperationContext(
    val sources: List<FileManagerPath>,
    val target: FileManagerPath? = null,
    val title: String = "文件操作"
)

sealed interface FileOperationValidation {
    data object Valid : FileOperationValidation
    data class Invalid(val message: String) : FileOperationValidation
}

sealed class FileOperationEvent {
    data class Started(val title: String) : FileOperationEvent()
    data class Progress(val current: Long, val total: Long?, val message: String) : FileOperationEvent()
    data class ConflictDetected(val source: FileManagerPath, val target: FileManagerPath) : FileOperationEvent()
    data class Completed(val summary: String) : FileOperationEvent()
    data class Failed(val message: String, val throwable: Throwable? = null) : FileOperationEvent()
    data object Cancelled : FileOperationEvent()
}

interface FileOperation {
    val id: FileOperationId
    val title: String
    fun validate(context: FileOperationContext): FileOperationValidation
    suspend fun execute(context: FileOperationContext): Flow<FileOperationEvent>
}

class FileOperationRunner {
    private val _events = MutableSharedFlow<FileOperationEvent>(extraBufferCapacity = 32)
    val events = _events.asSharedFlow()

    suspend fun run(operation: FileOperation, context: FileOperationContext): Flow<FileOperationEvent> = flow {
        when (val validation = operation.validate(context)) {
            FileOperationValidation.Valid -> {
                emit(FileOperationEvent.Started(operation.title))
                emitAll(operation.execute(context))
            }
            is FileOperationValidation.Invalid -> emit(FileOperationEvent.Failed(validation.message))
        }
    }

    suspend fun publish(event: FileOperationEvent) {
        _events.emit(event)
    }
}
