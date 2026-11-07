package org.rsmod.content.travel.teleports

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc3
import org.rsmod.content.travel.teleports.RouteTravel.travelFromHub
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Magic carpets across the Kharidian Desert: the `carpet_*` networks of `teleport_destination`,
 * each hub first. The stop comes from the player's position, since the Sophanem and Menaphos
 * merchants turn into the Nardah merchant's form after Icthlarin's Little Helper.
 */
class MagicCarpetScript : PluginScript() {
    private val networks: List<List<TeleportDestination>> by lazy {
        NETWORKS.map(TeleportNetworks::of)
    }

    override fun ScriptContext.startup() {
        for (merchant in MERCHANTS) {
            onOpNpc3(merchant) { travel() }
        }
    }

    private suspend fun ProtectedAccess.travel() =
        travelFromHub(networks, MENU_TITLE) {
            "You climb onto the magic carpet and fly to $it."
        }

    private companion object {
        private val NETWORKS = listOf("carpet_shantay", "carpet_pollnivneach")
        private const val MENU_TITLE = "Where would you like to travel to?"
        private val MERCHANTS = (1..8).map { "npc.magic_carpet_seller$it" }
    }
}
