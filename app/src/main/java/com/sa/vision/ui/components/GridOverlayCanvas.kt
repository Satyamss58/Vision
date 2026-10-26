package com.sa.vision.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sa.vision.model.GridConfig
import com.sa.vision.util.GridOverlayUtil

/**
 * Compose Canvas component that renders a dynamic numbered coordinate grid overlay over the WebView.
 */
@Composable
fun GridOverlayCanvas(
    gridConfig: GridConfig,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0x9900E5FF),
    badgeBgColor: Color = Color(0xE61E1E2C),
    badgeBorderColor: Color = Color(0xFF00E5FF),
    textColor: Color = Color.White
) {
    val density = LocalDensity.current

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        if (width <= 0f || height <= 0f || gridConfig.rows <= 0 || gridConfig.columns <= 0) return@Canvas

        val cells = GridOverlayUtil.calculateGridCells(width, height, gridConfig)
        if (cells.isEmpty()) return@Canvas

        val cellWidth = width / gridConfig.columns
        val cellHeight = height / gridConfig.rows

        val strokeWidthPx = with(density) { 1.5.dp.toPx() }

        // Draw vertical lines
        for (c in 1 until gridConfig.columns) {
            val x = c * cellWidth
            drawLine(
                color = Color.Black.copy(alpha = 0.4f),
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = strokeWidthPx + 2f
            )
            drawLine(
                color = lineColor,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = strokeWidthPx
            )
        }

        // Draw horizontal lines
        for (r in 1 until gridConfig.rows) {
            val y = r * cellHeight
            drawLine(
                color = Color.Black.copy(alpha = 0.4f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = strokeWidthPx + 2f
            )
            drawLine(
                color = lineColor,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = strokeWidthPx
            )
        }

        // Draw badges & numbers
        val textSizeSp = (minOf(cellWidth, cellHeight) * 0.22f).coerceIn(12f, 26f)
        val textSizePx = with(density) { textSizeSp.sp.toPx() }

        val textPaint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = textSizePx
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
        }

        val paddingX = textSizePx * 0.35f
        val paddingY = textSizePx * 0.2f
        val cornerRadiusPx = with(density) { 4.dp.toPx() }

        cells.forEach { cell ->
            val label = "#${cell.cellIndex}"
            val textWidth = textPaint.measureText(label)
            val fontMetrics = textPaint.fontMetrics
            val textHeight = fontMetrics.descent - fontMetrics.ascent

            val badgeWidth = textWidth + (paddingX * 2)
            val badgeHeight = textHeight + (paddingY * 2)

            val margin = strokeWidthPx * 2f
            val badgeLeft = cell.boundsLeft + margin
            val badgeTop = cell.boundsTop + margin

            // Draw Badge background
            drawRoundRect(
                color = badgeBgColor,
                topLeft = Offset(badgeLeft, badgeTop),
                size = Size(badgeWidth, badgeHeight),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx)
            )

            // Draw Badge border
            drawRoundRect(
                color = badgeBorderColor,
                topLeft = Offset(badgeLeft, badgeTop),
                size = Size(badgeWidth, badgeHeight),
                cornerRadius = CornerRadius(cornerRadiusPx, cornerRadiusPx),
                style = Stroke(width = with(density) { 1.dp.toPx() })
            )

            // Draw Text
            val textX = badgeLeft + (badgeWidth / 2f)
            val textY = badgeTop + paddingY - fontMetrics.ascent

            drawContext.canvas.nativeCanvas.drawText(label, textX, textY, textPaint)
        }
    }
}
