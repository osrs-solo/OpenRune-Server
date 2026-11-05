package org.rsmod.content.skills.magic.runepouch

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import dev.openrune.types.util.UncheckedType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.rsmod.api.combat.weapon.WeaponSpeeds
import org.rsmod.api.combat.weapon.scripts.WeaponAttackStylesScript
import org.rsmod.api.combat.weapon.styles.AttackStyles
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.testing.GameTestState
import org.rsmod.api.testing.scope.GameTestScope
import org.rsmod.content.interfaces.bank.BankRunePouchView
import org.rsmod.content.interfaces.bank.bankCapacity
import org.rsmod.content.interfaces.bank.openBank
import org.rsmod.content.interfaces.bank.runePouchViewOpen
import org.rsmod.content.interfaces.bank.scripts.BankInvScript
import org.rsmod.content.interfaces.bank.scripts.BankOpenScript
import org.rsmod.content.interfaces.bank.scripts.BankRunePouchScript
import org.rsmod.game.inv.InvObj
import org.rsmod.game.inv.Inventory

@OptIn(UncheckedType::class)
@Execution(ExecutionMode.SAME_THREAD)
class BankRunePouchPanelTest {
    @Test
    fun GameTestState.`configure opens the pouch panel and keeps the bank open`() = panelTest {
        player.inv[0] = InvObj(id(POUCH), 1)
        configure(0)

        assertEquals(BankRunePouchView.REGULAR, player.vars[BankRunePouchView.VIEW_VARBIT])
        assertTrue(player.ui.containsModal("interface.bankmain"))
        assertFalse(player.ui.containsModal("interface.rune_pouch"))

        button("component.bankside:runepouch_dismiss")
        assertFalse(player.runePouchViewOpen)
    }

    @Test
    fun GameTestState.`a divine pouch opens the four slot view`() = panelTest {
        player.inv[0] = InvObj(id(DIVINE), 1)
        configure(0)
        assertEquals(BankRunePouchView.DIVINE, player.vars[BankRunePouchView.VIEW_VARBIT])
    }

    @Test
    fun GameTestState.`withdrawing runes with the panel open fills the pouch`() = panelTest {
        player.inv[0] = InvObj(id(POUCH), 1)
        setBank(InvObj(id(AIR), 20_000), InvObj(id(LAW), 50))
        configure(0)
        val before = totals(AIR, LAW)

        withdraw(0, WITHDRAW_ALL)
        withdraw(1, WITHDRAW_ALL)

        assertEquals(16_000, RunePouch.storedAmount(player, AIR))
        assertEquals(50, RunePouch.storedAmount(player, LAW))
        assertEquals(4_000, bank.count(AIR))
        assertEquals(0, player.inv.count(AIR))
        assertEquals(before, totals(AIR, LAW))
    }

    @Test
    fun GameTestState.`withdraw all tops up a slot that already holds the rune`() = panelTest {
        player.inv[0] = InvObj(id(DIVINE), 1)
        RunePouch.setSlot(player, 0, FIRE, 10)
        setBank(InvObj(id(AIR), 5), InvObj(id(FIRE), 10_968))
        configure(0)
        val before = totals(FIRE)

        withdraw(1, WITHDRAW_ALL)

        assertEquals(FIRE to 10_978, RunePouch.slot(player, 0))
        assertEquals(0, bank.count(FIRE))
        assertEquals(before, totals(FIRE))
    }

    @Test
    fun GameTestState.`withdrawing from a slot outside every tab still lands in the pouch`() =
        panelTest {
            player.inv[0] = InvObj(id(DIVINE), 1)
            RunePouch.setSlot(player, 0, FIRE, 10)
            setBank(InvObj(id(AIR), 5), InvObj(id(FIRE), 10_968))
            VarPlayerIntMapSetter.set(player, "varbit.bank_tab_main", 1)
            configure(0)
            val before = totals(FIRE)

            withdraw(1, WITHDRAW_ALL)

            assertEquals(FIRE to 10_978, RunePouch.slot(player, 0))
            assertEquals(before, totals(FIRE))
        }

    @Test
    fun GameTestState.`a full pouch slot sends further withdrawals to the inventory`() = panelTest {
        player.inv[0] = InvObj(id(POUCH), 1)
        RunePouch.setSlot(player, 0, AIR, RunePouch.CAPACITY)
        setBank(InvObj(id(AIR), 100))
        configure(0)
        val before = totals(AIR)

        withdraw(0, WITHDRAW_ALL)

        assertEquals(RunePouch.CAPACITY, RunePouch.storedAmount(player, AIR))
        assertEquals(100, player.inv.count(AIR))
        assertEquals(before, totals(AIR))
    }

