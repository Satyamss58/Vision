package com.sa.vision.automation

import android.graphics.Bitmap
import android.view.View
import com.sa.vision.gesture.GestureExecutionEngine
import com.sa.vision.model.AiAction
import com.sa.vision.model.GridConfig
import com.sa.vision.parser.AiActionParser
import com.sa.vision.util.GridOverlayUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AutomationLoopManager(
    private val maxSteps: Int = 15
) {
    private val _state = MutableStateFlow(AutomationLoopState(maxSteps = maxSteps))
    val state: StateFlow<AutomationLoopState> = _state.asStateFlow()

    private var loopJob: Job? = null

    fun startLoop(userTask: String) {
        if (userTask.isBlank()) return

        stopLoop()
        _state.update {
            AutomationLoopState(
                status = LoopStatus.CAPTURING,
                currentStep = 1,
                maxSteps = maxSteps,
                userTask = userTask,
                stepHistory = emptyList()
            )
        }
    }

    fun onScreenCaptured(rawBitmap: Bitmap, gridConfig: GridConfig) {
        if (_state.value.status != LoopStatus.CAPTURING) return

        val stepNum = _state.value.currentStep
        val step = AutomationStep(
            stepNumber = stepNum,
            bitmap = rawBitmap,
            description = "Captured screen for step $stepNum"
        )

        _state.update { current ->
            current.copy(
                status = LoopStatus.PROMPTING_AI,
                stepHistory = current.stepHistory + step
            )
        }
    }

    fun processAiResponse(
        responseText: String,
        targetView: View?,
        screenWidth: Float,
        screenHeight: Float,
        gridConfig: GridConfig,
        scope: CoroutineScope = CoroutineScope(Dispatchers.Main),
        onStepComplete: (() -> Unit)? = null
    ) {
        if (_state.value.status != LoopStatus.PROMPTING_AI && _state.value.status != LoopStatus.IDLE) return

        val action = AiActionParser.parseAction(responseText)
        val currentStepNum = _state.value.currentStep

        // Check for MAX step limit guard
        if (currentStepNum > maxSteps && action !is AiAction.TaskComplete) {
            _state.update {
                it.copy(
                    status = LoopStatus.FAILED,
                    errorMessage = "Reached maximum step count ($maxSteps iterations limit)."
                )
            }
            return
        }

        when (action) {
            is AiAction.TaskComplete -> {
                _state.update { current ->
                    val updatedHistory = current.stepHistory.map { s ->
                        if (s.stepNumber == currentStepNum) s.copy(action = action, description = "Task Complete: ${action.result}") else s
                    }
                    current.copy(
                        status = LoopStatus.COMPLETED,
                        currentAction = action,
                        resultMessage = action.result,
                        stepHistory = updatedHistory
                    )
                }
            }

            is AiAction.Unknown -> {
                _state.update { current ->
                    current.copy(
                        status = LoopStatus.FAILED,
                        errorMessage = "Failed to parse AI action command: ${action.rawText}"
                    )
                }
            }

            else -> {
                // Calculate target cell coordinates for visual overlay indicator
                val cells = GridOverlayUtil.calculateGridCells(screenWidth, screenHeight, gridConfig)
                val targetCellNum = when (action) {
                    is AiAction.Tap -> action.cellNumber
                    is AiAction.Hold -> action.cellNumber
                    is AiAction.Type -> action.cellNumber
                    is AiAction.Scroll -> action.fromCell
                    else -> null
                }
                val coords = targetCellNum?.let { num ->
                    cells.firstOrNull { it.cellIndex == num }?.let { Pair(it.centerX, it.centerY) }
                }

                _state.update { current ->
                    val updatedHistory = current.stepHistory.map { s ->
                        if (s.stepNumber == currentStepNum) s.copy(action = action, description = "Action: $action") else s
                    }
                    current.copy(
                        status = LoopStatus.EXECUTING_GESTURE,
                        currentAction = action,
                        targetCoordinates = coords,
                        stepHistory = updatedHistory
                    )
                }

                // Execute gesture
                GestureExecutionEngine.executeAction(
                    action = action,
                    screenWidth = screenWidth,
                    screenHeight = screenHeight,
                    gridConfig = gridConfig,
                    targetView = targetView,
                    scope = scope,
                    useAccessibilityIfAvailable = true,
                    onComplete = { _ ->
                        scope.launch(Dispatchers.Main) {
                            // Wait for web page transition / DOM settle delay (1.5 seconds)
                            delay(1500L)
                            if (_state.value.status == LoopStatus.EXECUTING_GESTURE) {
                                val nextStep = currentStepNum + 1
                                if (nextStep > maxSteps) {
                                    _state.update {
                                        it.copy(
                                            status = LoopStatus.FAILED,
                                            errorMessage = "Maximum step count ($maxSteps) reached."
                                        )
                                    }
                                } else {
                                    _state.update {
                                        it.copy(
                                            status = LoopStatus.CAPTURING,
                                            currentStep = nextStep,
                                            currentAction = null,
                                            targetCoordinates = null
                                        )
                                    }
                                    onStepComplete?.invoke()
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    fun stopLoop() {
        loopJob?.cancel()
        loopJob = null
        _state.update { AutomationLoopState(status = LoopStatus.IDLE) }
    }

    fun pauseLoop() {
        _state.update { it.copy(status = LoopStatus.PAUSED) }
    }

    fun resumeLoop() {
        if (_state.value.status == LoopStatus.PAUSED) {
            _state.update { it.copy(status = LoopStatus.CAPTURING) }
        }
    }
}
