package com.sa.vision.automation

import android.graphics.Bitmap
import com.sa.vision.model.AiAction

enum class LoopStatus {
    IDLE,
    CAPTURING,
    PROMPTING_AI,
    EXECUTING_GESTURE,
    PAUSED,
    COMPLETED,
    FAILED
}

data class AutomationStep(
    val stepNumber: Int,
    val bitmap: Bitmap? = null,
    val action: AiAction? = null,
    val description: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class AutomationLoopState(
    val status: LoopStatus = LoopStatus.IDLE,
    val currentStep: Int = 0,
    val maxSteps: Int = 15,
    val userTask: String = "",
    val currentAction: AiAction? = null,
    val targetCoordinates: Pair<Float, Float>? = null,
    val resultMessage: String? = null,
    val errorMessage: String? = null,
    val stepHistory: List<AutomationStep> = emptyList()
)
