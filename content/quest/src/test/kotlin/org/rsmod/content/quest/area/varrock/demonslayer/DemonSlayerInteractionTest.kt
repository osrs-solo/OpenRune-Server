package org.rsmod.content.quest.area.varrock.demonslayer

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.varp.baseVar
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.ResourceLock
import org.rsmod.annotations.InternalApi
import org.rsmod.api.inv.storage.PlayerItemStorage
import org.rsmod.api.invtx.InvTransactionsScript
import org.rsmod.api.player.dialogue.align.TextAlignment
import org.rsmod.api.player.events.interact.NpcEvents
import org.rsmod.api.player.input.ResumePauseButtonInput
import org.rsmod.api.player.interact.NpcInteractions
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.protect.ProtectedAccessContextFactory
import org.rsmod.api.player.protect.clearPendingAction
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.registry.obj.ObjRegistry
import org.rsmod.api.registry.zone.ZoneUpdateMap
import org.rsmod.api.repo.obj.ObjRepository
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.BONES
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.COINS
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.DRAIN_KEY
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.DRAIN_STATE
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.INCANTATION_VARBITS
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.KEYS
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.LAMP
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.ROVIN_KEY
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.SILVERLIGHT
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.SILVERLIGHT_CASE
import org.rsmod.content.quest.area.varrock.demonslayer.DemonSlayerQuest.Companion.TRAIBORN_KEY
import org.rsmod.coroutine.GameCoroutine
import org.rsmod.events.EventBus
import org.rsmod.game.MapClock
import org.rsmod.game.cheat.CheatCommandMap
import org.rsmod.game.client.Client
import org.rsmod.game.entity.Npc
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.InvVirtualStorageHolder
import org.rsmod.game.inv.Inventory
import org.rsmod.game.queue.EngineQueueCache
import org.rsmod.map.CoordGrid
import org.rsmod.plugin.scripts.ScriptContext

@Execution(ExecutionMode.SAME_THREAD)
@ResourceLock("ServerCacheManager")
class DemonSlayerInteractionTest {
    @Test
    fun `the quest stage lives in the demonstart varp`() {
        val varbit =
            checkNotNull(ServerCacheManager.getVarbit("varbit.demonslayer_main".asRSCM(RSCMType.VARBIT)))
        assertEquals("varp.demonstart".asRSCM(RSCMType.VARP), varbit.baseVar.id)
    }

    @Test
    fun `Aris takes exactly one coin and starts the quest after the flashback`() {
        val f = Fixture(stage = 0)
        f.give(COINS to 10)
        f.talk("npc.aris")
        f.finish(listOf(1, 1, 3, 3, 5))
        assertEquals(9, f.player.inv.count(COINS))
        assertEquals(1, f.stage())
        assertEquals(1, f.scenes.flashbacks)
        val words = INCANTATION_VARBITS.map { f.player.vars[it] }
        assertEquals((0..4).toList(), words.sorted())
    }

    @Test
    fun `Aris without money takes nothing and starts nothing`() {
        val f = Fixture(stage = 0)
        f.talk("npc.aris")
        f.finish()
        assertEquals(0, f.stage())
        assertEquals(0, f.scenes.flashbacks)
    }

    @Test
    fun `refusing the fortune keeps the coin`() {
        val f = Fixture(stage = 0)
        f.give(COINS to 1)
        f.talk("npc.aris")
        f.finish(listOf(2))
        assertEquals(1, f.player.inv.count(COINS))
        assertEquals(0, f.stage())
    }

    @Test
    fun `the incantation is generated once and then stays fixed`() {
        val f = Fixture(stage = 1)
        val first = f.quest.incantationWords(f.player)
        repeat(5) { assertEquals(first, f.quest.incantationWords(f.player)) }
        assertEquals(DemonSlayerQuest.WORDS.toSet(), first.toSet())
    }

    @Test
    fun `Rovin hands over his key once and never a duplicate`() {
        val f = Fixture(stage = 2)
        f.talk("npc.captain_rovin")
        f.finish(ROVIN_PATH)
        assertEquals(1, f.player.inv.count(ROVIN_KEY))
        f.talk("npc.captain_rovin")
        f.finish(listOf(3, 1))
        assertEquals(1, f.player.inv.count(ROVIN_KEY))
    }

    @Test
    fun `Rovin keeps his key when the inventory is full`() {
        val f = Fixture(stage = 2)
        f.fill(28)
        f.talk("npc.captain_rovin")
        f.finish(ROVIN_PATH)
        assertEquals(0, f.player.inv.count(ROVIN_KEY))
        assertEquals(28, f.player.inv.count(FILLER))
    }

