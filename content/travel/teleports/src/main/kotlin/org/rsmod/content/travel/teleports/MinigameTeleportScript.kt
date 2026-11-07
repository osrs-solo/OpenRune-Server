package org.rsmod.content.travel.teleports

import dev.openrune.definition.type.widget.IfEvent
import dev.openrune.types.aconverted.interf.IfSubType
import jakarta.inject.Inject
import org.rsmod.api.area.checker.AreaChecker
import org.rsmod.api.player.hook.PlayerTeleportValidator
import org.rsmod.api.player.hook.TeleportType
import org.rsmod.api.player.output.ChatType
import org.rsmod.api.player.protect.ProtectedAccess
import org.rsmod.api.player.ui.ifSetEvents
import org.rsmod.api.script.onIfOverlayButton
import org.rsmod.api.script.onPlayerLogin
import org.rsmod.api.table.MinigameTeleportRow
import org.rsmod.plugin.scripts.PluginScript
import org.rsmod.plugin.scripts.ScriptContext

/**
 * The channels side tab and the Grouping minigame teleport.
 *
 * Each channels tab opens its panel into `side_channels:contents`. In Grouping, picking a minigame
 * is client-side (`grouping_viewchannel` stores it in a varc), and the client then rebuilds the
 * Teleport button as `grouping:teleport` with the minigame's id as its subcomponent, so the click
 * carries the id of the `minigame_teleport` row. Landing tiles are the `minigame` rows of
 * `teleport_destination`, matched by name.
 */
class MinigameTeleportScript
@Inject
constructor(
    private val teleportValidator: PlayerTeleportValidator,
    private val areaChecker: AreaChecker,
) : PluginScript() {
    private val landings: Map<Int, TeleportDestination> by lazy {
        val byName = TeleportNetworks.of(NETWORK).associateBy { it.name }
        MinigameTeleportRow.all()
            .mapNotNull { row -> byName[row.name]?.let { row.id to it } }
            .toMap()
    }

    override fun ScriptContext.startup() {
        onPlayerLogin {
            for (tab in TAB_PANELS.indices) {
                player.ifSetEvents("component.side_channels:tab_$tab", -1..-1, IfEvent.Op1)
            }
        }
        for ((tab, panel) in TAB_PANELS.withIndex()) {
            onIfOverlayButton("component.side_channels:tab_$tab") { openTab(tab, panel) }
        }
        onIfOverlayButton(TELEPORT_BUTTON) { teleport(it.comsub) }
    }

    private fun ProtectedAccess.openTab(tab: Int, panel: String) {
        vars[SELECTED_TAB] = tab
        ifOpenSub(panel, CONTENTS, IfSubType.Overlay)
        if (panel == GROUPING) {
            ifSetEvents(TELEPORT_BUTTON, 0..MAX_GAME_ID, IfEvent.Op1)
            ifSetEvents("component.grouping:dropdown_contents", 0..MAX_GAME_ID, IfEvent.Op1)
        }
    }

    private suspend fun ProtectedAccess.teleport(gameId: Int) {
        val landing = landings[gameId]
        if (landing == null) {
            mes("Select a minigame from the list first.")
            return
        }
        if (actionDelay > mapClock) {
            return
        }
        val denial = teleportValidator.validate(player, TeleportType.Minigame, areaChecker)
        if (denial != null) {
            mes(denial, ChatType.Engine)
            return
        }
        actionDelay = mapClock + CAST_TICKS + 1
        anim(CAST_ANIM)
        spotanim(CAST_SPOTANIM, height = CAST_SPOTANIM_HEIGHT)
        delay(CAST_TICKS)
        telejump(landing.coords, TeleportType.Exempt)
        anim(LAND_ANIM)
        mes("You teleport to ${landing.name}.")
    }

    private companion object {
        private const val NETWORK = "minigame"
        private const val SELECTED_TAB = "varbit.side_channels_tab_selected"
        private const val CONTENTS = "component.side_channels:contents"
        private const val GROUPING = "interface.grouping"
        private const val TELEPORT_BUTTON = "component.grouping:teleport"
        private const val MAX_GAME_ID = 40

        /** Chat-channel, Your Clan, View another clan, Grouping: `enum_3839` order. */
        private val TAB_PANELS =
            listOf(
                "interface.chatchannel_current",
                "interface.clans_sidepanel",
                "interface.clans_guest_sidepanel",
                GROUPING,
            )

        private const val CAST_ANIM = "seq.human_castteleport"
        private const val LAND_ANIM = "seq.human_castteleport_reverse"
        private const val CAST_SPOTANIM = "spotanim.teleport_casting"
        private const val CAST_SPOTANIM_HEIGHT = 92
        private const val CAST_TICKS = 3
    }
}
