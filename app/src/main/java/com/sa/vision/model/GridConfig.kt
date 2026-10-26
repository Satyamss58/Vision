package com.sa.vision.model

/**
 * Grid configuration representing rows and columns for screen coordinate mapping.
 */
data class GridConfig(
    val rows: Int = 10,
    val columns: Int = 5
) {
    val totalCells: Int
        get() = rows * columns
}

/**
 * Information about an individual grid cell, including bounds and center coordinates.
 */
data class GridCell(
    val cellIndex: Int, // 1-based index (1..totalCells)
    val row: Int,       // 0-based row index
    val col: Int,       // 0-based column index
    val boundsLeft: Float,
    val boundsTop: Float,
    val boundsRight: Float,
    val boundsBottom: Float,
    val centerX: Float,
    val centerY: Float
)
