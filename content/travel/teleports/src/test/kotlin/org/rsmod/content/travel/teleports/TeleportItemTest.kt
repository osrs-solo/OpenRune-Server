package org.rsmod.content.travel.teleports

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.rsmod.map.CoordGrid

class TeleportItemTest {
    private val somewhere =
        listOf(TeleportDestination("Somewhere", CoordGrid(3222, 3218, 0), wilderness = false))

    private val glory =
        TeleportItem(
            label = "amulet",
            objs = (4 downTo 1).map { "obj.amulet_of_glory_$it" },
            destinations = somewhere,
            uncharged = "obj.amulet_of_glory",
        )

    private val dueling =
        TeleportItem(
            label = "ring",
            objs = (2 downTo 1).map { "obj.ring_of_dueling_$it" },
            destinations = somewhere,
        )

    private val talisman =
        TeleportItem(
            label = "talisman",
            objs = listOf("obj.xeric_talisman"),
            destinations = somewhere,
            chargeVarbit = "varbit.charges_xerics_talisman_quantity",
            maxCharges = 1000,
        )

    @Test
    fun `a chain steps down one obj per charge and ends uncharged or as dust`() {
        assertEquals("obj.amulet_of_glory_3", glory.afterCharge("obj.amulet_of_glory_4"))
        assertEquals("obj.amulet_of_glory", glory.afterCharge("obj.amulet_of_glory_1"))
        assertEquals(3, glory.chargesAfter("obj.amulet_of_glory_4"))
        assertEquals(0, glory.chargesAfter("obj.amulet_of_glory_1"))
        assertNull(dueling.afterCharge("obj.ring_of_dueling_1"))
    }

    @Test
    fun `objs outside the chain and varbit items keep their obj`() {
        assertEquals("obj.coins", glory.afterCharge("obj.coins"))
        assertNull(glory.chargesAfter("obj.coins"))
        assertEquals("obj.xeric_talisman", talisman.afterCharge("obj.xeric_talisman"))
        assertNull(talisman.chargesAfter("obj.xeric_talisman"))
    }

    @Test
    fun `charging never goes past the cap or below zero`() {
        assertEquals(10, talisman.chargesToAdd(current = 0, available = 10))
        assertEquals(2, talisman.chargesToAdd(current = 998, available = 10))
        assertEquals(0, talisman.chargesToAdd(current = 1000, available = 10))
        assertEquals(0, talisman.chargesToAdd(current = 1200, available = 10))
        assertEquals(1000, talisman.chargesToAdd(current = 0, available = Int.MAX_VALUE))
        assertEquals(0, glory.chargesToAdd(current = 0, available = 10))
    }
}
