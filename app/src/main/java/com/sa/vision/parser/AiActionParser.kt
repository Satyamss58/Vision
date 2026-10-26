package com.sa.vision.parser

import com.sa.vision.model.AiAction
import com.sa.vision.model.ScrollDirection

object AiActionParser {

    /**
     * Parses AI response string into structured [AiAction].
     * Supports JSON formatted responses as well as regex fallback for plain text commands
     * like TAP: 15, HOLD: 8, SCROLL: 12 TO 34, TYPE: hello IN 5, TASK_COMPLETE: <result>.
     */
    fun parseAction(responseText: String): AiAction {
        val trimmed = responseText.trim()
        if (trimmed.isEmpty()) {
            return AiAction.Unknown(responseText, "Empty response text")
        }

        // Try JSON parsing first (handles markdown code blocks like ```json ... ```)
        val jsonAction = parseJsonAction(trimmed)
        if (jsonAction != null && jsonAction !is AiAction.Unknown) {
            return jsonAction
        }

        // Fallback to text/regex parser
        return parseTextAction(trimmed)
    }

    private fun parseJsonAction(rawText: String): AiAction? {
        val cleanedJson = extractJsonSubstring(rawText) ?: return null

        val actionStr = findJsonStringValue(cleanedJson, "action", "type", "command")?.uppercase() ?: return null

        val targetCell = findJsonIntValue(cleanedJson, "target_cell", "cell", "grid_number", "grid_cell", "cell_number")
        val fromCell = findJsonIntValue(cleanedJson, "from_cell", "start_cell", "source_cell")
        val toCell = findJsonIntValue(cleanedJson, "to_cell", "end_cell", "destination_cell")
        val inputText = findJsonStringValue(cleanedJson, "input_text", "text", "value", "query") ?: ""
        val resultText = findJsonStringValue(cleanedJson, "result", "message", "output", "summary") ?: ""
        val durationMs = findJsonLongValue(cleanedJson, "duration", "duration_ms") ?: 1000L

        return when {
            actionStr == "TAP" || actionStr == "CLICK" || actionStr == "PRESS" -> {
                val cell = targetCell ?: fromCell
                if (cell != null && cell > 0) {
                    AiAction.Tap(cell)
                } else null
            }
            actionStr == "HOLD" || actionStr == "LONG_PRESS" || actionStr == "LONGPRESS" -> {
                val cell = targetCell ?: fromCell
                if (cell != null && cell > 0) {
                    AiAction.Hold(cell, durationMs)
                } else null
            }
            actionStr == "SCROLL" || actionStr == "SWIPE" -> {
                val from = fromCell ?: targetCell ?: 1
                val to = toCell ?: (from + 5)
                val dirStr = findJsonStringValue(cleanedJson, "direction")?.uppercase() ?: ""
                val dir = parseScrollDirection(dirStr)
                AiAction.Scroll(fromCell = from, toCell = to, direction = dir)
            }
            actionStr == "SCROLL_UP" || actionStr == "SWIPE_UP" -> {
                val from = targetCell ?: fromCell ?: 10
                val to = toCell ?: (from - 5).coerceAtLeast(1)
                AiAction.Scroll(fromCell = from, toCell = to, direction = ScrollDirection.UP)
            }
            actionStr == "SCROLL_DOWN" || actionStr == "SWIPE_DOWN" -> {
                val from = targetCell ?: fromCell ?: 5
                val to = toCell ?: (from + 5)
                AiAction.Scroll(fromCell = from, toCell = to, direction = ScrollDirection.DOWN)
            }
            actionStr == "TYPE" || actionStr == "INPUT" || actionStr == "ENTER_TEXT" -> {
                AiAction.Type(cellNumber = targetCell ?: fromCell, text = inputText)
            }
            actionStr == "TASK_COMPLETE" || actionStr == "COMPLETE" || actionStr == "FINISHED" || actionStr == "DONE" -> {
                AiAction.TaskComplete(result = resultText.ifBlank { "Task completed" })
            }
            else -> null
        }
    }

