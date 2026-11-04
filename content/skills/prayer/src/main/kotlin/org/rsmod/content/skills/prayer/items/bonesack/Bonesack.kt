package org.rsmod.content.skills.prayer.items.bonesack

import org.rsmod.api.attr.AttributeKey
import org.rsmod.api.invtx.invAdd
import org.rsmod.api.invtx.invDel
import org.rsmod.api.table.prayer.SkillPrayerRow
import org.rsmod.game.entity.Player
import org.rsmod.game.inv.Inventory

/**
 * The Rag and Bone Man's bonesack, reworked into a bone store: 28 bones of any kind, like a fish
 * barrel. Contents belong to the player rather than the sack, so a dropped sack loses nothing.
 */
object Bonesack {
    const val OBJ = "obj.rag_bonesack"
    const val CAPACITY = 28

    private val STORE = AttributeKey<MutableMap<String, Int>>(persistenceKey = "bonesack")

    val bones: Set<String> by lazy {
        SkillPrayerRow.all().filter { !it.ashes }.mapTo(LinkedHashSet()) { it.item.internalName }
    }

    fun contents(player: Player): Map<String, Int> = player.attr[STORE].orEmpty()

    fun total(player: Player): Int = contents(player).values.sum()

    private fun store(player: Player): MutableMap<String, Int> =
        player.attr.getOrPut(STORE) { mutableMapOf() }

    /** Moves up to [limit] of each bone in [types] from [inv] into the sack; returns how many. */
    fun fill(player: Player, inv: Inventory, types: Collection<String> = bones, limit: Int = CAPACITY): Int {
        var moved = 0
        for (bone in types) {
            val room = minOf(CAPACITY - total(player), limit - moved)
            if (room <= 0) break
            val take = minOf(room, inv.count(bone))
            if (take <= 0) continue
            if (player.invDel(inv, bone, take).failure) continue
            val sack = store(player)
            sack[bone] = (sack[bone] ?: 0) + take
            moved += take
        }
        return moved
    }

    /**
     * Moves as much of the sack as fits into [into]. A bone type that only partly fits stays in the
     * sack for the remainder, so nothing is lost when the destination fills up.
     */
    fun empty(player: Player, into: Inventory): Int {
        val sack = player.attr[STORE] ?: return 0
        var moved = 0
        for (bone in sack.keys.toList()) {
            val count = sack[bone] ?: continue
            val fits = (count downTo 1).firstOrNull { player.invAdd(into, bone, it).success } ?: 0
            if (fits == 0) continue
            moved += fits
            if (fits == count) sack.remove(bone) else sack[bone] = count - fits
        }
        if (sack.isEmpty()) player.attr.remove(STORE)
        return moved
    }
}