    @Test
    fun GameTestState.`depositing a pouch slot moves exactly that many into the bank`() =
        panelTest {
            player.inv[0] = InvObj(id(POUCH), 1)
            RunePouch.setSlot(player, 0, FIRE, 300)
            configure(0)
            val before = totals(FIRE)

            pouchSlot(0, IfButtonOp.Op3)
            assertEquals(295, RunePouch.storedAmount(player, FIRE))
            assertEquals(5, bank.count(FIRE))

            pouchSlot(0, IfButtonOp.Op7)
            assertNull(RunePouch.slot(player, 0))
            assertEquals(300, bank.count(FIRE))
            assertEquals(before, totals(FIRE))
        }

    @Test
    fun GameTestState.`a full bank keeps the runes in the pouch`() = panelTest {
        player.inv[0] = InvObj(id(POUCH), 1)
        RunePouch.setSlot(player, 0, FIRE, 300)
        setBank(InvObj(id(AIR), 1))
        player.bankCapacity = 1
        configure(0)

        button("component.bankside:runepouch_bankall")

        assertEquals(300, RunePouch.storedAmount(player, FIRE))
        assertEquals(0, bank.count(FIRE))
    }

    @Test
    fun GameTestState.`a bank stack at max value only takes what fits`() = panelTest {
        player.inv[0] = InvObj(id(POUCH), 1)
        RunePouch.setSlot(player, 0, FIRE, 300)
        setBank(InvObj(id(FIRE), Int.MAX_VALUE - 100))
        configure(0)

        button("component.bankside:runepouch_bankall")

        assertEquals(Int.MAX_VALUE, bank.count(FIRE))
        assertEquals(200, RunePouch.storedAmount(player, FIRE))
    }

    @Test
    fun GameTestState.`bank all empties every slot into the bank`() = panelTest {
        player.inv[0] = InvObj(id(POUCH), 1)
        RunePouch.setSlot(player, 0, AIR, 10)
        RunePouch.setSlot(player, 1, LAW, 20)
        RunePouch.setSlot(player, 2, FIRE, 30)
        configure(0)
        val before = totals(AIR, LAW, FIRE)

        button("component.bankside:runepouch_bankall")
        button("component.bankside:runepouch_bankall")

        assertEquals(0, (0 until 3).count { RunePouch.slot(player, it) != null })
        assertEquals(before, totals(AIR, LAW, FIRE))
    }

    @Test
    fun GameTestState.`dragging pouch slots swaps them`() = panelTest {
        player.inv[0] = InvObj(id(POUCH), 1)
        RunePouch.setSlot(player, 0, AIR, 10)
        RunePouch.setSlot(player, 1, LAW, 20)
        configure(0)

        player.ifButtonD(component(BankRunePouchView.POUCH_SLOTS), 1, 2)
        advance()

        assertEquals(LAW to 20, RunePouch.slot(player, 0))
        assertEquals(AIR to 10, RunePouch.slot(player, 1))
    }

    @Test
    fun GameTestState.`loading a loadout banks the pouch and refills it from the bank`() =
        panelTest {
            player.inv[0] = InvObj(id(POUCH), 1)
            RunePouch.setSlot(player, 0, FIRE, 40)
            setBank(InvObj(id(AIR), 20_000), InvObj(id(LAW), 300))
            configure(0)
            setLoadoutSlot(loadout = 0, slot = 0, rune = AIR, amount = 0)
            setLoadoutSlot(loadout = 0, slot = 1, rune = LAW, amount = 100)
            assertEquals(RunePouchLoadouts.Entry(LAW, 100), RunePouchLoadouts.entry(player, 0, 1))
            val before = totals(AIR, LAW, FIRE)

            button("component.bankside:runepouch_load_a")
            button("component.bankside:runepouch_load_a")

            assertEquals(AIR to 16_000, RunePouch.slot(player, 0))
            assertEquals(LAW to 100, RunePouch.slot(player, 1))
            assertNull(RunePouch.slot(player, 2))
            assertEquals(40, bank.count(FIRE))
            assertEquals(200, bank.count(LAW))
            assertEquals(before, totals(AIR, LAW, FIRE))
        }

    @Test
    fun GameTestState.`loading with runes missing from the bank takes what is there`() =
        panelTest {
            player.inv[0] = InvObj(id(POUCH), 1)
            setBank(InvObj(id(LAW), 7))
            configure(0)
            RunePouchLoadouts.setEntry(player, 1, 0, LAW, 100)
            RunePouchLoadouts.setEntry(player, 1, 1, AIR, 0)
            val before = totals(AIR, LAW)

            button("component.bankside:runepouch_load_b")

            assertEquals(LAW to 7, RunePouch.slot(player, 0))
            assertNull(RunePouch.slot(player, 1))
            assertEquals(0, bank.count(LAW))
            assertEquals(before, totals(AIR, LAW))
        }

