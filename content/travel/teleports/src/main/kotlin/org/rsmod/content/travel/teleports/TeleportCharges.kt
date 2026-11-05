package org.rsmod.content.travel.teleports

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.add
import org.rsmod.api.invtx.delete
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.game.inv.Inventory

/**
 * Charge handling for teleport items. Charge chains swap the obj itself; varbit items keep the
 * count on the player, so every talisman or book a player owns draws on the same pool, as in OSRS.
 */
object TeleportCharges {
    fun ProtectedAccess.hasCharge(item: TeleportItem): Boolean {
        val varbit = item.chargeVarbit ?: return true
        if (vars[varbit] > 0) {
            return true
        }
        mes("Your ${item.label} has no charges left.")
        return false
    }

    /**
     * Spends one charge of the item in [slot]: steps a charge chain down one obj (or crumbles it),
     * or takes one from the item's charge varbit. Returns `false`, spending nothing, when the item
     * is no longer in [slot] or its charges ran out meanwhile.
     */
    fun ProtectedAccess.spendCharge(item: TeleportItem, inv: Inventory, slot: Int): Boolean {
        val obj = inv[slot] ?: return false
        val name = RSCM.getReverseMapping(RSCMType.OBJ, obj.id)
        if (name !in item.objs) {
            return false
        }
        val varbit = item.chargeVarbit
        if (varbit != null) {
            val left = vars[varbit] - 1
            if (left < 0) {
                return false
            }
            vars[varbit] = left
            mes(chargesMessage(item, left))
            return true
        }
        if (!item.isChain) {
            return true
        }
        val next = item.afterCharge(name)
        val replacement = next?.let { ServerCacheManager.getItem(it.asRSCM(RSCMType.OBJ)) }
        val success =
            when {
                next == null -> invDel(inv, name, count = 1, slot = slot).success
                replacement == null -> false
                else -> invReplaceSlot(inv, slot, count = 1, replacement = replacement).success
            }
        if (!success) {
            return false
        }
        if (inv === worn) {
            rebuildAppearance()
        }
        if (next == null) {
            mes("Your ${item.label} crumbles to dust.")
        } else {
            mes(chargesMessage(item, item.chargesAfter(name) ?: 0))
        }
        return true
    }

    fun ProtectedAccess.checkCharges(item: TeleportItem) {
        val varbit = item.chargeVarbit ?: return
        mes(chargesMessage(item, vars[varbit]))
    }

    /**
     * Uses every charge obj in the inventory that fits on the item in [slot], turning an inert item
     * into its charged form. The objs, the item and the varbit change together or not at all.
     */
    fun ProtectedAccess.charge(item: TeleportItem, slot: Int) {
        val varbit = item.chargeVarbit ?: return
        val chargeObj = item.chargeObj ?: return
        val target = inv[slot] ?: return
        val targetName = RSCM.getReverseMapping(RSCMType.OBJ, target.id)
        val isInert = targetName == item.inert
        if (!isInert && targetName !in item.objs) {
            return
        }
        val current = vars[varbit]
        val add = item.chargesToAdd(current, inv.physicalCount(chargeObj))
        if (add <= 0) {
            mes("Your ${item.label} cannot hold any more charges.")
            return
        }
        val charged = item.objs.first()
        val result =
            player.invTransaction(inv) {
                val into = select(inv)
                delete(into, chargeObj.asRSCM(RSCMType.OBJ), add)
                if (isInert) {
                    delete(into, target.id, count = 1, slot = slot)
                    add(into, charged.asRSCM(RSCMType.OBJ), count = 1, slot = slot)
                }
            }
        if (!result.success) {
            return
        }
        vars[varbit] = current + add
        mes(chargesMessage(item, current + add))
    }

    /**
     * Turns the charged item in [slot] back into its inert form and returns one charge obj per
     * charge. Nothing changes when the returned objs do not fit.
     */
    fun ProtectedAccess.uncharge(item: TeleportItem, inv: Inventory, slot: Int) {
        val varbit = item.chargeVarbit ?: return
        val chargeObj = item.chargeObj ?: return
        val inert = item.inert ?: return
        val target = inv[slot] ?: return
        if (RSCM.getReverseMapping(RSCMType.OBJ, target.id) !in item.objs) {
            return
        }
        val charges = vars[varbit]
        val result =
            player.invTransaction(inv) {
                val into = select(inv)
                delete(into, target.id, count = 1, slot = slot)
                add(into, inert.asRSCM(RSCMType.OBJ), count = 1, slot = slot)
                if (charges > 0) {
                    add(into, chargeObj.asRSCM(RSCMType.OBJ), charges)
                }
            }
        if (!result.success) {
            mes("You don't have enough inventory space to uncharge your ${item.label}.")
            return
        }
        vars[varbit] = 0
        mes("You uncharge your ${item.label}.")
    }

    private fun chargesMessage(item: TeleportItem, left: Int): String =
        when (left) {
            0 -> "Your ${item.label} has run out of charges."
            1 -> "Your ${item.label} has one charge left."
            else -> "Your ${item.label} has $left charges left."
        }
}
