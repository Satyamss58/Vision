package com.sa.vision.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.ui.graphics.vector.ImageVector

enum class AiPlatform(
    val displayName: String,
    val url: String,
    val icon: ImageVector,
    val description: String
) {
    GEMINI(
        displayName = "Gemini",
        url = "https://gemini.google.com",
        icon = Icons.Default.AutoAwesome,
        description = "Google Gemini AI"
    ),
    CLAUDE(
        displayName = "Claude",
        url = "https://claude.ai",
        icon = Icons.Default.Psychology,
        description = "Anthropic Claude AI"
    ),
    CHAT_GPT(
        displayName = "ChatGPT",
        url = "https://chatgpt.com",
        icon = Icons.Default.ChatBubbleOutline,
        description = "OpenAI ChatGPT"
    )
}
