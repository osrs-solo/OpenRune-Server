package org.rsmod.content.travel.teleports

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.vars.intVarBit
import org.rsmod.api.script.onOpLoc1
import org.rsmod.api.script.onOpLoc2
import org.rsmod.api.script.onOpLoc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The spirit tree network. Its stops are the `spirit_tree` rows of `teleport_destination`.
 *
 * Each tree is a multiloc whose visible form follows a quest: before it, the tree only offers
 * `Talk-to`. Every form is handled, and `Talk-to` opens the same travel list, so the network is
 * open whatever the player's quest progress (see docs/quirks.md).
 */
class SpiritTreeScript : PluginScript() {
    private var ProtectedAccess.previous by intVarBit("varbit.spirit_tree_previous")

    private val trees: List<TeleportDestination> by lazy { TeleportNetworks.of(NETWORK) }

    override fun ScriptContext.startup() {
        for (loc in TREE_LOCS) {
            onOpLoc1(loc) { chooseDestination() }
            onOpLoc2(loc) { chooseDestination() }
            onOpLoc3(loc) { lastDestination() }
        }
    }

    private suspend fun ProtectedAccess.chooseDestination() {
        val names = trees.map { it.name }
        val picked = menu(MENU_TITLE, hotkeys = true, choices = names + CANCEL)
        val tree = trees.getOrNull(picked) ?: return
        travel(tree)
    }

    private suspend fun ProtectedAccess.lastDestination() {
        val tree = trees.getOrNull(previous - 1)
        if (tree == null) {
            mes("You have not travelled by spirit tree yet.")
            return
        }
        travel(tree)
    }

    private suspend fun ProtectedAccess.travel(tree: TeleportDestination) {
        if (actionDelay > mapClock) {
            return
        }
        if (coords.chebyshevDistance(tree.coords) <= NEARBY) {
            mes("You are already at that spirit tree.")
            return
        }
        ifClose()
        actionDelay = mapClock + ACTION_DELAY
        mes(
            "You place your hands on the dry tough bark of the spirit tree, and feel a surge of " +
                "energy run through your veins."
        )
        delay(TRAVEL_DELAY)
        telejump(tree.coords)
        previous = trees.indexOf(tree) + 1
    }

    private companion object {
        private const val NETWORK = "spirit_tree"
        private const val MENU_TITLE = "Spirit Tree Locations"
        private const val CANCEL = "Cancel"
        private const val NEARBY = 8
        private const val TRAVEL_DELAY = 2
        private const val ACTION_DELAY = 3

        private val TREE_LOCS =
            listOf(
                "loc.spirittree_small_1op",
                "loc.spirittree_small_2ops",
                "loc.spirittree_big_1op",
                "loc.spirittree_big_2ops",
                "loc.spirittree_big_2ops_orbs",
                "loc.spirittree_prif_1op",
                "loc.spirittree_prif_2ops",
                "loc.pog_spirit_tree_alive_static",
            )
    }
}
