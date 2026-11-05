package org.rsmod.content.interfaces.bank

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.content.skills.magic.runepouch.RunePouch
import org.rsmod.content.skills.magic.runepouch.RunePouchLoadouts
import org.rsmod.game.entity.Player

/**
 * The bankside swaps its inventory for the rune pouch panel while `varbit.bank_viewcontainer` is 3
 * (rune pouch) or 4 (divine rune pouch); the client flips it itself on "Configure" and on the
 * panel's dismiss button, and the server mirrors it.
 */
object BankRunePouchView {
    const val VIEW_VARBIT = "varbit.bank_viewcontainer"
    const val VIEW_OBJ_VARP = "varp.bank_viewcontainer_obj"
    const val HIDDEN = 0
    const val REGULAR = 3
    const val DIVINE = 4

    const val POUCH_SLOTS = "component.bankside:runepouch_top"
    const val NAME_LIST = "component.bankside:runepouch_name_select_list"

    fun loadoutSlots(letter: String): String = "component.bankside:runepouch_loadout_$letter"

    fun loadButton(letter: String): String = "component.bankside:runepouch_load_$letter"

    fun nameButton(letter: String): String = "component.bankside:runepouch_name_$letter"

    val POUCH_SLOT_EVENTS =
        arrayOf(
            IfEvent.Op1,
            IfEvent.Op2,
            IfEvent.Op3,
            IfEvent.Op4,
            IfEvent.Op5,
            IfEvent.Op6,
            IfEvent.Op7,
            IfEvent.Op10,
            IfEvent.Depth1,
            IfEvent.DragTarget,
        )
}

val Player.runePouchViewOpen: Boolean
    get() =
        vars[BankRunePouchView.VIEW_VARBIT].let {
            it == BankRunePouchView.REGULAR || it == BankRunePouchView.DIVINE
        }

fun Player.openRunePouchView(pouch: String) {
    val divine = pouch in RunePouch.divinePouches
    val view = if (divine) BankRunePouchView.DIVINE else BankRunePouchView.REGULAR
    VarPlayerIntMapSetter.set(this, BankRunePouchView.VIEW_VARBIT, view)
    VarPlayerIntMapSetter.set(this, BankRunePouchView.VIEW_OBJ_VARP, pouch.asRSCM(RSCMType.OBJ))
    ifSetEvents(BankRunePouchView.POUCH_SLOTS, 1..4, *BankRunePouchView.POUCH_SLOT_EVENTS)
    for (letter in RunePouchLoadouts.letters) {
        ifSetEvents(BankRunePouchView.loadoutSlots(letter), 1..4, IfEvent.Op1)
    }
    val names = RunePouchLoadouts.nameCount
    ifSetEvents(BankRunePouchView.NAME_LIST, names until names * 2, IfEvent.Op1)
}

fun Player.closeRunePouchView() {
    VarPlayerIntMapSetter.set(this, BankRunePouchView.VIEW_VARBIT, BankRunePouchView.HIDDEN)
    VarPlayerIntMapSetter.set(this, BankRunePouchView.VIEW_OBJ_VARP, -1)
}

/** Closes the panel once the pouch it shows has left the inventory. */
fun Player.syncRunePouchView() {
    if (runePouchViewOpen && RunePouch.slotCount(this) == 0) {
        closeRunePouchView()
    }
}
