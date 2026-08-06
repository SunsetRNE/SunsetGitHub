package com.Sunset.REN.GitHub.domain.filemanager

import org.junit.Assert.assertEquals
import org.junit.Test

class FileSizeFormatterTest {
    @Test
    fun formatReturnsBytesBelowOneKib() {
        assertEquals("0 B", FileSizeFormatter.format(0L))
        assertEquals("1 B", FileSizeFormatter.format(1L))
        assertEquals("1023 B", FileSizeFormatter.format(1023L))
    }

    @Test
    fun formatReturnsBinaryUnitsWithSingleDecimalPlace() {
        assertEquals("1.0 KiB", FileSizeFormatter.format(1024L))
        assertEquals("1.5 KiB", FileSizeFormatter.format(1536L))
        assertEquals("1.0 MiB", FileSizeFormatter.format(1024L * 1024L))
        assertEquals("1.0 GiB", FileSizeFormatter.format(1024L * 1024L * 1024L))
    }

    @Test
    fun formatCapsAtGibUnit() {
        assertEquals("1024.0 GiB", FileSizeFormatter.format(1024L * 1024L * 1024L * 1024L))
    }
}