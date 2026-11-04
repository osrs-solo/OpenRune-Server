package org.rsmod.content.skills.prayer.items.bonesack

import dev.openrune.ServerCacheManager
import dev.openrune.rscm.RSCM.asRSCM
import dev.openrune.rscm.RSCMType
import org.rsmod.api.player.events.interact.HeldBanksideEvents
import org.rsmod.api.player.output.mes
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onEvent
import org.rsmod.api.script.onOpHeld1
import org.rsmod.api.script.onOpHeld3
import org.rsmod.api.script.onOpHeld4
import org.rsmod.api.script.onOpHeldU
import org.rsmod.game.entity.Player
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

class BonesackScript : PluginScript() {
    override fun ScriptContext.startup() {
        onOpHeld1(Bonesack.OBJ) { fill() }
        onOpHeld3(Bonesack.OBJ) { check() }
        onOpHeld4(Bonesack.OBJ) { empty() }
        for (bone in Bonesack.bones) {
            onOpHeldU(bone, Bonesack.OBJ) { fillWith(bone) }
        }
        onEvent<HeldBanksideEvents.Type>(Bonesack.OBJ.asRSCM(RSCMType.OBJ)) { emptyIntoBank(player) }
    }

    private fun ProtectedAccess.fill() {
        if (Bonesack.total(player) >= Bonesack.CAPACITY) {
            mes(FULL)
            return
        }
        val moved = Bonesack.fill(player, inv)
        mes(if (moved > 0) "You put $moved bones into the bonesack." else NO_BONES)
    }

    private fun ProtectedAccess.fillWith(bone: String) {
        if (Bonesack.total(player) >= Bonesack.CAPACITY) {
            mes(FULL)
            return
        }
        val moved = Bonesack.fill(player, inv, listOf(bone))
        if (moved > 0) mes("You put $moved ${name(bone).lowercase()} into the bonesack.")
    }

    private fun ProtectedAccess.check() {
        val contents = Bonesack.contents(player)
        if (contents.isEmpty()) {
            mes(EMPTY)
            return
        }
        mes("The bonesack holds ${Bonesack.total(player)}/${Bonesack.CAPACITY} bones:")
        for ((bone, count) in contents) {
            mes("$count x ${name(bone)}")
        }
    }

    private fun ProtectedAccess.empty() {
        if (Bonesack.contents(player).isEmpty()) {
            mes(EMPTY)
            return
        }
        Bonesack.empty(player, inv)
        mes(
            if (Bonesack.contents(player).isEmpty()) "You empty the bonesack."
            else "You don't have enough inventory space to empty the whole bonesack."
        )
    }

    private fun emptyIntoBank(player: Player) {
        if (Bonesack.contents(player).isEmpty()) {
            player.mes(EMPTY)
            return
        }
        Bonesack.empty(player, player.invMap.getOrPut("inv.bank"))
        player.mes("You empty the bonesack into your bank.")
    }

    private fun name(obj: String): String =
        ServerCacheManager.getItem(obj.asRSCM(RSCMType.OBJ))?.name ?: obj

    private companion object {
        const val FULL = "The bonesack is full."
        const val EMPTY = "The bonesack is empty."
        const val NO_BONES = "You have no bones to put in the bonesack."
    }
}
