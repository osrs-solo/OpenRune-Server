package org.rsmod.content.travel.teleports.pack

import dev.openrune.definition.dbtables.dbTable
import dev.openrune.definition.util.VarType
import dev.openrune.pack.columnCoord
import org.rsmod.map.CoordGrid

/**
 * Where teleports go, and the items that take the player there.
 *
 * `teleport_destination` is shared by every travel network: each row is one stop on a named
 * network (`glory`, later `spirit_tree`, `glider`, ...), ordered the way the network lists them.
 * For items the order is the order of the item's sub-menu and worn ops, so a destination's
 * ordinal is the index the client sends back when that entry is clicked.
 *
 * `teleport_item` is one row per family of items sharing a network and a way of spending charges:
 * - a charge chain (`objs` from most to fewest charges) that steps down one obj per teleport, into
 *   `uncharged` or to dust when there is none;
 * - `unlimited` items that never change;
 * - items whose charges live in `charge_varbit`, refilled by using `charge_obj` on them.
 *
 * Coordinates come from the OSRS wiki teleport tables; destinations that need a quest or unlock in
 * OSRS are open to everyone here (see docs/quirks.md).
 */
object TeleportTables {
    const val DEST_NETWORK = 0
    const val DEST_ORDINAL = 1
    const val DEST_NAME = 2
    const val DEST_COORDS = 3
    const val DEST_WILDERNESS = 4

    const val ITEM_OBJS = 0
    const val ITEM_NETWORK = 1
    const val ITEM_LABEL = 2
    const val ITEM_TELEPORT_TYPE = 3
    const val ITEM_UNCHARGED = 4
    const val ITEM_UNLIMITED = 5
    const val ITEM_CHARGE_VARBIT = 6
    const val ITEM_MAX_CHARGES = 7
    const val ITEM_CHARGE_OBJ = 8
    const val ITEM_INERT = 9
    const val ITEM_DEPART_MESSAGE = 10
    const val ITEM_ARRIVE_MESSAGE = 11

    class Destination(
        val name: String,
        val x: Int,
        val z: Int,
        val level: Int = 0,
        val wilderness: Boolean = false,
    )

    class Item(
        val key: String,
        val network: String,
        val label: String,
        val objs: List<String>,
        val teleportType: String = STANDARD,
        val uncharged: String? = null,
        val unlimited: Boolean = false,
        val chargeVarbit: String? = null,
        val maxCharges: Int? = null,
        val chargeObj: String? = null,
        val inert: String? = null,
        val departMessage: String? = null,
        val arriveMessage: String? = null,
    )

    private const val STANDARD = "Standard"
    private const val LEVEL_30 = "MemberLevel30"

    private fun chain(base: String, from: Int, to: Int = 1): List<String> =
        (from downTo to).map { "obj.$base$it" }

