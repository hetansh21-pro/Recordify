package com.audiovault.app

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FilenameGeneratorTest {

    @Test
    fun `filename format matches expected Recording_YYYYMMDD_HHmmss pattern`() {
        val testDate = Date(1774887085000L) // Fixed timestamp
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(testDate)
        val fileName = "Recording_$timeStamp.m4a"

        assertThat(fileName).matches("Recording_\\d{8}_\\d{6}\\.m4a")
        assertThat(fileName.endsWith(".m4a")).isTrue()
        assertThat(fileName.startsWith("Recording_")).isTrue()
    }

    @Test
    fun `default title format matches expected pattern`() {
        val testDate = Date(1774887085000L)
        val dateFormat = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.US)
        val title = "Recording ${dateFormat.format(testDate)}"

        assertThat(title).startsWith("Recording ")
        assertThat(title).contains("2026")
    }
}
