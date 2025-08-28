/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.ClickBlockEvent
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
// Intentionally not using SilentHotbar to avoid silent swaps

object AutoTool : Module("AutoTool", Category.PLAYER, subjective = true, gameDetecting = false) {

    private val switchBack by boolean("SwitchBack", false)
    private val onlySneaking by boolean("OnlySneaking", false)
    private var previousSlot: Int? = null

    val onGameTick = handler<GameTickEvent> {
        if (!switchBack || mc.gameSettings.keyBindAttack.isKeyDown)
            return@handler

        val player = mc.thePlayer ?: return@handler

        previousSlot?.let { slot ->
            if (slot in 0..8 && player.inventory.currentItem != slot) {
                player.inventory.currentItem = slot
            }
            previousSlot = null
        }
    }

    val onClick = handler<ClickBlockEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        val block = mc.theWorld.getBlockState(event.clickedBlock ?: return@handler).block

        if (onlySneaking && !player.isSneaking || block.getBlockHardness(mc.theWorld, event.clickedBlock) == 0f)
            return@handler

        var fastest = 1f

        val slot = (0..8).maxByOrNull {
            val item = player.inventory.getStackInSlot(it) ?: return@maxByOrNull 1f

            item.getStrVsBlock(block).also { speed -> fastest = fastest.coerceAtLeast(speed) }
        } ?: return@handler

        if (fastest == (player.currentEquippedItem?.getStrVsBlock(block) ?: 1f))
            return@handler

        if (switchBack && previousSlot == null)
            previousSlot = player.inventory.currentItem

        // Perform a legit (visible) hotbar switch to the best tool
        player.inventory.currentItem = slot
    }

}
