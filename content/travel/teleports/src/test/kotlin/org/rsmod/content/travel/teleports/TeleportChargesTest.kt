package org.rsmod.content.travel.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.content.travel.teleports.TeleportCharges.charge
import org.rsmod.content.travel.teleports.TeleportCharges.spendCharge
import org.rsmod.content.travel.teleports.TeleportCharges.uncharge
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.inv.isType
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class TeleportChargesTest {
    @Test
    fun `a chain charge swaps the obj in its slot and the last one leaves the uncharged form`() {
        val f = Fixture()
        f.player.inv[3] = InvObj(GLORY_1)
        assertTrue(f.run { spendCharge(f.item(GLORY_1), inv, 3) })
        assertTrue(f.player.inv[3].isType(GLORY))
        assertEquals(1, f.player.inv.count(GLORY))
        assertEquals(0, f.player.inv.count(GLORY_1))
    }

    @Test
    fun `the last charge of a dueling ring destroys exactly that ring`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(DUELING_1)
        f.player.inv[1] = InvObj(DUELING_1)
        assertTrue(f.run { spendCharge(f.item(DUELING_1), inv, 0) })
        assertNull(f.player.inv[0])
        assertTrue(f.player.inv[1].isType(DUELING_1))
    }

    @Test
    fun `a worn chain item steps down in the worn slot`() {
        val f = Fixture()
        f.player.worn[RING_SLOT] = InvObj(DUELING_8)
        assertTrue(f.run { spendCharge(f.item(DUELING_8), worn, RING_SLOT) })
        assertTrue(f.player.worn[RING_SLOT].isType("obj.ring_of_dueling_7"))
    }

    @Test
    fun `an item that left its slot spends nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj("obj.coins", 10)
        assertFalse(f.run { spendCharge(f.item(GLORY_1), inv, 0) })
        assertFalse(f.run { spendCharge(f.item(GLORY_1), inv, 1) })
        assertEquals(10, f.player.inv.count("obj.coins"))

        f.player.inv[2] = InvObj(TALISMAN)
        f.vars(5)
        assertFalse(f.run { spendCharge(f.item(TALISMAN), inv, 0) })
        assertEquals(5, f.charges())
    }

    @Test
    fun `a varbit item spends one charge and refuses at zero`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(TALISMAN)
        f.vars(1)
        assertTrue(f.run { spendCharge(f.item(TALISMAN), inv, 0) })
        assertEquals(0, f.charges())
        assertFalse(f.run { spendCharge(f.item(TALISMAN), inv, 0) })
        assertEquals(0, f.charges())
        assertTrue(f.player.inv[0].isType(TALISMAN))
    }

    @Test
    fun `fangs charge an inert talisman and every fang taken becomes a charge`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(INERT)
        f.player.inv[1] = InvObj(FANG, 40)
        f.run { charge(f.item(TALISMAN), 0) }
        assertTrue(f.player.inv[0].isType(TALISMAN))
        assertEquals(0, f.player.inv.count(FANG))
        assertEquals(0, f.player.inv.count(INERT))
        assertEquals(40, f.charges())
    }

    @Test
    fun `charging stops at the cap and leaves the spare fangs`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(TALISMAN)
        f.player.inv[1] = InvObj(FANG, Int.MAX_VALUE)
        f.vars(995)
        f.run { charge(f.item(TALISMAN), 0) }
        assertEquals(1000, f.charges())
        assertEquals(Int.MAX_VALUE - 5, f.player.inv.count(FANG))
        f.run { charge(f.item(TALISMAN), 0) }
        assertEquals(1000, f.charges())
        assertEquals(Int.MAX_VALUE - 5, f.player.inv.count(FANG))
    }

    @Test
    fun `charging with no fangs or a missing talisman changes nothing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(INERT)
        f.run { charge(f.item(TALISMAN), 0) }
        assertTrue(f.player.inv[0].isType(INERT))
        assertEquals(0, f.charges())

        f.player.inv[1] = InvObj(FANG, 10)
        f.run { charge(f.item(TALISMAN), 5) }
        assertTrue(f.player.inv[0].isType(INERT))
        assertEquals(10, f.player.inv.count(FANG))
        assertEquals(0, f.charges())
    }

    @Test
    fun `uncharging refunds every charge as a fang`() {
        val f = Fixture()
        f.player.inv[4] = InvObj(TALISMAN)
        f.vars(30)
        f.run { uncharge(f.item(TALISMAN), inv, 4) }
        assertTrue(f.player.inv[4].isType(INERT))
        assertEquals(30, f.player.inv.count(FANG))
        assertEquals(0, f.charges())
    }

    @Test
    fun `uncharging with no room for the fangs rolls back`() {
        val f = Fixture()
        for (slot in 0 until f.player.inv.size) {
            f.player.inv[slot] = InvObj("obj.coins".takeIf { slot == 0 } ?: "obj.bones")
        }
        f.player.inv[5] = InvObj(TALISMAN)
        f.vars(30)
        f.run { uncharge(f.item(TALISMAN), inv, 5) }
        assertTrue(f.player.inv[5].isType(TALISMAN))
        assertEquals(0, f.player.inv.count(FANG))
        assertEquals(30, f.charges())
    }

    @Test
    fun `uncharging into a nearly full fang stack rolls back instead of overflowing`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(TALISMAN)
        f.player.inv[1] = InvObj(FANG, Int.MAX_VALUE - 10)
        f.vars(30)
        f.run { uncharge(f.item(TALISMAN), inv, 0) }
        assertTrue(f.player.inv[0].isType(TALISMAN))
        assertEquals(Int.MAX_VALUE - 10, f.player.inv.count(FANG))
        assertEquals(30, f.charges())
    }

    @Test
    fun `a second talisman uncharged after the first refunds nothing more`() {
        val f = Fixture()
        f.player.inv[0] = InvObj(TALISMAN)
        f.player.inv[1] = InvObj(TALISMAN)
        f.vars(30)
        f.run { uncharge(f.item(TALISMAN), inv, 0) }
        f.run { uncharge(f.item(TALISMAN), inv, 1) }
        assertEquals(30, f.player.inv.count(FANG))
        assertEquals(2, f.player.inv.count(INERT))
        assertEquals(0, f.charges())
    }

    private class Fixture {
        private val coroutine = GameCoroutine("teleport-charges-test")
        private val context =
            ProtectedAccessContextFactory.empty().copy(
                getEventBus = { EventBus() },
                getAlignment = { TextAlignment() },
            )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = SilentClient()
                uuid = 797L
                slotId = 1
                assignUid()
                coords = CoordGrid(3222, 3218, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv = Inventory(invType("inv.inv"), arrayOfNulls(28))
                worn = Inventory(invType("inv.worn"), arrayOfNulls(14))
            }

        private val access = ProtectedAccess(player, coroutine, context)

        fun item(obj: String): TeleportItem = checkNotNull(TeleportItems.byObj[obj]) { obj }

        fun <T> run(block: ProtectedAccess.() -> T): T {
            player.activeCoroutine = coroutine
            var result: T? = null
            val body: suspend () -> Unit = { result = access.block() }
            body.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        result.getOrThrow()
                    }
                }
            )
            @Suppress("UNCHECKED_CAST")
            return result as T
        }

        fun vars(charges: Int) {
            access.vars[CHARGES] = charges
        }

        fun charges(): Int = access.vars[CHARGES]
    }

    private class SilentClient : Client<Any, Any> {
        override fun write(message: Any) {}

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val GLORY = "obj.amulet_of_glory"
        private const val GLORY_1 = "obj.amulet_of_glory_1"
        private const val DUELING_1 = "obj.ring_of_dueling_1"
        private const val DUELING_8 = "obj.ring_of_dueling_8"
        private const val TALISMAN = "obj.xeric_talisman"
        private const val INERT = "obj.xeric_talisman_empty"
        private const val FANG = "obj.lizardman_fang"
        private const val CHARGES = "varbit.charges_xerics_talisman_quantity"
        private const val RING_SLOT = 12
        private val restored = mutableListOf<() -> Unit>()

        private fun invType(name: String) =
            checkNotNull(ServerCacheManager.getInventory(name.asRSCM()))

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(241).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field = Class.forName(owner).getDeclaredField(name)
                field.isAccessible = true
                val old = field.get(null)
                restored += { field.set(null, old) }
            }
            val oldStorage = InvVirtualStorageHolder.instance
            restored += { InvVirtualStorageHolder.instance = oldStorage }
            with(InvTransactionsScript(PlayerItemStorage(emptySet()))) {
                ScriptContext(EventBus(), CheatCommandMap(), EngineQueueCache()).startup()
            }
        }

        @JvmStatic
        @AfterAll
        fun restore() {
            restored.asReversed().forEach { it() }
            restored.clear()
        }
    }
}