    @Test
    fun GameTestState.`a loadout that cannot bank the pouch changes nothing`() = panelTest {
        player.inv[0] = InvObj(id(POUCH), 1)
        RunePouch.setSlot(player, 0, FIRE, 40)
        setBank(InvObj(id(AIR), 500))
        player.bankCapacity = 1
        configure(0)
        RunePouchLoadouts.setEntry(player, 0, 0, AIR, 0)
        val before = totals(AIR, FIRE)

        button("component.bankside:runepouch_load_a")

        assertEquals(FIRE to 40, RunePouch.slot(player, 0))
        assertEquals(500, bank.count(AIR))
        assertEquals(before, totals(AIR, FIRE))
    }

    @Test
    fun GameTestState.`split loadout varbits round trip`() = panelTest {
        RunePouchLoadouts.setEntry(player, 0, 0, "obj.wrathrune", 0)
        RunePouchLoadouts.setEntry(player, 3, 2, LAW, 12_345)
        val wrath = RunePouchLoadouts.Entry("obj.wrathrune", 0)
        assertEquals(wrath, RunePouchLoadouts.entry(player, 0, 0))
        assertEquals(RunePouchLoadouts.Entry(LAW, 12_345), RunePouchLoadouts.entry(player, 3, 2))
        RunePouchLoadouts.setEntry(player, 3, 2, null, 0)
        assertNull(RunePouchLoadouts.entry(player, 3, 2))
    }

    @Test
    fun GameTestState.`picking a name renames the selected loadout`() = panelTest {
        player.inv[0] = InvObj(id(POUCH), 1)
        configure(0)
        button("component.bankside:runepouch_name_c")
        val count = RunePouchLoadouts.nameCount
        player.ifButton(component(BankRunePouchView.NAME_LIST), comsub = count + 2)
        advance()
        assertEquals(RunePouchLoadouts.nameValue(2), RunePouchLoadouts.name(player, 2))
    }

    @Test
    fun GameTestState.`depositing the pouch closes the panel`() = panelTest {
        player.inv[0] = InvObj(id(POUCH), 1)
        configure(0)
        player.ifButton(component("component.bankside:items"), comsub = 0, op = IfButtonOp.Op8)
        advance()
        assertFalse(player.runePouchViewOpen)
    }

    private fun GameTestState.panelTest(body: GameTestScope.() -> Unit) =
        runInjectedGameTest(
            WeaponSpeeds::class,
            WeaponModule(),
            BankInvScript::class,
            BankOpenScript::class,
            BankRunePouchScript::class,
            WeaponAttackStylesScript::class,
        ) {
            player.clearAllInvs()
            player.bankCapacity = 800
            player.openBank(eventBus)
            body()
        }

    private val GameTestScope.bank: Inventory
        get() = player.invMap.getOrPut("inv.bank")

    private fun GameTestScope.setBank(vararg objs: InvObj) {
        objs.forEachIndexed { i, obj -> bank[i] = obj }
        VarPlayerIntMapSetter.set(player, "varbit.bank_tab_main", objs.size)
    }

    private fun GameTestScope.configure(slot: Int) {
        player.ifButton(component("component.bankside:items"), comsub = slot, op = IfButtonOp.Op1)
        advance()
    }

    private fun GameTestScope.button(component: String) {
        player.ifButton(component(component))
        advance()
    }

    private fun GameTestScope.withdraw(bankSlot: Int, op: IfButtonOp) {
        player.ifButton(component("component.bankmain:items"), comsub = bankSlot, op = op)
        advance()
    }

    private fun GameTestScope.pouchSlot(slot: Int, op: IfButtonOp) {
        player.ifButton(component(BankRunePouchView.POUCH_SLOTS), comsub = slot + 1, op = op)
        advance()
    }

    private fun GameTestScope.setLoadoutSlot(loadout: Int, slot: Int, rune: String, amount: Int) {
        val letter = RunePouchLoadouts.letters[loadout]
        player.ifButton(component(BankRunePouchView.loadoutSlots(letter)), comsub = slot + 1)
        advance()
        player.resumeObjDialog(id(rune))
        advance()
        player.resumeCountDialog(amount)
        advance()
    }

    private fun GameTestScope.totals(vararg runes: String): List<Int> =
        runes.map { player.inv.count(it) + bank.count(it) + RunePouch.storedAmount(player, it) }

    private fun Inventory.count(obj: String): Int =
        filterNotNull().filter { it.id == id(obj) }.sumOf { it.count }

    private fun component(name: String) =
        ServerCacheManager.fromComponent(name.asRSCM(RSCMType.COMPONENT))

    private fun id(obj: String): Int = obj.asRSCM(RSCMType.OBJ)

    private class WeaponModule : AbstractModule() {
        override fun configure() {
            bind(AttackStyles::class.java).`in`(Scopes.SINGLETON)
            bind(WeaponSpeeds::class.java).`in`(Scopes.SINGLETON)
        }
    }

    private companion object {
        const val POUCH = "obj.bh_rune_pouch"
        const val DIVINE = "obj.divine_rune_pouch"
        const val AIR = "obj.airrune"
        const val LAW = "obj.lawrune"
        const val FIRE = "obj.firerune"
        val WITHDRAW_ALL = IfButtonOp.Op7
    }
}
