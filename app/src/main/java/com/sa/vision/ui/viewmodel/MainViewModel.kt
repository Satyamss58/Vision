package com.sa.vision.ui.viewmodel

import android.graphics.Bitmap
import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sa.vision.automation.AutomationLoopManager
import com.sa.vision.automation.AutomationLoopState
import com.sa.vision.automation.LoopStatus
import com.sa.vision.model.AiPlatform
import com.sa.vision.model.GridConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

data class UiState(
    val selectedPlatform: AiPlatform = AiPlatform.GEMINI,
    val promptText: String = "",
    val isListening: Boolean = false,
    val statusMessage: String? = null,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val isLoading: Boolean = false,
    val progress: Int = 0,

    // Task 2: Grid & Screen Capture State
    val isGridOverlayVisible: Boolean = false,
    val gridConfig: GridConfig = GridConfig(rows = 10, columns = 5),
    val captureTrigger: Long = 0L,
    val lastCapturedBitmap: Bitmap? = null,
    val lastCompositedBitmap: Bitmap? = null,
    val isCapturing: Boolean = false,
    val lastPromptContext: String? = null,
    val base64Image: String? = null,

    // Task 3: Vision Automation State
    val automationState: AutomationLoopState = AutomationLoopState()
)

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val automationLoopManager = AutomationLoopManager(maxSteps = 15)

    init {
        automationLoopManager.state.onEach { autoState ->
            _uiState.update { current ->
                current.copy(automationState = autoState)
            }
        }.launchIn(viewModelScope)
    }

    fun selectPlatform(platform: AiPlatform) {
        _uiState.update { it.copy(selectedPlatform = platform) }
    }

    fun updatePromptText(text: String) {
        _uiState.update { it.copy(promptText = text) }
    }

    fun appendPromptText(text: String) {
        _uiState.update { current ->
            val updated = if (current.promptText.isBlank()) {
                text
            } else {
                "${current.promptText}\n$text"
            }
            current.copy(promptText = updated)
        }
    }

    fun setListening(isListening: Boolean) {
        _uiState.update { it.copy(isListening = isListening) }
    }

    fun showStatusMessage(message: String) {
        _uiState.update { it.copy(statusMessage = message) }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }

    fun updateNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        _uiState.update {
            it.copy(canGoBack = canGoBack, canGoForward = canGoForward)
        }
    }

    fun updateLoadingProgress(isLoading: Boolean, progress: Int) {
        _uiState.update {
            it.copy(isLoading = isLoading, progress = progress)
        }
    }

    // --- Task 2: Grid & Screen Capture ---

    fun toggleGridOverlay() {
        _uiState.update { it.copy(isGridOverlayVisible = !it.isGridOverlayVisible) }
    }

    fun setGridOverlayVisible(visible: Boolean) {
        _uiState.update { it.copy(isGridOverlayVisible = visible) }
    }

    fun updateGridDimensions(rows: Int, columns: Int) {
        _uiState.update {
            it.copy(gridConfig = GridConfig(rows = rows.coerceAtLeast(1), columns = columns.coerceAtLeast(1)))
        }
    }

    fun triggerScreenCapture() {
        _uiState.update {
            it.copy(captureTrigger = System.currentTimeMillis(), isCapturing = true)
        }
    }

    fun onBitmapCaptured(
        rawBitmap: Bitmap,
        compositedBitmap: Bitmap,
        promptContext: String,
        base64: String
    ) {
        _uiState.update {
            it.copy(
                lastCapturedBitmap = rawBitmap,
                lastCompositedBitmap = compositedBitmap,
                lastPromptContext = promptContext,
                base64Image = base64,
                isCapturing = false
            )
        }

        // Notify Automation Loop Manager if loop is capturing
        if (uiState.value.automationState.status == LoopStatus.CAPTURING) {
            automationLoopManager.onScreenCaptured(rawBitmap, uiState.value.gridConfig)
        }
    }

    fun clearCapturedState() {
        _uiState.update {
            it.copy(
                lastCapturedBitmap = null,
                lastCompositedBitmap = null,
                lastPromptContext = null,
                base64Image = null
            )
        }
    }

    // --- Task 3: Vision Automation Control ---

    fun startVisionAutomation(taskText: String? = null) {
        val task = taskText?.takeIf { it.isNotBlank() } ?: uiState.value.promptText.takeIf { it.isNotBlank() } ?: "Perform task"
        _uiState.update { it.copy(isGridOverlayVisible = true) }
        automationLoopManager.startLoop(task)
        triggerScreenCapture()
    }

    fun processAiAutomationResponse(
        responseText: String,
        targetView: View?,
        screenWidth: Float,
        screenHeight: Float,
        onNextCaptureRequested: () -> Unit
    ) {
        automationLoopManager.processAiResponse(
            responseText = responseText,
            targetView = targetView,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            gridConfig = uiState.value.gridConfig,
            scope = viewModelScope,
            onStepComplete = {
                triggerScreenCapture()
                onNextCaptureRequested()
            }
        )
    }

    fun stopVisionAutomation() {
        automationLoopManager.stopLoop()
    }

    fun pauseVisionAutomation() {
        automationLoopManager.pauseLoop()
    }

    fun resumeVisionAutomation() {
        automationLoopManager.resumeLoop()
        triggerScreenCapture()
    }
}
