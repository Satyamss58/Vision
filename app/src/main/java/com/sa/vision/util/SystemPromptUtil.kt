package com.sa.vision.util

import com.sa.vision.model.GridConfig

object SystemPromptUtil {

    /**
     * Formats structured vision context prompt for AI containing screen dimensions,
     * grid resolution (rows x columns), cell pixel bounds, and touch action command instructions.
     */
    fun buildVisionSystemPrompt(
        screenWidth: Int,
        screenHeight: Int,
        gridConfig: GridConfig,
        densityDpi: Int = 160,
        userTask: String? = null
    ): String {
        val cells = GridOverlayUtil.calculateGridCells(
            width = screenWidth.toFloat(),
            height = screenHeight.toFloat(),
            gridConfig = gridConfig
        )

        val sb = StringBuilder()
        sb.appendLine("=== VISION CONTROL SYSTEM PROMPT ===")
        sb.appendLine("Screen Resolution: ${screenWidth}x${screenHeight} px (DPI: $densityDpi)")
        sb.appendLine("Grid Resolution: ${gridConfig.rows} rows x ${gridConfig.columns} columns (Total ${gridConfig.totalCells} cells)")
        sb.appendLine()
        sb.appendLine("GRID CELL COORDINATES MAPPING:")
        cells.forEach { cell ->
            sb.appendLine(
                "  Cell #${cell.cellIndex}: Row ${cell.row + 1}, Col ${cell.col + 1} | Bounds: [L:${cell.boundsLeft.toInt()}, T:${cell.boundsTop.toInt()}, R:${cell.boundsRight.toInt()}, B:${cell.boundsBottom.toInt()}] | Center: (${cell.centerX.toInt()}, ${cell.centerY.toInt()})"
            )
        }
        sb.appendLine()
        sb.appendLine("INSTRUCTIONS FOR VISION AI:")
        sb.appendLine("1. Analyze the attached screenshot overlaid with grid badges #1 to #${gridConfig.totalCells}.")
        sb.appendLine("2. Identify the target UI component (button, text input, link, icon, menu item) relevant to the request.")
        sb.appendLine("3. Determine the EXACT grid cell number (#X) covering the target component.")
        sb.appendLine("4. Respond strictly in the following JSON format (no extra markdown explanation):")
        sb.appendLine("""
            {
              "target_cell": <cell_number_1_to_${gridConfig.totalCells}>,
              "action": "<TAP | HOLD | TYPE | SCROLL_UP | SCROLL_DOWN>",
              "input_text": "<optional text to type if action is TYPE>",
              "reasoning": "<short description of target UI element>"
            }
        """.trimIndent())

        if (!userTask.isNullOrBlank()) {
            sb.appendLine()
            sb.appendLine("CURRENT USER REQUEST: \"$userTask\"")
        }

        return sb.toString()
    }
}