    private fun parseTextAction(text: String): AiAction {
        val lines = text.lines()

        // 1. Check for TASK_COMPLETE format
        val taskCompleteRegex = Regex("""(?i)\b(?:TASK_COMPLETE|TASK COMPLETE|FINISHED|DONE|TASK_FINISHED|RESULT)[:\s]+(.+)""")
        lines.forEach { line ->
            val match = taskCompleteRegex.find(line)
            if (match != null) {
                val res = match.groupValues[1].trim()
                return AiAction.TaskComplete(res)
            }
        }
        if (text.contains("TASK_COMPLETE", ignoreCase = true) || text.contains("TASK COMPLETE", ignoreCase = true)) {
            val res = text.substringAfter("TASK_COMPLETE", "").substringAfter("TASK COMPLETE", "").trim(':', ' ', '\n')
            return AiAction.TaskComplete(if (res.isNotBlank()) res else "Task completed")
        }

        // 2. Check for SCROLL format (e.g., SCROLL: 12 TO 34 or SCROLL FROM 12 TO 34)
        val scrollRangeRegex = Regex("""(?i)\b(?:SCROLL|SWIPE)[:\s]+(?:FROM\s+)?(?:GRID|CELL)?\s*(\d+)\s*(?:TO|->|UNTIL)\s*(?:GRID|CELL)?\s*(\d+)""")
        val scrollRangeMatch = scrollRangeRegex.find(text)
        if (scrollRangeMatch != null) {
            val from = scrollRangeMatch.groupValues[1].toIntOrNull() ?: 1
            val to = scrollRangeMatch.groupValues[2].toIntOrNull() ?: 1
            return AiAction.Scroll(fromCell = from, toCell = to)
        }

        // Directional scroll (e.g., SCROLL UP ON 15 or SCROLL DOWN 10 TO 20)
        val directionalScrollRegex = Regex("""(?i)\b(?:SCROLL|SWIPE)[_\s]+(UP|DOWN|LEFT|RIGHT)(?:\s+(?:ON|AT|FROM)?\s*(?:GRID|CELL)?\s*(\d+))?""")
        val dirMatch = directionalScrollRegex.find(text)
        if (dirMatch != null) {
            val dirStr = dirMatch.groupValues[1].uppercase()
            val dir = parseScrollDirection(dirStr)
            val cell = dirMatch.groupValues[2].toIntOrNull() ?: 10
            val targetCell = if (dir == ScrollDirection.UP) (cell - 5).coerceAtLeast(1) else (cell + 5)
            return AiAction.Scroll(fromCell = cell, toCell = targetCell, direction = dir)
        }

        // 3. Check for HOLD format (e.g., HOLD: 8 or LONG PRESS 8)
        val holdRegex = Regex("""(?i)\b(?:HOLD|LONG[_\s]*PRESS)[:\s]+(?:ON\s+)?(?:GRID|CELL)?\s*(\d+)""")
        val holdMatch = holdRegex.find(text)
        if (holdMatch != null) {
            val cell = holdMatch.groupValues[1].toIntOrNull() ?: 1
            return AiAction.Hold(cellNumber = cell)
        }

        // 4. Check for TYPE format (e.g., TYPE "hello" ON 5 or TYPE: hello IN GRID 5)
        val typeWithCellRegex = Regex("""(?i)\b(?:TYPE|INPUT|ENTER)[:\s]+["']?([^"'\n]+?)["']?\s*(?:IN|ON|AT|INTO)\s*(?:GRID|CELL)?\s*(\d+)""")
        val typeWithCellMatch = typeWithCellRegex.find(text)
        if (typeWithCellMatch != null) {
            val input = typeWithCellMatch.groupValues[1].trim()
            val cell = typeWithCellMatch.groupValues[2].toIntOrNull()
            return AiAction.Type(cellNumber = cell, text = input)
        }

        val typeGenericRegex = Regex("""(?i)\b(?:TYPE|INPUT|ENTER)[:\s]+["']?([^"'\n]+)["']?""")
        val typeGenericMatch = typeGenericRegex.find(text)
        if (typeGenericMatch != null) {
            val input = typeGenericMatch.groupValues[1].trim()
            return AiAction.Type(cellNumber = null, text = input)
        }

        // 5. Check for TAP format (e.g., TAP: 15, TAP ON GRID 15, CLICK CELL 15)
        val tapRegex = Regex("""(?i)\b(?:TAP|CLICK|PRESS)[:\s]+(?:ON\s+)?(?:GRID|CELL)?\s*(\d+)""")
        val tapMatch = tapRegex.find(text)
        if (tapMatch != null) {
            val cell = tapMatch.groupValues[1].toIntOrNull() ?: 1
            return AiAction.Tap(cellNumber = cell)
        }

        // Fallback: search for stand-alone grid number if TAP is mentioned anywhere
        if (text.contains("TAP", ignoreCase = true) || text.contains("CLICK", ignoreCase = true)) {
            val numMatch = Regex("""\b(\d+)\b""").find(text)
            if (numMatch != null) {
                val cell = numMatch.groupValues[1].toIntOrNull() ?: 1
                return AiAction.Tap(cellNumber = cell)
            }
        }

        return AiAction.Unknown(text, "No recognized action command found")
    }