    @Test
    fun `Rovin replaces a key that was lost`() {
        val f = Fixture(stage = 2)
        assertTrue(f.quest.giveQuestKey(f.access(), ROVIN_KEY))
        f.player.inv[0] = null
        assertTrue(f.quest.giveQuestKey(f.access(), ROVIN_KEY))
        assertFalse(f.quest.giveQuestKey(f.access(), ROVIN_KEY))
        assertEquals(1, f.player.inv.count(ROVIN_KEY))
    }

    @Test
    fun `Traiborn takes bones over several visits and gives one key at 25`() {
        val f = Fixture(stage = 2)
        f.give(BONES to 10)
        assertEquals(10, f.quest.handInBones(f.player))
        assertEquals(0, f.player.inv.count(BONES))
        assertEquals(0, f.player.inv.count(TRAIBORN_KEY))
        f.give(BONES to 20)
        assertEquals(15, f.quest.handInBones(f.player))
        assertEquals(5, f.player.inv.count(BONES))
        assertEquals(1, f.player.inv.count(TRAIBORN_KEY))
        assertEquals(0, f.quest.handInBones(f.player))
        assertEquals(5, f.player.inv.count(BONES))
        assertEquals(1, f.player.inv.count(TRAIBORN_KEY))
    }

    @Test
    fun `the last bone's slot holds the key in a full inventory`() {
        val f = Fixture(stage = 2)
        VarPlayerIntMapSetter.set(f.player, "varbit.demon_slayer_traiborn_bones", 24)
        f.fill(27)
        f.give(BONES to 1)
        assertEquals(1, f.quest.handInBones(f.player))
        assertEquals(1, f.player.inv.count(TRAIBORN_KEY))
        assertEquals(27, f.player.inv.count(FILLER))
        assertEquals(0, f.player.inv.count(BONES))
    }

    @Test
    fun `Traiborn's whole conversation takes the bones and hands the key over`() {
        val f = Fixture(stage = 2)
        VarPlayerIntMapSetter.set(f.player, "varbit.demon_slayer_traiborn_asked", 1)
        f.give(BONES to 25)
        f.talkTraiborn()
        f.finish(listOf(1))
        assertEquals(0, f.player.inv.count(BONES))
        assertEquals(1, f.player.inv.count(TRAIBORN_KEY))
        assertEquals(1, f.scenes.rituals)
    }

    @Test
    fun `a lost Traiborn key means another 25 bones`() {
        val f = Fixture(stage = 2)
        VarPlayerIntMapSetter.set(f.player, "varbit.demon_slayer_traiborn_asked", 1)
        VarPlayerIntMapSetter.set(f.player, "varbit.demon_slayer_traiborn_bones", 25)
        f.talkTraiborn()
        f.finish(listOf(1))
        assertEquals(0, f.player.vars["varbit.demon_slayer_traiborn_bones"])
        assertEquals(0, f.player.inv.count(TRAIBORN_KEY))
    }

    @Test
    fun `three keys become exactly one Silverlight`() {
        val f = Fixture(stage = 2)
        f.give(*KEYS.map { it to 1 }.toTypedArray())
        assertTrue(f.quest.takeKeysForSilverlight(f.player))
        assertEquals(1, f.player.inv.count(SILVERLIGHT))
        KEYS.forEach { assertEquals(0, f.player.inv.count(it)) }
        assertEquals(1, f.player.vars[SILVERLIGHT_CASE])
        f.give(*KEYS.map { it to 1 }.toTypedArray())
        assertFalse(f.quest.takeKeysForSilverlight(f.player))
        assertEquals(1, f.player.inv.count(SILVERLIGHT))
    }

    @Test
    fun `two keys open nothing and keep both keys`() {
        val f = Fixture(stage = 2)
        f.give(TRAIBORN_KEY to 1, ROVIN_KEY to 1)
        assertFalse(f.quest.takeKeysForSilverlight(f.player))
        assertEquals(1, f.player.inv.count(TRAIBORN_KEY))
        assertEquals(1, f.player.inv.count(ROVIN_KEY))
        assertEquals(0, f.player.inv.count(SILVERLIGHT))
        assertEquals(0, f.player.vars[SILVERLIGHT_CASE])
    }

    @Test
    fun `Prysin's handover dialogue swaps the keys through the cutscene`() {
        val f = Fixture(stage = 2)
        f.give(*KEYS.map { it to 1 }.toTypedArray())
        f.talk("npc.sir_prysin")
        f.finish()
        assertEquals(1, f.player.inv.count(SILVERLIGHT))
        KEYS.forEach { assertEquals(0, f.player.inv.count(it)) }
    }

