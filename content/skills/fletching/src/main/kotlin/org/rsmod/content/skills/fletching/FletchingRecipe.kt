package org.rsmod.content.skills.fletching

import dev.openrune.types.ItemServerType
import dev.openrune.types.StatType
import org.rsmod.api.table.Tuple2
import org.rsmod.content.skills.Material
import org.rsmod.game.inv.Inventory

/**
 * One fletching recipe, normalised from whichever generated row type produced it.
 *
 * All five tables share this shape, which is what lets a single queued action drive every flow
 * instead of one loop per family.
 */
data class FletchingRecipe(
    val inputs: List<Material>,
    val output: ItemServerType,
    val outputAmount: Int,
    val statReq: List<Tuple2<StatType, Int>>,
    val xp: Double,
    val ticks: Int,
    val category: String,
    /** Held, never consumed, so deliberately not one of [inputs]. Null when none is needed. */
    val tool: String? = null,
)

/**
 * Darts, arrows, bolts and javelins are made in sets of [FletchingRecipe.outputAmount], one of each
 * input per item made, and live lets the last set come up short: two dart tips make two darts.
 */
val FletchingRecipe.allowsPartialSet: Boolean
    get() = outputAmount > 1 && inputs.all { it.count == outputAmount }

/**
 * How many items one action makes from [held] - the inventory count of each input, in [inputs]
 * order. Zero means the action cannot run.
 */
fun batchSize(setSize: Int, inputCounts: List<Int>, held: List<Int>, partial: Boolean): Int {
    if (partial) {
        return minOf(setSize, held.minOrNull() ?: 0)
    }
    val sets = inputCounts.zip(held).all { (needed, have) -> have >= needed }
    return if (sets) setSize else 0
}

/** How many actions [held] covers, counting a short final set where [batchSize] allows one. */
fun actionsAvailable(setSize: Int, inputCounts: List<Int>, held: List<Int>, partial: Boolean): Int {
    if (partial) {
        val units = held.minOrNull() ?: 0
        return units / setSize + if (units % setSize > 0) 1 else 0
    }
    return inputCounts.zip(held).minOfOrNull { (needed, have) -> have / needed } ?: 0
}

private fun FletchingRecipe.held(inv: Inventory): List<Int> =
    inputs.map { inv.count(it.obj.internalName) }

fun FletchingRecipe.batchSize(inv: Inventory): Int =
    batchSize(outputAmount, inputs.map { it.count }, held(inv), allowsPartialSet)

fun FletchingRecipe.hasMaterials(inv: Inventory): Boolean = batchSize(inv) > 0

fun FletchingRecipe.maxProducible(inv: Inventory): Int =
    actionsAvailable(outputAmount, inputs.map { it.count }, held(inv), allowsPartialSet)

/**
 * The tool is held rather than consumed, so it is a presence check and never limits how many can be
 * made - which is why it is not folded into [maxProducible].
 */
fun FletchingRecipe.hasTool(inv: Inventory): Boolean {
    val tool = tool ?: return true
    return FletchingDefinitions.toolVariants(tool).any { inv.count(it) > 0 }
}
