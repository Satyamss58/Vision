package com.sa.vision.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.sa.vision.model.AiPlatform
import com.sa.vision.ui.theme.VisionTheme

@Composable
fun AiTabRow(
    selectedPlatform: AiPlatform,
    onPlatformSelected: (AiPlatform) -> Unit,
    modifier: Modifier = Modifier
) {
    val platforms = AiPlatform.entries

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp
    ) {
        PrimaryTabRow(
            selectedTabIndex = selectedPlatform.ordinal,
            modifier = Modifier.fillMaxWidth(),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            contentColor = MaterialTheme.colorScheme.primary
        ) {
            platforms.forEach { platform ->
                val selected = platform == selectedPlatform
                Tab(
                    selected = selected,
                    onClick = { onPlatformSelected(platform) },
                    modifier = Modifier.height(48.dp),
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Icon(
                                imageVector = platform.icon,
                                contentDescription = platform.description,
                                tint = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = platform.displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AiTabRowPreview() {
    VisionTheme {
        AiTabRow(
            selectedPlatform = AiPlatform.GEMINI,
            onPlatformSelected = {}
        )
    }
}
