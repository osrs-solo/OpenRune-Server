package org.rsmod.content.skills.magic.runepouch

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.rsmod.api.inv.storage.PlayerItemStorageContext
import org.rsmod.api.inv.storage.PlayerItemStorageHook
import org.rsmod.api.inv.storage.VirtualItemConsumePolicy

/**
 * Lets the runes in a held rune pouch count as inventory runes, so spells (and anything else that
 * deletes runes through the inventory) draw on the pouch once the inventory's own stack runs out.
 * Runes are only ever put in the pouch through its interface, so nothing incoming is stored.
 */
@Singleton
internal class RunePouchStorageHook @Inject constructor() : PlayerItemStorageHook {
    override val consumePolicy: VirtualItemConsumePolicy = VirtualItemConsumePolicy.InventoryFirst

    override fun shouldProcess(ctx: PlayerItemStorageContext): Boolean =
        ctx.inventory === ctx.player.inv &&
            RunePouch.isRune(ctx.itemInternal) &&
            RunePouch.slotCount(ctx.player) > 0

    override fun contains(ctx: PlayerItemStorageContext): Int =
        RunePouch.usableAmount(ctx.player, ctx.itemInternal)

    override fun remove(ctx: PlayerItemStorageContext, amount: Int): Int =
        RunePouch.take(ctx.player, ctx.itemInternal, amount)

    override fun add(ctx: PlayerItemStorageContext, amount: Int): Int = 0
}
