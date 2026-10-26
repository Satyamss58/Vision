package com.sa.vision

import com.sa.vision.model.GridConfig
import com.sa.vision.util.GridOverlayUtil
import org.junit.Assert.assertEquals
import org.junit.Test

class GridOverlayUtilTest {

    @Test
    fun testCalculateGridCells_validDimensions() {
        val width = 1080f
        val height = 2400f
        val config = GridConfig(rows = 10, columns = 5)

        val cells = GridOverlayUtil.calculateGridCells(width, height, config)

        assertEquals(50, cells.size)

        // Cell #1 (top-left)
        val cell1 = cells.first()
        assertEquals(1, cell1.cellIndex)
        assertEquals(0, cell1.row)
        assertEquals(0, cell1.col)
        assertEquals(0f, cell1.boundsLeft, 0.01f)
        assertEquals(0f, cell1.boundsTop, 0.01f)
        assertEquals(216f, cell1.boundsRight, 0.01f)
        assertEquals(240f, cell1.boundsBottom, 0.01f)
        assertEquals(108f, cell1.centerX, 0.01f)
        assertEquals(120f, cell1.centerY, 0.01f)

        // Cell #50 (bottom-right)
        val cell50 = cells.last()
        assertEquals(50, cell50.cellIndex)
        assertEquals(9, cell50.row)
        assertEquals(4, cell50.col)
        assertEquals(864f, cell50.boundsLeft, 0.01f)
        assertEquals(2160f, cell50.boundsTop, 0.01f)
        assertEquals(1080f, cell50.boundsRight, 0.01f)
        assertEquals(2400f, cell50.boundsBottom, 0.01f)
        assertEquals(972f, cell50.centerX, 0.01f)
        assertEquals(2280f, cell50.centerY, 0.01f)
    }

    @Test
    fun testCalculateGridCells_zeroOrNegativeDimensions() {
        val config = GridConfig(rows = 5, columns = 5)
        val cells = GridOverlayUtil.calculateGridCells(0f, 1000f, config)
        assertEquals(0, cells.size)
    }
}
