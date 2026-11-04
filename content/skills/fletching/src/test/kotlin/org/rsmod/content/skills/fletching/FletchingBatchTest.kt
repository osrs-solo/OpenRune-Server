package org.rsmod.content.skills.fletching

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FletchingBatchTest {
    private val dart = listOf(10, 10)
    private val javelin = listOf(15, 15)

    @Test
    fun `a short dart set makes what the tips allow`() {
        assertEquals(2, batchSize(10, dart, listOf(500, 2), partial = true))
        assertEquals(1, actionsAvailable(10, dart, listOf(500, 2), partial = true))
    }

    @Test
    fun `leftover tips after full sets get one more short action`() {
        assertEquals(10, batchSize(10, dart, listOf(102, 102), partial = true))
        assertEquals(11, actionsAvailable(10, dart, listOf(102, 102), partial = true))
        assertEquals(2, batchSize(10, dart, listOf(2, 2), partial = true))
    }

    @Test
    fun `the scarcer input limits a partial set`() {
        assertEquals(7, batchSize(15, javelin, listOf(7, 40), partial = true))
        assertEquals(3, actionsAvailable(15, javelin, listOf(40, 31), partial = true))
    }

    @Test
    fun `nothing held makes nothing`() {
        assertEquals(0, batchSize(10, dart, listOf(0, 50), partial = true))
        assertEquals(0, actionsAvailable(10, dart, listOf(0, 50), partial = true))
    }

    @Test
    fun `fixed recipes still need a whole set`() {
        val bow = listOf(1, 1)
        assertEquals(1, batchSize(1, bow, listOf(1, 1), partial = false))
        assertEquals(0, batchSize(15, listOf(1), listOf(0), partial = false))
        assertEquals(15, batchSize(15, listOf(1), listOf(3), partial = false))
        assertEquals(3, actionsAvailable(15, listOf(1), listOf(3), partial = false))
    }

    @Test
    fun `stack totals near the int limit do not overflow`() {
        val max = Int.MAX_VALUE
        assertEquals(10, batchSize(10, dart, listOf(max, max), partial = true))
        assertEquals(max / 10 + 1, actionsAvailable(10, dart, listOf(max, max), partial = true))
    }
}
