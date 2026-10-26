package com.sa.vision.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sa.vision.ui.theme.VisionTheme

@Composable
fun TopPromptBar(
    promptText: String,
    onPromptChange: (String) -> Unit,
    onSendPrompt: () -> Unit,
    onVoiceInputClick: () -> Unit,
    onStartAutomationClick: (() -> Unit)? = null,
    isAutomationActive: Boolean = false,
    isListening: Boolean = false,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = promptText,
                onValueChange = onPromptChange,
                modifier = Modifier
                    .weight(1f),
                placeholder = {
                    Text(
                        text = "Ask Gemini, Claude, or ChatGPT...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = false,
                maxLines = 3,
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                trailingIcon = {
                    AnimatedVisibility(visible = promptText.isNotEmpty()) {
                        IconButton(
                            onClick = { onPromptChange("") },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear prompt",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = {
                        keyboardController?.hide()
                        onSendPrompt()
                    }
                )
            )

            // Mic Icon Button for Voice Input
            val micBgColor by animateColorAsState(
                targetValue = if (isListening) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
                label = "MicBgColor"
            )
            val micTintColor by animateColorAsState(
                targetValue = if (isListening) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                label = "MicTintColor"
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = micBgColor
            ) {
                IconButton(
                    onClick = onVoiceInputClick,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = if (isListening) "Stop voice input" else "Voice input",
                        tint = micTintColor
                    )
                }
            }

            // Start Vision Automation Control Button
            if (onStartAutomationClick != null) {
                val autoBgColor by animateColorAsState(
                    targetValue = if (isAutomationActive) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                    label = "AutoBgColor"
                )
                val autoTintColor by animateColorAsState(
                    targetValue = if (isAutomationActive) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.primary,
                    label = "AutoTintColor"
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = autoBgColor
                ) {
                    IconButton(
                        onClick = {
                            keyboardController?.hide()
                            onStartAutomationClick()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoMode,
                            contentDescription = "Start Vision Automation",
                            tint = autoTintColor
                        )
                    }
                }
            }

            // Send Button
            val isSendEnabled = promptText.isNotBlank()
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isSendEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                IconButton(
                    onClick = {
                        if (isSendEnabled) {
                            keyboardController?.hide()
                            onSendPrompt()
                        }
                    },
                    enabled = isSendEnabled,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send prompt",
                        tint = if (isSendEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TopPromptBarPreview() {
    VisionTheme {
        TopPromptBar(
            promptText = "Explain quantum computing simply",
            onPromptChange = {},
            onSendPrompt = {},
            onVoiceInputClick = {},
            onStartAutomationClick = {},
            isAutomationActive = false,
            isListening = false
        )
    }
}
