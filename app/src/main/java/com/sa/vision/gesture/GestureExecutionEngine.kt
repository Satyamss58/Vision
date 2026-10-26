package com.sa.vision.gesture

import android.view.View
import com.sa.vision.model.AiAction
import com.sa.vision.model.GridCell
import com.sa.vision.model.GridConfig
import com.sa.vision.model.ScrollDirection
import com.sa.vision.service.VisionAccessibilityService
import com.sa.vision.util.GridOverlayUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object GestureExecutionEngine {

    /**
     * Executes the parsed [AiAction] by mapping grid cell numbers to (X, Y) pixel coordinates
     * and performing simulated touch events or accessibility gestures.
     */
    fun executeAction(
        action: AiAction,
        screenWidth: Float,
        screenHeight: Float,
        gridConfig: GridConfig,
        targetView: View?,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        useAccessibilityIfAvailable: Boolean = true,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        val cells = GridOverlayUtil.calculateGridCells(screenWidth, screenHeight, gridConfig)

        when (action) {
            is AiAction.Tap -> {
                val targetCell = findCell(cells, action.cellNumber)
                val targetX = targetCell?.centerX ?: (screenWidth / 2f)
                val targetY = targetCell?.centerY ?: (screenHeight / 2f)

                val service = VisionAccessibilityService.instance
                if (useAccessibilityIfAvailable && service != null) {
                    service.dispatchTap(targetX, targetY, onComplete)
                } else if (targetView != null) {
                    TouchSimulator.simulateTap(targetView, targetX, targetY, onComplete)
                } else {
                    onComplete?.invoke(false)
                }
            }

            is AiAction.Hold -> {
                val targetCell = findCell(cells, action.cellNumber)
                val targetX = targetCell?.centerX ?: (screenWidth / 2f)
                val targetY = targetCell?.centerY ?: (screenHeight / 2f)

                val service = VisionAccessibilityService.instance
                if (useAccessibilityIfAvailable && service != null) {
                    service.dispatchHold(targetX, targetY, action.durationMs, onComplete)
                } else if (targetView != null) {
                    TouchSimulator.simulateHold(targetView, targetX, targetY, action.durationMs, scope, onComplete)
                } else {
                    onComplete?.invoke(false)
                }
            }

            is AiAction.Scroll -> {
                val fromCellObj = findCell(cells, action.fromCell)
                val toCellObj = findCell(cells, action.toCell)

                var startX = fromCellObj?.centerX ?: (screenWidth / 2f)
                var startY = fromCellObj?.centerY ?: (screenHeight * 0.7f)
                var endX = toCellObj?.centerX ?: (screenWidth / 2f)
                var endY = toCellObj?.centerY ?: (screenHeight * 0.3f)

                if (action.direction != null) {
                    val scrollDistanceY = screenHeight * 0.35f
                    val scrollDistanceX = screenWidth * 0.35f
                    when (action.direction) {
                        ScrollDirection.UP -> {
                            // Drag down to scroll page up (reveal top content)
                            startY = screenHeight * 0.3f
                            endY = startY + scrollDistanceY
                        }
                        ScrollDirection.DOWN -> {
                            // Drag up to scroll page down (reveal bottom content)
                            startY = screenHeight * 0.7f
                            endY = startY - scrollDistanceY
                        }
                        ScrollDirection.LEFT -> {
                            startX = screenWidth * 0.2f
                            endX = startX + scrollDistanceX
                        }
                        ScrollDirection.RIGHT -> {
                            startX = screenWidth * 0.8f
                            endX = startX - scrollDistanceX
                        }
                    }
                }

                val service = VisionAccessibilityService.instance
                if (useAccessibilityIfAvailable && service != null) {
                    service.dispatchScroll(startX, startY, endX, endY, 300L, onComplete)
                } else if (targetView != null) {
                    TouchSimulator.simulateScroll(
                        view = targetView,
                        startX = startX,
                        startY = startY,
                        endX = endX,
                        endY = endY,
                        durationMs = 300L,
                        steps = 10,
                        scope = scope,
                        onComplete = onComplete
                    )
                } else {
                    onComplete?.invoke(false)
                }
            }

            is AiAction.Type -> {
                val targetCell = action.cellNumber?.let { findCell(cells, it) }
                val targetX = targetCell?.centerX
                val targetY = targetCell?.centerY

                TouchSimulator.simulateType(targetView, action.text, targetX, targetY, onComplete)
            }

            is AiAction.TaskComplete -> {
                onComplete?.invoke(true)
            }

            is AiAction.Unknown -> {
                onComplete?.invoke(false)
            }
        }
    }

    private fun findCell(cells: List<GridCell>, cellNumber: Int): GridCell? {
        return cells.firstOrNull { it.cellIndex == cellNumber }
    }
}
