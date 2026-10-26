package com.sa.vision.parser

import com.sa.vision.model.AiAction
import com.sa.vision.model.ScrollDirection
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiActionParserTest {

    @Test
    fun testParseJsonTapAction() {
        val jsonInput = """
            ```json
            {
              "action": "TAP",
              "target_cell": 15,
              "reasoning": "Click on search button"
            }
            ```
        """.trimIndent()

        val action = AiActionParser.parseAction(jsonInput)
        assertTrue(action is AiAction.Tap)
        assertEquals(15, (action as AiAction.Tap).cellNumber)
    }

    @Test
    fun testParseJsonHoldAction() {
        val jsonInput = """
            {
              "action": "HOLD",
              "target_cell": 8,
              "duration": 1500
            }
        """.trimIndent()

        val action = AiActionParser.parseAction(jsonInput)
        assertTrue(action is AiAction.Hold)
        val holdAction = action as AiAction.Hold
        assertEquals(8, holdAction.cellNumber)
        assertEquals(1500L, holdAction.durationMs)
    }

    @Test
    fun testParseJsonScrollAction() {
        val jsonInput = """
            {
              "action": "SCROLL",
              "from_cell": 12,
              "to_cell": 34
            }
        """.trimIndent()

        val action = AiActionParser.parseAction(jsonInput)
        assertTrue(action is AiAction.Scroll)
        val scrollAction = action as AiAction.Scroll
        assertEquals(12, scrollAction.fromCell)
        assertEquals(34, scrollAction.toCell)
    }

    @Test
    fun testParseJsonTypeAction() {
        val jsonInput = """
            {
              "action": "TYPE",
              "target_cell": 5,
              "input_text": "Jetpack Compose"
            }
        """.trimIndent()

        val action = AiActionParser.parseAction(jsonInput)
        assertTrue(action is AiAction.Type)
        val typeAction = action as AiAction.Type
        assertEquals(5, typeAction.cellNumber)
        assertEquals("Jetpack Compose", typeAction.text)
    }

    @Test
    fun testParseJsonTaskCompleteAction() {
        val jsonInput = """
            {
              "action": "TASK_COMPLETE",
              "result": "Successfully created account and logged in"
            }
        """.trimIndent()

        val action = AiActionParser.parseAction(jsonInput)
        assertTrue(action is AiAction.TaskComplete)
        assertEquals("Successfully created account and logged in", (action as AiAction.TaskComplete).result)
    }

    @Test
    fun testParseTextTapAction() {
        val textInput = "TAP: 15"
        val action = AiActionParser.parseAction(textInput)
        assertTrue(action is AiAction.Tap)
        assertEquals(15, (action as AiAction.Tap).cellNumber)
    }

    @Test
    fun testParseTextTapWithCellPrefix() {
        val textInput = "Click on grid cell 23 to proceed"
        val action = AiActionParser.parseAction(textInput)
        assertTrue(action is AiAction.Tap)
        assertEquals(23, (action as AiAction.Tap).cellNumber)
    }

    @Test
    fun testParseTextHoldAction() {
        val textInput = "HOLD: 8"
        val action = AiActionParser.parseAction(textInput)
        assertTrue(action is AiAction.Hold)
        assertEquals(8, (action as AiAction.Hold).cellNumber)
    }

    @Test
    fun testParseTextScrollAction() {
        val textInput = "SCROLL: 12 TO 34"
        val action = AiActionParser.parseAction(textInput)
        assertTrue(action is AiAction.Scroll)
        val scrollAction = action as AiAction.Scroll
        assertEquals(12, scrollAction.fromCell)
        assertEquals(34, scrollAction.toCell)
    }

    @Test
    fun testParseTextDirectionalScrollAction() {
        val textInput = "SCROLL UP ON GRID 18"
        val action = AiActionParser.parseAction(textInput)
        assertTrue(action is AiAction.Scroll)
        val scrollAction = action as AiAction.Scroll
        assertEquals(18, scrollAction.fromCell)
        assertEquals(ScrollDirection.UP, scrollAction.direction)
    }

    @Test
    fun testParseTextTypeAction() {
        val textInput = "TYPE \"Hello World\" ON GRID 7"
        val action = AiActionParser.parseAction(textInput)
        assertTrue(action is AiAction.Type)
        val typeAction = action as AiAction.Type
        assertEquals(7, typeAction.cellNumber)
        assertEquals("Hello World", typeAction.text)
    }

    @Test
    fun testParseTextTaskCompleteAction() {
        val textInput = "TASK_COMPLETE: Search results retrieved successfully."
        val action = AiActionParser.parseAction(textInput)
        assertTrue(action is AiAction.TaskComplete)
        assertEquals("Search results retrieved successfully.", (action as AiAction.TaskComplete).result)
    }

    @Test
    fun testParseUnknownAction() {
        val textInput = "Hello world, how are you today?"
        val action = AiActionParser.parseAction(textInput)
        assertTrue(action is AiAction.Unknown)
    }
}
