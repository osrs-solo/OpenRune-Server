package org.rsmod.content.skills.magic.runepouch

import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.invtx.invTransaction
import org.rsmod.api.invtx.select
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeldU
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class DivineRunePouchScript : PluginScript() {
    override fun ScriptContext.startup() {
        for ((plain, divine) in UPGRADES) {
            onOpHeldU(THREAD, plain) { upgrade(plain, divine) }
        }
        onOpHeld3(DIVINE) { revert() }
    }

    private suspend fun ProtectedAccess.upgrade(plain: String, divine: String) {
        if (statBase(CRAFTING) < CRAFTING_LEVEL) {
            mesbox("You need a Crafting level of $CRAFTING_LEVEL to weave the thread into the pouch.")
            return
        }
        if (weaveThread(plain, divine)) {
            objbox(divine, "You weave the thread into your rune pouch. It can now hold four runes.")
        }
    }

    private suspend fun ProtectedAccess.revert() {
        if (RunePouch.slot(player, DIVINE_ONLY_SLOT) != null) {
            mes("You need to empty the fourth slot of your rune pouch before reverting it.")
            return
        }
        val confirmed =
            choice2(
                "Revert it. The thread of Elidinis will be lost.",
                true,
                "Keep it as it is.",
                false,
                title = "Revert your divine rune pouch?",
            )
        if (confirmed && unweave()) {
            objbox(PLAIN, "Your divine rune pouch reverts to a regular rune pouch.")
        }
    }

    /** Swaps [plain] for [divine] and consumes the thread in one transaction, or changes nothing. */
    fun ProtectedAccess.weaveThread(plain: String, divine: String): Boolean {
        val result =
            player.invTransaction(inv) {
                val from = select(inv)
                delete {
                    this.from = from
                    this.obj = THREAD.asRSCM(RSCMType.OBJ)
                    this.strictCount = 1
                }
                delete {
                    this.from = from
                    this.obj = plain.asRSCM(RSCMType.OBJ)
                    this.strictCount = 1
                }
                insert {
                    this.into = from
                    this.obj = divine.asRSCM(RSCMType.OBJ)
                    this.strictCount = 1
                }
            }
        return result.success
    }

    fun ProtectedAccess.unweave(): Boolean {
        if (RunePouch.slot(player, DIVINE_ONLY_SLOT) != null) return false
        return invReplace(inv, DIVINE, 1, PLAIN).success
    }

    private companion object {
        const val THREAD = "obj.thread_of_elidinis"
        const val PLAIN = "obj.bh_rune_pouch"
        const val DIVINE = "obj.divine_rune_pouch"
        const val CRAFTING = "stat.crafting"
        const val CRAFTING_LEVEL = 75
        const val DIVINE_ONLY_SLOT = 3

        val UPGRADES =
            listOf(PLAIN to DIVINE, "obj.bh_rune_pouch_trouver" to "obj.divine_rune_pouch_trouver")
    }
}
