/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.ToolComponent
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ItemStack
import net.minecraft.registry.tag.ItemTags

class MiningToolItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    companion object {
        val VALUE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SILK_TOUCH, 1.0f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.2f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.FORTUNE, 0.33f),
            )
        private val COMPARATOR =
            ComparatorChain<MiningToolItemFacet>(
                compareBy { it.toolComponent.defaultMiningSpeed },
                compareBy { VALUE_ESTIMATOR.estimateValue(it.itemStack) },
                PREFER_BETTER_DURABILITY,
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    private val toolComponent: ToolComponent
    private val toolType: MiningToolItemType

    init {
        val component = this.itemStack.components.get(DataComponentTypes.TOOL)

        this.toolComponent = component ?: throw IllegalStateException("No tool component found")
        this.toolType = MiningToolItemType.getToolTypeFor(this.itemStack)
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.TOOL, this.toolType.ordinal)

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as MiningToolItemFacet)
    }

    enum class MiningToolItemType {
        PICKAXE,
        AXE,
        SHOVEL,
        HOE;

        companion object {
            fun getToolTypeFor(itemStack: ItemStack): MiningToolItemType {
                return when {
                    itemStack.isIn(ItemTags.PICKAXES) -> PICKAXE
                    itemStack.isIn(ItemTags.AXES) -> AXE
                    itemStack.isIn(ItemTags.SHOVELS) -> SHOVEL
                    itemStack.isIn(ItemTags.HOES) -> HOE
                    else -> {
                        logger.warn("Cannot get tool type of tool item ${itemStack.item}")

                        PICKAXE
                    }
                }
            }
        }
    }
}