    val NETWORKS: Map<String, List<Destination>> =
        linkedMapOf(
            "glory" to
                listOf(
                    Destination("Edgeville", 3087, 3496),
                    Destination("Karamja", 2918, 3176),
                    Destination("Draynor Village", 3105, 3251),
                    Destination("Al Kharid", 3293, 3163),
                ),
            "dueling" to
                listOf(
                    Destination("Emir's Arena", 3315, 3235),
                    Destination("Castle Wars", 2440, 3090),
                    Destination("Ferox Enclave", 3151, 3635),
                    Destination("Fortis Colosseum", 1793, 3107),
                ),
            "games" to
                listOf(
                    Destination("Burthorpe", 2899, 3553),
                    Destination("Barbarian Outpost", 2520, 3571),
                    Destination("Corporeal Beast", 2967, 4254, level = 2),
                    Destination("Tears of Guthix", 3245, 9500, level = 2),
                    Destination("Wintertodt Camp", 1631, 3940),
                ),
            "skills" to
                listOf(
                    Destination("Fishing Guild", 2611, 3390),
                    Destination("Mining Guild", 3049, 9763),
                    Destination("Crafting Guild", 2933, 3295),
                    Destination("Cooking Guild", 3144, 3438),
                    Destination("Woodcutting Guild", 1662, 3505),
                    Destination("Farming Guild", 1248, 3719),
                ),
            "combat" to
                listOf(
                    Destination("Warriors' Guild", 2882, 3547),
                    Destination("Champions' Guild", 3192, 3368),
                    Destination("Monastery", 3052, 3490),
                    Destination("Ranging Guild", 2653, 3439),
                ),
            "wealth" to
                listOf(
                    Destination("Miscellania", 2534, 3862),
                    Destination("Grand Exchange", 3163, 3478),
                    Destination("Falador", 2995, 3375),
                    Destination("Dondakan", 2824, 10168),
                ),
            "passage" to
                listOf(
                    Destination("Wizards' Tower", 3114, 3179),
                    Destination("The Outpost", 2430, 3349),
                    Destination("Eagles' Eyrie", 3405, 3158),
                    Destination("Wyrmscraig", 2591, 2221),
                ),
            "burning" to
                listOf(
                    Destination("Chaos Temple", 3234, 3634, wilderness = true),
                    Destination("Bandit Camp", 3038, 3651, wilderness = true),
                    Destination("Lava Maze", 3028, 3842, wilderness = true),
                ),
            "digsite" to
                listOf(
                    Destination("Digsite", 3341, 3445),
                    Destination("Fossil Island", 3763, 3869, level = 1),
                    Destination("Lithkren Dungeon", 3549, 10456, level = 1),
                ),
            "slayer" to
                listOf(
                    Destination("Stronghold", 2431, 3422),
                    Destination("Slayer Tower", 3421, 3537),
                    Destination("Fremennik Dungeon", 2802, 9999),
                    Destination("Tarn's Lair", 3185, 4601),
                    Destination("Dark Beasts", 2028, 4636),
                    Destination("Wyrmscraig Cavern", 2581, 8633),
                ),
            "xeric" to
                listOf(
                    Destination("Xeric's Lookout", 1579, 3530),
                    Destination("Xeric's Glade", 1752, 3566),
                    Destination("Xeric's Inferno", 1504, 3815),
                    Destination("Xeric's Heart", 1644, 3673),
                    Destination("Xeric's Honour", 1254, 3560),
                ),
            "memoirs" to
                listOf(
                    Destination("Lunch by the Lancalliums", 1713, 3612),
                    Destination("The Fisher's Flute", 1804, 3748),
                    Destination("History and Hearsay", 1478, 3576),
                    Destination("Jewellery of Jubilation", 1545, 3761),
                    Destination("A Dark Disposition", 1681, 3747),
                ),
            "drakans_medallion" to
                listOf(
                    Destination("Ver Sinhaza", 3649, 3230),
                    Destination("Darkmeyer", 3592, 3337),
                    Destination("Slepe", 3808, 9700, level = 1),
                    Destination("Castle Drakan", 3166, 7711),
                ),
            "ardougne_cloak_easy" to listOf(Destination("Kandarin Monastery", 2607, 3221)),
            "ardougne_cloak" to
                listOf(
                    Destination("Kandarin Monastery", 2607, 3221),
                    Destination("Ardougne Farm", 2665, 3376),
                ),
            "royal_seed_pod" to listOf(Destination("Grand Tree", 2465, 3495)),
            "ectophial" to listOf(Destination("Ectofuntus", 3659, 3522)),
            "spirit_tree" to
                listOf(
                    Destination("Tree Gnome Village", 2542, 3170),
                    Destination("Gnome Stronghold", 2461, 3444),
                    Destination("Battlefield of Khazard", 2555, 3259),
                    Destination("Grand Exchange", 3185, 3508),
                    Destination("Feldip Hills", 2488, 2850),
                    Destination("Prifddinas", 3274, 6123),
                    Destination("Poison Waste", 2339, 3109),
                    Destination("Laguna Aurorae", 1202, 2785),
                ),
        )

