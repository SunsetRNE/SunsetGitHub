package com.Sunset.REN.GitHub.ui.common

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.OpenableColumns
import com.Sunset.REN.GitHub.R
import java.io.File
import java.io.FileInputStream
import java.util.UUID

/**
 * Shared file picking helpers for repository uploads and release assets.
 * Prefers the system document picker while keeping third-party file managers available.
 */
object LocalFilePicker {
    private val knownFileManagerPackages = listOf(
        "bin.mt.plus",
        "bin.mt.plus.canary",
        "bin.mt.plus.x",
        "bin.mt.plus.apk",
        "player.normal.np",
        "com.np.manager",
        "com.wn.app.np"
    )
    private const val SystemFileSourceKey = "android.system.file_picker"
    private const val FallbackDisplayName = "selected-file"

    fun createChooser(context: Context, title: String): Intent? {
        val sources = getFileSources(context)
        val primaryIntent = createSystemFileIntent().takeIf {
            it.resolveActivity(context.packageManager) != null
        } ?: sources.firstOrNull()?.intent ?: return null
        val alternateIntents = sources
            .map { it.intent }
            .filterNot { it.filterEquals(primaryIntent) }
            .toTypedArray()
        return Intent.createChooser(primaryIntent, title).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, alternateIntents)
        }
    }

    fun getFileSources(context: Context): List<FileSourceUiModel> {
        val packageManager = context.packageManager
        val sources = linkedMapOf<String, FileSourceUiModel>()

        createSystemFileIntent().takeIf { it.resolveActivity(packageManager) != null }?.let { systemIntent ->
            sources[SystemFileSourceKey] = FileSourceUiModel(
                label = context.getString(R.string.file_source_picker_system_default),
                packageName = SystemFileSourceKey,
                intent = systemIntent,
                iconRes = R.drawable.ic_home_black_24dp,
                isRecommended = true,
                groupLabel = context.getString(R.string.file_source_picker_group_system)
            )
        }

        knownFileManagerPackages.forEach { knownPackage ->
            if (!isPackageInstalled(packageManager, knownPackage)) return@forEach
            sources[knownPackage] = FileSourceUiModel(
                label = resolveApplicationLabel(packageManager, knownPackage) ?: knownPackage,
                packageName = knownPackage,
                intent = createPackageIntent(knownPackage),
                iconRes = R.drawable.ic_dashboard_black_24dp,
                isRecommended = false,
                groupLabel = context.getString(R.string.file_source_picker_group_third_party)
            )
        }

        val baseIntent = createBaseIntent()
        packageManager.queryIntentActivities(baseIntent, 0).forEach { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@forEach
            val packageName = activityInfo.packageName
            if (packageName in sources) return@forEach
            val systemPackage = isSystemPackage(packageManager, packageName)
            sources[packageName] = FileSourceUiModel(
                label = resolveInfo.loadLabel(packageManager)?.toString().orEmpty().ifBlank { packageName },
                packageName = packageName,
                intent = Intent(baseIntent).apply {
                    component = ComponentName(packageName, activityInfo.name)
                    setPackage(packageName)
                },
                iconRes = if (systemPackage) R.drawable.ic_home_black_24dp else R.drawable.ic_dashboard_black_24dp,
                isRecommended = false,
                groupLabel = if (systemPackage) {
                    context.getString(R.string.file_source_picker_group_system)
                } else {
                    context.getString(R.string.file_source_picker_group_other)
                }
            )
        }

        return sources.values.sortedWith(
            compareBy<FileSourceUiModel> { sourceSortGroup(it.packageName) }
                .thenBy { it.label.lowercase() }
        )
    }

    private fun createSystemFileIntent(): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
    }

    private fun createPackageIntent(packageName: String): Intent {
        return createBaseIntent().apply {
            setPackage(packageName)
            component = null
        }
    }

    private fun isPackageInstalled(packageManager: PackageManager, packageName: String): Boolean {
        return runCatching { packageManager.getPackageInfo(packageName, 0) }.isSuccess
    }

    private fun sourceSortGroup(packageName: String): Int {
        return when {
            packageName == SystemFileSourceKey -> 0
            packageName in knownFileManagerPackages -> 1
            else -> 2
        }
    }

    private fun isSystemPackage(packageManager: PackageManager, packageName: String): Boolean {
        return runCatching {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_SYSTEM) != 0
        }.getOrDefault(false)
    }
    private fun createBaseIntent(): Intent {
        return Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
    }


    private fun resolveApplicationLabel(packageManager: PackageManager, packageName: String): String? {
        return runCatching {
            val applicationInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrNull()
    }

    fun cachePickedFile(context: Context, uri: Uri, directoryName: String): CachedPickedFile {
        val resolver = context.contentResolver
        val displayName = resolveDisplayName(context, uri)
        val mimeType = if (uri.scheme == "file") {
            "application/octet-stream"
        } else {
            resolver.getType(uri) ?: "application/octet-stream"
        }
        val safeName = sanitizeFileName(displayName)
        val directory = File(context.cacheDir, directoryName).apply { mkdirs() }
        val file = File(directory, "${UUID.randomUUID()}-$safeName")
        val inputStream = if (uri.scheme == "file") {
            uri.path?.let { FileInputStream(File(it)) }
        } else {
            resolver.openInputStream(uri)
        }
        inputStream?.use { input ->
            file.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IllegalStateException(context.getString(R.string.repository_file_upload_permission_failed))
        return CachedPickedFile(
            uri = Uri.fromFile(file),
            displayName = displayName,
            mimeType = mimeType,
            sizeBytes = file.length()
        )
    }

    fun resolveDisplayName(context: Context, uri: Uri): String {
        if (uri.scheme == "file") {
            return uri.path?.let { File(it).name }?.takeIf { it.isNotBlank() } ?: "selected-file"
        }
        val resolver = context.contentResolver
        val queriedName = runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
            }
        }.getOrNull()
        return queriedName?.takeIf { it.isNotBlank() }
            ?: uri.lastPathSegment?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
            ?: "selected-file"
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(120).ifBlank { "selected-file" }
    }
}

data class CachedPickedFile(
    val uri: Uri,
    val displayName: String,
    val mimeType: String,
    val sizeBytes: Long
)