    @Test
    fun `a lost Silverlight is returned once`() {
        val f = Fixture(stage = 2)
        VarPlayerIntMapSetter.set(f.player, SILVERLIGHT_CASE, 1)
        assertTrue(f.quest.returnSilverlight(f.access()))
        assertFalse(f.quest.returnSilverlight(f.access()))
        assertEquals(1, f.player.inv.count(SILVERLIGHT))
    }

    @Test
    fun `a wielded Silverlight is not replaced`() {
        val f = Fixture(stage = 2)
        VarPlayerIntMapSetter.set(f.player, SILVERLIGHT_CASE, 1)
        f.player.worn[3] = InvObj(SILVERLIGHT, 1)
        assertFalse(f.quest.returnSilverlight(f.access()))
        assertEquals(0, f.player.inv.count(SILVERLIGHT))
    }

    @Test
    fun `a full inventory leaves the lost Silverlight with Prysin`() {
        val f = Fixture(stage = 2)
        VarPlayerIntMapSetter.set(f.player, SILVERLIGHT_CASE, 1)
        f.fill(28)
        assertFalse(f.quest.returnSilverlight(f.access()))
        assertFalse(f.quest.returnSilverlight(f.access()))
        assertEquals(0, f.player.inv.count(SILVERLIGHT))
        assertEquals(28, f.player.inv.count(FILLER))
        f.player.inv[0] = null
        assertTrue(f.quest.returnSilverlight(f.access()))
        assertEquals(1, f.player.inv.count(SILVERLIGHT))
    }

    @Test
    fun `Aris gives the two lamps only once`() {
        val f = Fixture(stage = 3)
        assertTrue(f.quest.giveLamps(f.access()))
        assertEquals(2, f.player.inv.count(LAMP))
        assertFalse(f.quest.giveLamps(f.access()))
        assertEquals(2, f.player.inv.count(LAMP))
    }

    @Test
    fun `no lamps leave Aris without two free slots`() {
        val f = Fixture(stage = 3)
        f.fill(27)
        assertFalse(f.quest.giveLamps(f.access()))
        assertEquals(0, f.player.inv.count(LAMP))
        assertEquals(0, f.player.vars["varbit.demon_slayer_lamps_given"])
        f.player.inv[0] = null
        assertTrue(f.quest.giveLamps(f.access()))
        assertEquals(2, f.player.inv.count(LAMP))
    }

    @Test
    fun `the sewer key is picked up once`() {
        val f = Fixture(stage = 2)
        VarPlayerIntMapSetter.set(f.player, DRAIN_STATE, 1)
        assertTrue(f.drain.pickUpSewerKey(f.access()))
        assertFalse(f.drain.pickUpSewerKey(f.access()))
        assertEquals(1, f.player.inv.count(DRAIN_KEY))
        assertEquals(2, f.player.vars[DRAIN_STATE])
    }

    @Test
    fun `the sewer key stays put with a full inventory`() {
        val f = Fixture(stage = 2)
        VarPlayerIntMapSetter.set(f.player, DRAIN_STATE, 1)
        f.fill(28)
        assertFalse(f.drain.pickUpSewerKey(f.access()))
        assertEquals(0, f.player.inv.count(DRAIN_KEY))
        assertEquals(1, f.player.vars[DRAIN_STATE])
    }

    private class FakeScenes : DemonSlayerScenes {
        var flashbacks = 0
        var rituals = 0

        override fun crystalBallStart(access: ProtectedAccess, aris: Npc) {}

        override fun crystalBallGaze(aris: Npc) {}

        override fun crystalBallEnd(access: ProtectedAccess, aris: Npc) {}

        override suspend fun wallyFlashback(access: ProtectedAccess, quest: DemonSlayerQuest) {
            flashbacks++
            quest.incantationWords(access.player)
        }

        override suspend fun silverlightHandover(
            access: ProtectedAccess,
            prysin: Npc,
            quest: DemonSlayerQuest,
        ) {
            quest.takeKeysForSilverlight(access.player)
        }

        override suspend fun traibornRitual(access: ProtectedAccess, traiborn: Npc) {
            rituals++
        }

        override fun releaseLeakedRegion(player: Player) {}
    }

    private class Fixture(stage: Int) {
        val events = EventBus()
        private val client = RecordingClient()
        private val coroutine = GameCoroutine("demon-slayer-test")
        private var result: Result<Unit>? = null
        private val context =
            ProtectedAccessContextFactory.empty()
                .copy(
                    getEventBus = { events },
                    getAlignment = { TextAlignment() },
                    getNpcInteractions = { NpcInteractions(events) },
                )

