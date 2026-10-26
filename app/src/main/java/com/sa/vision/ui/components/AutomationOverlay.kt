package com.sa.vision.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sa.vision.automation.AutomationLoopState
import com.sa.vision.automation.LoopStatus
import com.sa.vision.model.AiAction

@Composable
fun AutomationOverlay(
    automationState: AutomationLoopState,
    onStopClick: () -> Unit,
    onPauseClick: () -> Unit,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = automationState.status != LoopStatus.IDLE

    AnimatedVisibility(
        visible = isRunning,
        enter = fadeIn() + slideInVertically { -it },
        exit = fadeOut() + slideOutVertically { -it },
        modifier = modifier.fillMaxSize()
    ) {
        Box(modifier = Modifier.fillMaxSize()) {

            // Top Status Banner Overlay
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
                    .align(Alignment.TopCenter),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f),
                shadowElevation = 8.dp,
                tonalElevation = 6.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        color = when (automationState.status) {
                                            LoopStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                                            LoopStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                                            else -> MaterialTheme.colorScheme.secondaryContainer
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (automationState.status == LoopStatus.PROMPTING_AI || automationState.status == LoopStatus.CAPTURING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.5.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.AutoMode,
                                        contentDescription = "Automation status",
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            Column {
                                Text(
                                    text = "Vision Automation (${automationState.currentStep}/${automationState.maxSteps})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = getStatusDescription(automationState),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Action Controls
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (automationState.status == LoopStatus.PAUSED) {
                                IconButton(onClick = onResumeClick) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Resume automation",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else if (automationState.status != LoopStatus.COMPLETED && automationState.status != LoopStatus.FAILED) {
                                IconButton(onClick = onPauseClick) {
                                    Icon(
                                        imageVector = Icons.Default.Pause,
                                        contentDescription = "Pause automation",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            IconButton(onClick = onStopClick) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Stop automation",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    // Progress Bar
                    val progressFraction = (automationState.currentStep.toFloat() / automationState.maxSteps.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        color = when (automationState.status) {
                            LoopStatus.COMPLETED -> Color(0xFF4CAF50)
                            LoopStatus.FAILED -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.primary
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                }
            }

            // Target Gesture Marker Overlay (Pulsing Circle at Cell Center)
            if (automationState.status == LoopStatus.EXECUTING_GESTURE && automationState.targetCoordinates != null) {
                val (targetX, targetY) = automationState.targetCoordinates

                val infiniteTransition = rememberInfiniteTransition(label = "TargetPulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 20f,
                    targetValue = 60f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "PulseRadius"
                )
                val pulseAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "PulseAlpha"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val center = Offset(targetX, targetY)

                    // Draw outer pulsing ripple circle
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = pulseAlpha),
                        radius = pulseScale,
                        center = center,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw inner target dot
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = 12.dp.toPx(),
                        center = center
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = center
                    )
                }
            }
        }
    }
}

private fun getStatusDescription(state: AutomationLoopState): String {
    return when (state.status) {
        LoopStatus.IDLE -> "Idle"
        LoopStatus.CAPTURING -> "Capturing screen & overlaying grid..."
        LoopStatus.PROMPTING_AI -> "Sending vision payload to AI agent..."
        LoopStatus.EXECUTING_GESTURE -> {
            when (val action = state.currentAction) {
                is AiAction.Tap -> "Executing TAP on Cell #${action.cellNumber}"
                is AiAction.Hold -> "Executing HOLD on Cell #${action.cellNumber}"
                is AiAction.Scroll -> "Executing SCROLL from Cell #${action.fromCell} to #${action.toCell}"
                is AiAction.Type -> "Executing TYPE into field"
                else -> "Executing gesture..."
            }
        }
        LoopStatus.PAUSED -> "Automation paused"
        LoopStatus.COMPLETED -> "Task Completed: ${state.resultMessage ?: "Success"}"
        LoopStatus.FAILED -> "Failed: ${state.errorMessage ?: "Unknown error"}"
    }
}
