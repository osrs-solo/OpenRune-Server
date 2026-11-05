package org.rsmod.content.skills.magic.runepouch

import dev.openrune.rscm.RSCM
import dev.openrune.rscm.RSCMType
import dev.openrune.types.enums.EnumTypeMap
import dev.openrune.types.enums.`enum`
import org.rsmod.api.player.vars.VarPlayerIntMapSetter
import org.rsmod.game.entity.Player

/**
 * The ten bank loadouts `a`..`j`, four slots each. A slot is a rune compact id plus a cap (0 means
 * "fill to [RunePouch.CAPACITY]"). Where the cache splits a value over several varbits
 * (`_bitsa` low bits first), the widths match the client's `rune_pouch_loadout_getdata`.
 */
object RunePouchLoadouts {
    const val COUNT = 10

    val letters: List<String> = ('a'..'j').map(Char::toString)

    /** The bankside name picker's rows in order, as keys into the loadout name strings. */
    private val nameOrder: Map<Int, Int> by lazy {
        val order: EnumTypeMap<Int, Int> = enum("rune_pouch_loadout_name_order")
        order.filterValuesNotNull().backing
    }

    val nameCount: Int get() = nameOrder.size

    fun nameValue(index: Int): Int? = nameOrder[index]

    data class Entry(val rune: String, val cap: Int)

    fun entry(player: Player, loadout: Int, slot: Int): Entry? {
        val letter = letters[loadout]
        val compact = read(player, "varbit.rune_pouch_loadout_${letter}_rune${slot + 1}", RUNE_BITS)
        val rune = RunePouch.runeForCompactId(compact) ?: return null
        val cap = read(player, "varbit.rune_pouch_loadout_${letter}_cap${slot + 1}", CAP_BITS)
        return Entry(rune, if (cap in 1 until RunePouch.CAPACITY) cap else 0)
    }

    fun entries(player: Player, loadout: Int, slots: Int): List<Entry?> =
        (0 until slots).map { entry(player, loadout, it) }

    fun setEntry(player: Player, loadout: Int, slot: Int, rune: String?, cap: Int) {
        val letter = letters[loadout]
        val compact = rune?.let(RunePouch::compactId) ?: 0
        val storedCap = if (compact == 0 || cap !in 1 until RunePouch.CAPACITY) 0 else cap
        write(player, "varbit.rune_pouch_loadout_${letter}_rune${slot + 1}", RUNE_BITS, compact)
        write(player, "varbit.rune_pouch_loadout_${letter}_cap${slot + 1}", CAP_BITS, storedCap)
    }

    fun setName(player: Player, loadout: Int, value: Int) {
        val varbit = "varbit.rune_pouch_loadout_name_${letters[loadout]}"
        VarPlayerIntMapSetter.set(player, varbit, value)
    }

    fun name(player: Player, loadout: Int): Int =
        player.vars["varbit.rune_pouch_loadout_name_${letters[loadout]}"]

    private val RUNE_BITS = intArrayOf(4)
    private val CAP_BITS = intArrayOf(6, 4)

    private val SPLIT_SUFFIXES = listOf("_bitsa", "_bitsb", "_bitsc")

    private fun parts(name: String, widths: IntArray): List<Pair<String, Int>> {
        if (RSCM.getRSCMOrNull(name, RSCMType.VARBIT) != null) return listOf(name to 0)
        var shift = 0
        return SPLIT_SUFFIXES.take(widths.size + 1).mapIndexed { i, suffix ->
            val part = (name + suffix) to shift
            if (i < widths.size) shift += widths[i]
            part
        }
    }

    private fun read(player: Player, name: String, widths: IntArray): Int =
        parts(name, widths).sumOf { (varbit, shift) -> player.vars[varbit] shl shift }

    private fun write(player: Player, name: String, widths: IntArray, value: Int) {
        val parts = parts(name, widths)
        for ((i, part) in parts.withIndex()) {
            val (varbit, shift) = part
            val mask = if (i == parts.lastIndex) -1 else (1 shl widths[i]) - 1
            VarPlayerIntMapSetter.set(player, varbit, (value ushr shift) and mask)
        }
    }
}
