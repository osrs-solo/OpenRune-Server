package org.rsmod.content.travel.teleports

import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.script.onOpNpc3
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * Magic carpets across the Kharidian Desert. Each `carpet_*` network in `teleport_destination`
 * lists its hub first and the stops it serves after: from the hub a rug merchant flies to any of
 * them, from any other stop only back to the hub.
 *
 * Which stop the player is at comes from where they stand rather than which merchant they clicked,
 * since the Sophanem and Menaphos merchants turn into the Nardah merchant's form after Icthlarin's
 * Little Helper.
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

    private suspend fun ProtectedAccess.travel() {
        val (stops, here) = currentStop() ?: return mes("There are no carpets to ride from here.")
        val choices = if (here == HUB) stops.indices.filter { it != HUB } else listOf(HUB)
        val labels = choices.map { stops[it].name } + CANCEL
        val picked = menu(MENU_TITLE, hotkeys = true, choices = labels)
        val stop = choices.getOrNull(picked)?.let(stops::get) ?: return
        ride(stop)
    }

    private fun ProtectedAccess.currentStop(): Pair<List<TeleportDestination>, Int>? {
        for (stops in networks) {
            val index = stops.indexOfFirst { it.coords.chebyshevDistance(coords) <= NEARBY }
            if (index != -1) {
                return stops to index
            }
        }
        return null
    }

    private suspend fun ProtectedAccess.ride(stop: TeleportDestination) {
        ifClose()
        mes("You climb onto the magic carpet and fly to ${stop.name}.")
        fadeOverlay(0, 255, 0, 0, FADE_CYCLES)
        delay(RIDE_TICKS)
        telejump(stop.coords)
        fadeOverlay(0, 0, 0, 255, FADE_CYCLES)
        closeFadeOverlay(cycles = 2)
    }

    private companion object {
        private val NETWORKS = listOf("carpet_shantay", "carpet_pollnivneach")
        private const val HUB = 0
        private const val NEARBY = 10
        private const val RIDE_TICKS = 4
        private const val FADE_CYCLES = 50
        private const val MENU_TITLE = "Where would you like to travel to?"
        private const val CANCEL = "Cancel"

        private val MERCHANTS = (1..8).map { "npc.magic_carpet_seller$it" }
    }
}