        @OptIn(InternalApi::class)
        val player =
            Player().apply {
                this.client = this@Fixture.client
                uuid = 790L
                observerUUID = 790L
                slotId = 1
                assignUid()
                coords = CoordGrid(3204, 3424, 0)
                currentMapClock = 100
                processedMapClock = 100
                inv =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.inv".asRSCM())),
                        arrayOfNulls(28),
                    )
                worn =
                    Inventory(
                        checkNotNull(ServerCacheManager.getInventory("inv.worn".asRSCM())),
                        arrayOfNulls(14),
                    )
            }

        private val objRepo = ObjRepository(MapClock(100), ObjRegistry(ZoneUpdateMap()))
        val scenes = FakeScenes()
        val quest = DemonSlayerQuest(objRepo, scenes)
        val drain = DemonSlayerDrain(quest)

        init {
            val scripts = ScriptContext(events, CheatCommandMap(), EngineQueueCache())
            with(quest) { scripts.startup() }
            VarPlayerIntMapSetter.set(player, "varbit.demonslayer_main", stage)
        }

        fun stage() = quest.quest.getQuestStage(player)

        fun access() = ProtectedAccess(player, coroutine, context)

        fun give(vararg objs: Pair<String, Int>) {
            for ((obj, count) in objs) {
                val type = checkNotNull(ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ)))
                val stacks = if (type.stackable) listOf(count) else List(count) { 1 }
                for (amount in stacks) {
                    val slot = (0 until 28).first { player.inv[it] == null }
                    player.inv[slot] = InvObj(obj, amount)
                }
            }
        }

        fun fill(slots: Int) {
            for (slot in 0 until slots) player.inv[slot] = InvObj(FILLER, 1)
        }

        fun talk(npc: String) =
            start { assertTrue(events.publish(this, NpcEvents.Op1(Npc(npc, coords.translateZ(1))))) }

        fun talkTraiborn() {
            val npc = Npc("npc.traiborn", player.coords.translateZ(1))
            start { startDialogue(npc) { with(quest) { traibornDemonSlayer() } } }
        }

        private fun start(block: suspend ProtectedAccess.() -> Unit) {
            player.clearPendingAction(events)
            result = null
            player.activeCoroutine = coroutine
            val access = access()
            val run: suspend () -> Unit = { access.block() }
            run.startCoroutine(
                object : Continuation<Unit> {
                    override val context = EmptyCoroutineContext

                    override fun resumeWith(result: Result<Unit>) {
                        this@Fixture.result = result
                    }
                }
            )
            result?.getOrThrow()
        }

        fun finish(options: List<Int> = emptyList()) {
            val selections = options.iterator()
            repeat(300) {
                if (coroutine.isIdle) return
                advance(selections)
            }
            fail<Unit>("Interaction did not finish: ${client.messages.joinToString("\n")}")
        }

        private fun advance(options: Iterator<Int>) {
            if (coroutine.isAwaiting(ResumePauseButtonInput::class)) {
                val parent =
                    listOf("chat_left", "chat_right", "messagebox", "objectbox", "chatmenu")
                        .firstOrNull { player.ui.containsModal("interface.$it") }
                        ?: error("Unknown dialogue: ${client.messages.joinToString("\n")}")
                val input =
                    when (parent) {
                        "chatmenu" ->
                            ResumePauseButtonInput(
                                "component.chatmenu:options",
                                if (options.hasNext()) options.next() else 1,
                            )
                        "objectbox" -> ResumePauseButtonInput("component.objectbox:universe", 0)
                        else -> ResumePauseButtonInput("component.$parent:continue", -1)
                    }
                coroutine.resumeWith(input)
            } else {
                player.currentMapClock++
                player.processedMapClock = player.currentMapClock
                coroutine.advance()
            }
            result?.getOrThrow()
        }
    }

    private class RecordingClient : Client<Any, Any> {
        val messages = mutableListOf<Any>()

        override fun write(message: Any) {
            messages += message
        }

        override fun close() {}

        override fun read(player: Player) {}

        override fun flush() {}

        override fun flushHighPriority() {}

        override fun unregister(service: Any, player: Player) {}
    }

    companion object {
        private const val FILLER = "obj.bronze_dagger"

        /** Important, demon, very, me, Prysin said so, why did he give you one. */
        private val ROVIN_PATH = listOf(3, 1, 2, 2, 3, 1)

        private val restored = mutableListOf<() -> Unit>()

        @OptIn(InternalApi::class)
        @JvmStatic
        @BeforeAll
        fun cache() {
            ServerCacheManager.init(240).close()
            for ((owner, name) in
                listOf(
                    "org.rsmod.api.invtx.InvTransactionsScriptKt" to "cachedInventoryTransactions",
                    "org.rsmod.api.invtx.VirtualInvTransactionsKt" to "cachedPlayerItemStorage",
                )) {
                val field = Class.forName(owner).getDeclaredField(name).apply { isAccessible = true }
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
