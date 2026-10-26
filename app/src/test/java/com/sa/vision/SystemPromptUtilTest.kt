package com.sa.vision

import com.sa.vision.model.GridConfig
import com.sa.vision.util.SystemPromptUtil
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemPromptUtilTest {

    @Test
    fun testBuildVisionSystemPrompt_includesDimensionsAndGridInstructions() {
        val gridConfig = GridConfig(rows = 10, columns = 5)
        val prompt = SystemPromptUtil.buildVisionSystemPrompt(
            screenWidth = 1080,
            screenHeight = 2400,
            gridConfig = gridConfig,
            densityDpi = 440,
            userTask = "Click on the Search button"
        )

        assertTrue(prompt.contains("1080x2400 px"))
        assertTrue(prompt.contains("10 rows x 5 columns"))
        assertTrue(prompt.contains("Total 50 cells"))
        assertTrue(prompt.contains("Cell #1"))
        assertTrue(prompt.contains("Cell #50"))
        assertTrue(prompt.contains("Click on the Search button"))
        assertTrue(prompt.contains("INSTRUCTIONS FOR VISION AI"))
        assertTrue(prompt.contains("target_cell"))
    }
}
