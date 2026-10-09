package com.example.exifclean.core.shred

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BestEffortOverwriterTest {

    @Test
    fun testOverwriteAndDeleteRemovesFile() = runBlocking {
        val tempFile = File.createTempFile("test_shred", ".tmp")
        tempFile.writeText("Sensitive private user data that needs to be overwritten")

        assertTrue(tempFile.exists())

        val result = BestEffortOverwriter.overwriteAndDelete(tempFile)

        assertEquals(ResultStatus.OVERWRITTEN_BEST_EFFORT, result)
        assertFalse(tempFile.exists())
    }
}
