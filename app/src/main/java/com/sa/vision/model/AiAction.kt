package com.sa.vision.model

enum class ScrollDirection {
    UP, DOWN, LEFT, RIGHT
}

sealed interface AiAction {
    data class Tap(val cellNumber: Int) : AiAction
    data class Hold(val cellNumber: Int, val durationMs: Long = 1000L) : AiAction
    data class Scroll(
        val fromCell: Int,
        val toCell: Int,
        val direction: ScrollDirection? = null
    ) : AiAction
    data class Type(val cellNumber: Int?, val text: String) : AiAction
    data class TaskComplete(val result: String) : AiAction
    data class Unknown(val rawText: String, val reason: String = "Could not parse command") : AiAction
}
