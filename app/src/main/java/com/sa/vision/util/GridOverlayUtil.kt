package com.sa.vision.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import com.sa.vision.model.GridCell
import com.sa.vision.model.GridConfig
import kotlin.math.min

object GridOverlayUtil {

    /**
     * Computes the list of GridCells given image or container dimensions and GridConfig.
     */
    fun calculateGridCells(
        width: Float,
        height: Float,
        gridConfig: GridConfig
    ): List<GridCell> {
        if (width <= 0f || height <= 0f || gridConfig.rows <= 0 || gridConfig.columns <= 0) {
            return emptyList()
        }

        val cellWidth = width / gridConfig.columns
        val cellHeight = height / gridConfig.rows
        val cells = ArrayList<GridCell>(gridConfig.totalCells)

        var index = 1
        for (r in 0 until gridConfig.rows) {
            val top = r * cellHeight
            val bottom = if (r == gridConfig.rows - 1) height else (r + 1) * cellHeight
            for (c in 0 until gridConfig.columns) {
                val left = c * cellWidth
                val right = if (c == gridConfig.columns - 1) width else (c + 1) * cellWidth
                val centerX = left + (right - left) / 2f
                val centerY = top + (bottom - top) / 2f

                cells.add(
                    GridCell(
                        cellIndex = index,
                        row = r,
                        col = c,
                        boundsLeft = left,
                        boundsTop = top,
                        boundsRight = right,
                        boundsBottom = bottom,
                        centerX = centerX,
                        centerY = centerY
                    )
                )
                index++
            }
        }
        return cells
    }

    /**
     * Composites a dynamic coordinate grid overlay layer with legible numbered badges directly onto a bitmap.
     */
    fun drawGridOnBitmap(
        sourceBitmap: Bitmap,
        gridConfig: GridConfig,
        showLabels: Boolean = true
    ): Bitmap {
        val composited = sourceBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(composited)
        val width = composited.width.toFloat()
        val height = composited.height.toFloat()

        val cells = calculateGridCells(width, height, gridConfig)
        if (cells.isEmpty()) return composited

        val minDimension = min(width, height)
        val strokeWidth = (minDimension / 400f).coerceIn(2f, 6f)

        // Paint for Grid Lines
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#8000E5FF") // Semi-transparent Cyan
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth
        }

        // Paint for Grid Line Border/Outline
        val lineShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#60000000")
            style = Paint.Style.STROKE
            this.strokeWidth = strokeWidth + 2f
        }

        // Draw vertical and horizontal lines
        val cellWidth = width / gridConfig.columns
        val cellHeight = height / gridConfig.rows

        for (c in 1 until gridConfig.columns) {
            val x = c * cellWidth
            canvas.drawLine(x, 0f, x, height, lineShadowPaint)
            canvas.drawLine(x, 0f, x, height, linePaint)
        }
        for (r in 1 until gridConfig.rows) {
            val y = r * cellHeight
            canvas.drawLine(0f, y, width, y, lineShadowPaint)
            canvas.drawLine(0f, y, width, y, linePaint)
        }

        if (!showLabels) return composited

        // Paint configuration for Badge and Text
        val textSizePx = (min(cellWidth, cellHeight) * 0.26f).coerceIn(18f, 42f)

        val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#D81E1E2C") // Dark translucent background badge
            style = Paint.Style.FILL
        }

        val badgeBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FF00E5FF") // High-contrast border
            style = Paint.Style.STROKE
            this.strokeWidth = (strokeWidth * 0.8f).coerceAtLeast(1.5f)
        }

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#FFFFFFFF") // Crisp white text
            textSize = textSizePx
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val paddingX = (textSizePx * 0.4f).coerceAtLeast(8f)
        val paddingY = (textSizePx * 0.25f).coerceAtLeast(4f)
        val cornerRadius = (textSizePx * 0.3f).coerceAtLeast(6f)

        // Draw badges for each cell
        cells.forEach { cell ->
            val label = "#${cell.cellIndex}"
            val textWidth = textPaint.measureText(label)
            val fontMetrics = textPaint.fontMetrics
            val textHeight = fontMetrics.descent - fontMetrics.ascent

            val badgeWidth = textWidth + (paddingX * 2)
            val badgeHeight = textHeight + (paddingY * 2)

            // Place badge in top-left of cell with small margin
            val margin = (strokeWidth * 2f).coerceAtLeast(4f)
            val badgeLeft = cell.boundsLeft + margin
            val badgeTop = cell.boundsTop + margin
            val badgeRight = badgeLeft + badgeWidth
            val badgeBottom = badgeTop + badgeHeight

            val badgeRect = RectF(badgeLeft, badgeTop, badgeRight, badgeBottom)

            // Draw badge background
            canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, badgeBgPaint)
            // Draw badge border
            canvas.drawRoundRect(badgeRect, cornerRadius, cornerRadius, badgeBorderPaint)

            // Draw text centered within badge
            val textX = badgeLeft + (badgeWidth / 2f)
            val textY = badgeTop + paddingY - fontMetrics.ascent

            canvas.drawText(label, textX, textY, textPaint)
        }

        return composited
    }
}
