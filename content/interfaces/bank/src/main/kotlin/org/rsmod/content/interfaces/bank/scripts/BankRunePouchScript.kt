package org.rsmod.content.interfaces.bank.scripts

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.aconverted.interf.IfButtonOp
import jakarta.inject.Inject
import kotlin.math.max
import org.rsmod.api.player.output.objExamine
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.IfModalDrag
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.api.script.onIfModalButton
import org.rsmod.api.script.onIfModalDrag
import org.rsmod.content.interfaces.bank.BankRunePouchView
import org.rsmod.content.interfaces.bank.QuantityMode
import org.rsmod.content.interfaces.bank.alwaysPlacehold
import org.rsmod.content.interfaces.bank.closeRunePouchView
import org.rsmod.content.interfaces.bank.lastQtyInput
import org.rsmod.content.interfaces.bank.leftClickQtyMode
import org.rsmod.content.interfaces.bank.runePouchViewOpen
import org.rsmod.content.skills.magic.runepouch.RunePouch
import org.rsmod.content.skills.magic.runepouch.RunePouchLoadouts
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/** The rune pouch panel the bankside shows after "Configure" on a rune pouch. */
class BankRunePouchScript @Inject constructor(private val bankInv: BankInvScript) :
    PluginScript() {
    override fun ScriptContext.startup() {
        onIfModalButton("component.bankside:runepouch_dismiss") { player.closeRunePouchView() }
        onIfModalButton("component.bankside:runepouch_bankall") { if (panelOpen()) bankAll() }
        onIfModalButton(BankRunePouchView.POUCH_SLOTS) { pouchSlotOp(it.comsub - 1, it.op) }
        onIfModalDrag(BankRunePouchView.POUCH_SLOTS) { dragPouchSlot(it) }
        onIfModalButton(BankRunePouchView.NAME_LIST) { selectName(it.comsub) }
        RunePouchLoadouts.letters.forEachIndexed { loadout, letter ->
            onIfModalButton(BankRunePouchView.loadoutSlots(letter)) {
                if (it.op == IfButtonOp.Op1) changeLoadoutSlot(loadout, it.comsub - 1)
            }
            onIfModalButton(BankRunePouchView.loadButton(letter)) {
                if (panelOpen()) loadLoadout(loadout)
            }
            onIfModalButton(BankRunePouchView.nameButton(letter)) {
                VarPlayerIntMapSetter.set(player, SELECTED_NAME, loadout + 1)
            }
        }
    }

    private fun ProtectedAccess.panelOpen(): Boolean {
        if (!player.runePouchViewOpen) return false
        if (RunePouch.slotCount(player) == 0) {
            player.closeRunePouchView()
            return false
        }
        return true
    }

    private suspend fun ProtectedAccess.pouchSlotOp(slot: Int, op: IfButtonOp) {
        if (!panelOpen() || slot !in 0 until RunePouch.slotCount(player)) return
        val (rune, stored) = RunePouch.slot(player, slot) ?: return
        if (op == IfButtonOp.Op10) {
            val type = ServerCacheManager.getItem(rune.asRSCM(RSCMType.OBJ)) ?: return
            player.objExamine(type, stored, 0)
            return
        }
        val count =
            when (op) {
                IfButtonOp.Op1 -> leftClickQty()
                IfButtonOp.Op2 -> 1
                IfButtonOp.Op3 -> 5
                IfButtonOp.Op4 -> 10
                IfButtonOp.Op5 -> max(1, lastQtyInput)
                IfButtonOp.Op6 -> countDialog().also { lastQtyInput = it }
                IfButtonOp.Op7 -> Int.MAX_VALUE
                else -> return
            }
        depositPouchSlot(slot, count)
    }

    private fun ProtectedAccess.leftClickQty(): Int =
        when (leftClickQtyMode) {
            QuantityMode.One -> 1
            QuantityMode.Five -> 5
            QuantityMode.Ten -> 10
            QuantityMode.X -> max(1, lastQtyInput)
            QuantityMode.All -> Int.MAX_VALUE
        }

    /** Banks up to [requested] runes from pouch [slot]; returns how many left the pouch. */
    fun ProtectedAccess.depositPouchSlot(slot: Int, requested: Int): Int {
        val (rune, stored) = RunePouch.slot(player, slot) ?: return 0
        val amount = minOf(requested, stored)
        val banked = with(bankInv) { depositIntoBank(rune.asRSCM(RSCMType.OBJ), amount) }
        if (banked <= 0) return 0
        return RunePouch.remove(player, slot, banked)
    }

    fun ProtectedAccess.bankAll() {
        val slots = RunePouch.slotCount(player)
        if ((0 until slots).none { RunePouch.slot(player, it) != null }) {
            mes(EMPTY)
            return
        }
        for (slot in 0 until slots) {
            val stored = RunePouch.slot(player, slot)?.second ?: continue
            if (depositPouchSlot(slot, stored) < stored) return
        }
    }

    private fun ProtectedAccess.dragPouchSlot(drag: IfModalDrag) {
        val from = (drag.selectedSlot ?: return) - 1
        val into = (drag.targetSlot ?: return) - 1
        val slots = RunePouch.slotCount(player)
        if (!panelOpen() || from !in 0 until slots || into !in 0 until slots) return
        RunePouch.swap(player, from, into)
    }

    private suspend fun ProtectedAccess.changeLoadoutSlot(loadout: Int, slot: Int) {
        if (!panelOpen() || slot !in 0 until RunePouch.slotCount(player)) return
        val picked = RSCM.getReverseMapping(RSCMType.OBJ, awaitObjSelection().id)
        if (!RunePouch.isRune(picked)) {
            RunePouchLoadouts.setEntry(player, loadout, slot, null, 0)
            return
        }
        val slots = RunePouch.slotCount(player)
        val others = RunePouchLoadouts.entries(player, loadout, slots).filterIndexed { i, _ ->
            i != slot
        }
        if (others.any { it?.rune == picked }) {
            mes(ALREADY_IN_LOADOUT)
            return
        }
        RunePouchLoadouts.setEntry(player, loadout, slot, picked, 0)
        val amount = countDialog(AMOUNT_PROMPT)
        RunePouchLoadouts.setEntry(player, loadout, slot, picked, amount)
    }

    /**
     * Banks the pouch, then fills each slot with its loadout rune from the bank (the cap, or as
     * many as fit). Runes only ever move between the pouch and the bank, one exact count at a time,
     * so a full bank or a missing rune stops short instead of losing anything.
     */
    fun ProtectedAccess.loadLoadout(loadout: Int) {
        val slots = RunePouch.slotCount(player)
        val entries = RunePouchLoadouts.entries(player, loadout, slots)
        if (entries.all { it == null }) {
            mes(LOADOUT_EMPTY)
            return
        }
        for (slot in 0 until slots) {
            val stored = RunePouch.slot(player, slot)?.second ?: continue
            if (depositPouchSlot(slot, stored) < stored) return
        }
        var short = false
        for ((slot, entry) in entries.withIndex()) {
            if (entry == null || RunePouch.storedAmount(player, entry.rune) > 0) continue
            val wanted = if (entry.cap > 0) entry.cap else RunePouch.CAPACITY
            val objId = entry.rune.asRSCM(RSCMType.OBJ)
            val bankSlot = bank.indexOfFirst { it?.id == objId }
            val available = if (bankSlot == -1) 0 else bank[bankSlot]?.count ?: 0
            val amount = minOf(wanted, available)
            if (amount < wanted) short = true
            if (amount <= 0) continue
            with(bankInv) {
                removeFromBank(bankSlot, objId, amount, alwaysPlacehold) {
                    RunePouch.setSlot(player, slot, entry.rune, amount)
                }
            }
        }
        if (short) {
            mes(NOT_ENOUGH)
        }
    }

    private fun ProtectedAccess.selectName(comsub: Int) {
        val count = RunePouchLoadouts.nameCount
        val value = RunePouchLoadouts.nameValue(comsub - count) ?: return
        val loadout = vars[SELECTED_NAME] - 1
        if (loadout !in 0 until RunePouchLoadouts.COUNT) return
        RunePouchLoadouts.setName(player, loadout, value)
    }

    private companion object {
        const val SELECTED_NAME = "varbit.rune_pouch_loadout_name_selected"

        const val EMPTY = "Your rune pouch is empty."
        const val ALREADY_IN_LOADOUT = "That loadout already holds that rune."
        const val LOADOUT_EMPTY = "That loadout is empty."
        const val NOT_ENOUGH = "You don't have enough runes in your bank to fill that loadout."
        const val AMOUNT_PROMPT = "Enter amount (0 to fill the slot):"
    }
}
