package com.sa.vision.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sa.vision.ui.theme.VisionTheme

@Composable
fun WebControlBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    isGridOverlayVisible: Boolean = false,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onRefreshClick: () -> Unit,
    onHomeClick: () -> Unit,
    onToggleGridClick: () -> Unit,
    onCaptureScreenClick: () -> Unit,
    onCopyPromptClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onBackClick,
                    enabled = canGoBack
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate back",
                        tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                IconButton(
                    onClick = onForwardClick,
                    enabled = canGoForward
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Navigate forward",
                        tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                }

                IconButton(onClick = onRefreshClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh web page",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onHomeClick) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Go to platform home",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onToggleGridClick) {
                    Icon(
                        imageVector = if (isGridOverlayVisible) Icons.Default.GridOn else Icons.Default.GridOff,
                        contentDescription = "Toggle Grid Overlay",
                        tint = if (isGridOverlayVisible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(onClick = onCaptureScreenClick) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "Capture screen and grid overlay",
                        tint = MaterialTheme.colorScheme.secondary
                    )
                }

                IconButton(onClick = onCopyPromptClick) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy prompt to clipboard",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WebControlBarPreview() {
    VisionTheme {
        WebControlBar(
            canGoBack = true,
            canGoForward = false,
            isGridOverlayVisible = true,
            onBackClick = {},
            onForwardClick = {},
            onRefreshClick = {},
            onHomeClick = {},
            onToggleGridClick = {},
            onCaptureScreenClick = {},
            onCopyPromptClick = {}
        )
    }
}