    val ITEMS: List<Item> =
        listOf(
            Item(
                key = "glory",
                network = "glory",
                label = "amulet",
                objs = chain("amulet_of_glory_", 6),
                uncharged = "obj.amulet_of_glory",
                teleportType = LEVEL_30,
            ),
            Item(
                key = "glory_trimmed",
                network = "glory",
                label = "amulet",
                objs = chain("trail_amulet_of_glory_", 6),
                uncharged = "obj.trail_amulet_of_glory",
                teleportType = LEVEL_30,
            ),
            Item(
                key = "glory_eternal",
                network = "glory",
                label = "amulet",
                objs = listOf("obj.amulet_of_glory_inf"),
                unlimited = true,
                teleportType = LEVEL_30,
            ),
            Item(
                key = "dueling",
                network = "dueling",
                label = "ring",
                objs = chain("ring_of_dueling_", 8),
            ),
            Item(
                key = "games",
                network = "games",
                label = "necklace",
                objs = chain("necklace_of_minigames_", 8),
            ),
            Item(
                key = "skills",
                network = "skills",
                label = "necklace",
                objs = chain("jewl_necklace_of_skills_", 6),
                uncharged = "obj.jewl_necklace_of_skills",
                teleportType = LEVEL_30,
            ),
            Item(
                key = "combat",
                network = "combat",
                label = "bracelet",
                objs = chain("jewl_bracelet_of_combat_", 6),
                uncharged = "obj.jewl_bracelet_of_combat",
                teleportType = LEVEL_30,
            ),
            Item(
                key = "wealth",
                network = "wealth",
                label = "ring",
                objs = chain("ring_of_wealth_", 5),
                uncharged = "obj.ring_of_wealth",
                teleportType = LEVEL_30,
            ),
            Item(
                key = "wealth_imbued",
                network = "wealth",
                label = "ring",
                objs = chain("ring_of_wealth_i", 5),
                uncharged = "obj.ring_of_wealth_i",
                teleportType = LEVEL_30,
            ),
            Item(
                key = "passage",
                network = "passage",
                label = "necklace",
                objs = chain("necklace_of_passage_", 5),
            ),
            Item(
                key = "burning",
                network = "burning",
                label = "amulet",
                objs = chain("burning_amulet_", 5),
            ),
            Item(
                key = "digsite",
                network = "digsite",
                label = "pendant",
                objs = chain("necklace_of_digsite_", 5),
            ),
            Item(
                key = "slayer",
                network = "slayer",
                label = "ring",
                objs = chain("slayer_ring_", 8),
                teleportType = LEVEL_30,
            ),
            Item(
                key = "slayer_eternal",
                network = "slayer",
                label = "ring",
                objs = listOf("obj.slayer_ring_eternal"),
                unlimited = true,
                teleportType = LEVEL_30,
            ),
            Item(
                key = "xeric",
                network = "xeric",
                label = "talisman",
                objs = listOf("obj.xeric_talisman"),
                chargeVarbit = "varbit.charges_xerics_talisman_quantity",
                maxCharges = 1000,
                chargeObj = "obj.lizardman_fang",
                inert = "obj.xeric_talisman_empty",
            ),
            Item(
                key = "memoirs",
                network = "memoirs",
                label = "book",
                objs = listOf("obj.veos_kharedsts_memoirs"),
                chargeVarbit = "varbit.veos_memoir_charges",
                maxCharges = 100,
            ),
            Item(
                key = "book_of_the_dead",
                network = "memoirs",
                label = "book",
                objs = listOf("obj.book_of_the_dead"),
                chargeVarbit = "varbit.veos_memoir_charges",
                maxCharges = 250,
            ),
            Item(
                key = "drakans_medallion",
                network = "drakans_medallion",
                label = "medallion",
                objs = listOf("obj.drakans_medallion"),
                unlimited = true,
            ),
            Item(
                key = "ardougne_cloak_easy",
                network = "ardougne_cloak_easy",
                label = "cloak",
                objs = listOf("obj.ardy_cape_easy"),
                unlimited = true,
            ),
            Item(
                key = "ardougne_cloak",
                network = "ardougne_cloak",
                label = "cloak",
                objs = listOf("obj.ardy_cape_medium", "obj.ardy_cape_hard", "obj.ardy_cape_elite"),
                unlimited = true,
            ),
            Item(
                key = "royal_seed_pod",
                network = "royal_seed_pod",
                label = "seed pod",
                objs = listOf("obj.mm2_royal_seed_pod"),
                unlimited = true,
                teleportType = LEVEL_30,
                arriveMessage = "The Royal seed pod teleports you to the Grand Tree.",
            ),
            Item(
                key = "ectophial",
                network = "ectophial",
                label = "ectophial",
                objs = listOf("obj.ectophial"),
                unlimited = true,
                departMessage = "You empty the ectoplasm onto the ground around your feet...",
                arriveMessage = "...and the world changes around you.",
            ),
        )

