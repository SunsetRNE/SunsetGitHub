package com.Sunset.REN.GitHub.domain.filemanager

import java.util.Locale

object FileSizeFormatter {
    fun format(sizeBytes: Long): String {
        if (sizeBytes < BytesPerKiB) return "$sizeBytes B"
        var value = sizeBytes / BytesPerKiB.toDouble()
        var unitIndex = 0
        while (value >= BytesPerKiB && unitIndex < Units.lastIndex) {
            value /= BytesPerKiB
            unitIndex++
        }
        return String.format(Locale.US, "%.1f %s", value, Units[unitIndex])
    }

    private const val BytesPerKiB = 1024L
    private val Units = listOf("KiB", "MiB", "GiB")
}