    private fun extractJsonSubstring(raw: String): String? {
        val trimmed = raw.trim()
        val jsonFenceRegex = Regex("""```(?:json)?\s*(\{[\s\S]*?\})\s*```""", RegexOption.IGNORE_CASE)
        val fenceMatch = jsonFenceRegex.find(trimmed)
        if (fenceMatch != null) {
            return fenceMatch.groupValues[1].trim()
        }

        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1).trim()
        }
        return null
    }

    private fun findJsonStringValue(json: String, vararg keys: String): String? {
        for (key in keys) {
            // Match "key"\s*:\s*"value"
            val patternQuoted = Regex(""""$key"\s*:\s*"([^"\\]*(?:\\.[^"\\]*)*)"""", RegexOption.IGNORE_CASE)
            val matchQuoted = patternQuoted.find(json)
            if (matchQuoted != null) {
                return unescapeJsonString(matchQuoted.groupValues[1])
            }

            // Match "key"\s*:\s*([A-Za-z0-9_]+)
            val patternUnquoted = Regex(""""$key"\s*:\s*([A-Za-z0-9_]+)""", RegexOption.IGNORE_CASE)
            val matchUnquoted = patternUnquoted.find(json)
            if (matchUnquoted != null) {
                return matchUnquoted.groupValues[1]
            }
        }
        return null
    }

    private fun findJsonIntValue(json: String, vararg keys: String): Int? {
        for (key in keys) {
            val pattern = Regex(""""$key"\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)
            val match = pattern.find(json)
            if (match != null) {
                return match.groupValues[1].toIntOrNull()
            }
        }
        return null
    }

    private fun findJsonLongValue(json: String, vararg keys: String): Long? {
        for (key in keys) {
            val pattern = Regex(""""$key"\s*:\s*(\d+)""", RegexOption.IGNORE_CASE)
            val match = pattern.find(json)
            if (match != null) {
                return match.groupValues[1].toLongOrNull()
            }
        }
        return null
    }

    private fun unescapeJsonString(escaped: String): String {
        return escaped
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
            .replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
    }

    private fun parseScrollDirection(str: String): ScrollDirection? {
        return when (str) {
            "UP" -> ScrollDirection.UP
            "DOWN" -> ScrollDirection.DOWN
            "LEFT" -> ScrollDirection.LEFT
            "RIGHT" -> ScrollDirection.RIGHT
            else -> null
        }
    }
}