    fun destinationRow(network: String, ordinal: Int): String =
        "dbrow.teleport_dest_${network}_$ordinal"

    fun itemRow(key: String): String = "dbrow.teleport_item_$key"

    fun destinations() =
        dbTable("dbtable.teleport_destination", serverOnly = true) {
            column("network", DEST_NETWORK, VarType.STRING)
            column("ordinal", DEST_ORDINAL, VarType.INT)
            column("name", DEST_NAME, VarType.STRING)
            column("coords", DEST_COORDS, VarType.COORDGRID)
            column("wilderness", DEST_WILDERNESS, VarType.BOOLEAN)

            for ((network, stops) in NETWORKS) {
                stops.forEachIndexed { ordinal, stop ->
                    row(destinationRow(network, ordinal)) {
                        column(DEST_NETWORK, network)
                        column(DEST_ORDINAL, ordinal)
                        column(DEST_NAME, stop.name)
                        columnCoord(DEST_COORDS, CoordGrid(stop.x, stop.z, stop.level))
                        column(DEST_WILDERNESS, stop.wilderness)
                    }
                }
            }
        }

    fun items() =
        dbTable("dbtable.teleport_item", serverOnly = true) {
            column("objs", ITEM_OBJS, VarType.OBJ)
            column("network", ITEM_NETWORK, VarType.STRING)
            column("label", ITEM_LABEL, VarType.STRING)
            column("teleport_type", ITEM_TELEPORT_TYPE, VarType.STRING)
            column("uncharged", ITEM_UNCHARGED, VarType.OBJ)
            column("unlimited", ITEM_UNLIMITED, VarType.BOOLEAN)
            column("charge_varbit", ITEM_CHARGE_VARBIT, VarType.INT)
            column("max_charges", ITEM_MAX_CHARGES, VarType.INT)
            column("charge_obj", ITEM_CHARGE_OBJ, VarType.OBJ)
            column("inert", ITEM_INERT, VarType.OBJ)
            column("depart_message", ITEM_DEPART_MESSAGE, VarType.STRING)
            column("arrive_message", ITEM_ARRIVE_MESSAGE, VarType.STRING)

            for (item in ITEMS) {
                row(itemRow(item.key)) {
                    columnRSCM(ITEM_OBJS, *item.objs.toTypedArray())
                    column(ITEM_NETWORK, item.network)
                    column(ITEM_LABEL, item.label)
                    column(ITEM_TELEPORT_TYPE, item.teleportType)
                    item.uncharged?.let { columnRSCM(ITEM_UNCHARGED, it) }
                    column(ITEM_UNLIMITED, item.unlimited)
                    item.chargeVarbit?.let { columnRSCM(ITEM_CHARGE_VARBIT, it) }
                    item.maxCharges?.let { column(ITEM_MAX_CHARGES, it) }
                    item.chargeObj?.let { columnRSCM(ITEM_CHARGE_OBJ, it) }
                    item.inert?.let { columnRSCM(ITEM_INERT, it) }
                    item.departMessage?.let { column(ITEM_DEPART_MESSAGE, it) }
                    item.arriveMessage?.let { column(ITEM_ARRIVE_MESSAGE, it) }
                }
            }
        }
}
