package org.rsmod.content.skills.magic.runepouch

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.testing.GameTestState
import org.rsmod.game.inv.InvObj

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class DivineRunePouchScriptTest {
    @Test
    fun GameTestState.`the thread turns a pouch divine and keeps its runes`() =
        runInjectedGameTest(DivineRunePouchScript::class, null, DivineRunePouchScript::class) {
            script ->
            player.clearInv()
            player.inv[0] = InvObj(PLAIN.asRSCM(RSCMType.OBJ), 1)
            player.inv[1] = InvObj(THREAD.asRSCM(RSCMType.OBJ), 1)
            RunePouch.setSlot(player, 0, "obj.lawrune", 500)

            with(script) { player.withProtectedAccess { assertTrue(weaveThread(PLAIN, DIVINE)) } }

            assertEquals(0, player.inv.count(PLAIN))
            assertEquals(0, player.inv.count(THREAD))
            assertEquals(1, player.inv.count(DIVINE))
            assertEquals("obj.lawrune" to 500, RunePouch.slot(player, 0))
            assertEquals(4, RunePouch.slotCount(player))
        }

    @Test
    fun GameTestState.`without a thread nothing changes`() =
        runInjectedGameTest(DivineRunePouchScript::class, null, DivineRunePouchScript::class) {
            script ->
            player.clearInv()
            player.inv[0] = InvObj(PLAIN.asRSCM(RSCMType.OBJ), 1)

            with(script) { player.withProtectedAccess { assertFalse(weaveThread(PLAIN, DIVINE)) } }

            assertEquals(1, player.inv.count(PLAIN))
            assertEquals(0, player.inv.count(DIVINE))
        }

    @Test
    fun GameTestState.`the locked pouch becomes the locked divine pouch`() =
        runInjectedGameTest(DivineRunePouchScript::class, null, DivineRunePouchScript::class) {
            script ->
            player.clearInv()
            player.inv[0] = InvObj(LOCKED.asRSCM(RSCMType.OBJ), 1)
            player.inv[1] = InvObj(THREAD.asRSCM(RSCMType.OBJ), 2)

            with(script) {
                player.withProtectedAccess { assertTrue(weaveThread(LOCKED, LOCKED_DIVINE)) }
                player.withProtectedAccess { assertFalse(weaveThread(LOCKED, LOCKED_DIVINE)) }
            }

            assertEquals(1, player.inv.count(THREAD))
            assertEquals(1, player.inv.count(LOCKED_DIVINE))
            assertEquals(0, player.inv.count(LOCKED))
        }

    @Test
    fun GameTestState.`reverting needs the fourth slot empty`() =
        runInjectedGameTest(DivineRunePouchScript::class, null, DivineRunePouchScript::class) {
            script ->
            player.clearInv()
            player.inv[0] = InvObj(DIVINE.asRSCM(RSCMType.OBJ), 1)
            RunePouch.setSlot(player, 3, "obj.deathrune", 100)

            with(script) { player.withProtectedAccess { assertFalse(unweave()) } }
            assertEquals(1, player.inv.count(DIVINE))

            RunePouch.setSlot(player, 3, null, 0)
            with(script) { player.withProtectedAccess { assertTrue(unweave()) } }
            assertEquals(0, player.inv.count(DIVINE))
            assertEquals(1, player.inv.count(PLAIN))
        }

    private companion object {
        const val THREAD = "obj.thread_of_elidinis"
        const val PLAIN = "obj.bh_rune_pouch"
        const val DIVINE = "obj.divine_rune_pouch"
        const val LOCKED = "obj.bh_rune_pouch_trouver"
        const val LOCKED_DIVINE = "obj.divine_rune_pouch_trouver"
    }
}
