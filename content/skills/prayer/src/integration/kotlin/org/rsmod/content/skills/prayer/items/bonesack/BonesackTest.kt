package org.rsmod.content.skills.prayer.items.bonesack

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class BonesackTest {
    @Test
    fun GameTestState.`fill takes mixed bones up to 28 and leaves the rest`() =
        runGameTest(BonesackScript::class) {
            player.clearInv()
            player.inv[0] = InvObj(Bonesack.OBJ.asRSCM(RSCMType.OBJ), 1)
            for (slot in 1..20) player.inv[slot] = InvObj(BONES.asRSCM(RSCMType.OBJ), 1)
            for (slot in 21..27) player.inv[slot] = InvObj(BIG.asRSCM(RSCMType.OBJ), 1)
            Bonesack.empty(player, player.inv)

            assertEquals(27, Bonesack.fill(player, player.inv))
            assertEquals(27, Bonesack.total(player))
            assertEquals(0, player.inv.count(BONES) + player.inv.count(BIG))

            for (slot in 1..5) player.inv[slot] = InvObj(DRAGON.asRSCM(RSCMType.OBJ), 1)
            assertEquals(1, Bonesack.fill(player, player.inv))
            assertEquals(Bonesack.CAPACITY, Bonesack.total(player))
            assertEquals(4, player.inv.count(DRAGON))
            assertEquals(0, Bonesack.fill(player, player.inv))
            assertEquals(4, player.inv.count(DRAGON))
        }

    @Test
    fun GameTestState.`a single type fills only that bone`() =
        runGameTest(BonesackScript::class) {
            player.clearInv()
            player.inv[0] = InvObj(Bonesack.OBJ.asRSCM(RSCMType.OBJ), 1)
            player.inv[1] = InvObj(BONES.asRSCM(RSCMType.OBJ), 1)
            player.inv[2] = InvObj(BIG.asRSCM(RSCMType.OBJ), 1)
            Bonesack.empty(player, player.inv)

            assertEquals(1, Bonesack.fill(player, player.inv, listOf(BIG)))
            assertEquals(1, player.inv.count(BONES))
            assertEquals(mapOf(BIG to 1), Bonesack.contents(player))
        }

    @Test
    fun GameTestState.`emptying into a nearly full inventory keeps the overflow`() =
        runGameTest(BonesackScript::class) {
            player.clearInv()
            player.inv[0] = InvObj(Bonesack.OBJ.asRSCM(RSCMType.OBJ), 1)
            for (slot in 1..10) player.inv[slot] = InvObj(BONES.asRSCM(RSCMType.OBJ), 1)
            Bonesack.empty(player, player.inv)
            assertEquals(10, Bonesack.fill(player, player.inv))
            for (slot in 1..24) player.inv[slot] = InvObj(DAGGER.asRSCM(RSCMType.OBJ), 1)

            assertEquals(3, Bonesack.empty(player, player.inv))
            assertEquals(3, player.inv.count(BONES))
            assertEquals(7, Bonesack.total(player))

            player.clearInv()
            assertEquals(7, Bonesack.empty(player, player.inv))
            assertEquals(7, player.inv.count(BONES))
            assertTrue(Bonesack.contents(player).isEmpty())
        }

    @Test
    fun GameTestState.`every prayer bone is storable and no ashes are`() = runBasicGameTest {
        assertTrue(BONES in Bonesack.bones)
        assertTrue(DRAGON in Bonesack.bones)
        assertTrue(Bonesack.bones.none { "ashes" in it })
    }

    private companion object {
        const val BONES = "obj.bones"
        const val BIG = "obj.big_bones"
        const val DRAGON = "obj.dragon_bones"
        const val DAGGER = "obj.bronze_dagger"
    }
}
