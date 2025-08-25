/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.ccbluex.liquidbounce.utils.inventory.SilentHotbar
import net.ccbluex.liquidbounce.utils.inventory.attackDamage
import net.ccbluex.liquidbounce.utils.movement.FallingPlayer
import net.minecraft.item.ItemSword
import net.minecraft.item.ItemTool
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.EntityLivingBase
import kotlin.math.cos
import kotlin.math.sin

object AutoWeapon : Module("AutoWeapon", Category.COMBAT, subjective = true) {

    private val onlySword by boolean("OnlySword", false)

    private val spoof by boolean("SpoofItem", false)
    private val spoofTicks by int("SpoofTicks", 10, 1..20) { spoof }

    // Prefer KB stick logic
    private val preferKBInVoid by boolean("PreferKBInVoid", true)
    private val kbVoidTicks by int("VoidPredictTicks", 300, 20..400) { preferKBInVoid }
    private val kbHorizontal by float("KBHorizontal", 0.4f, 0.2f..1.0f) { preferKBInVoid }
    private val kbPerLevel by float("KBPerLevel", 0.5f, 0.0f..1.0f) { preferKBInVoid }
    private val kbVertical by float("KBVertical", 0.1f, 0.0f..0.5f) { preferKBInVoid }
    // Optional debug disabled by default (no chat spam)
    // private val debugLog by boolean("DebugKB", false) { preferKBInVoid }.subjective()

    private var attackEnemy = false
    private var lastTarget: EntityLivingBase? = null

    val onAttack = handler<AttackEvent> {
        attackEnemy = true
        lastTarget = it.targetEntity as? EntityLivingBase
    }

    val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler

        if (event.packet is C02PacketUseEntity && event.packet.action == ATTACK && attackEnemy) {
            attackEnemy = false

            // Try to prefer a KB-enchanted item if hitting should void the target.
            var preferredSlot: Int? = null
            if (preferKBInVoid) {
                val target = lastTarget
                    ?: (event.packet as? C02PacketUseEntity)?.getEntityFromWorld(mc.theWorld) as? EntityLivingBase

                if (target != null && target.isEntityAlive) {
                    // Find highest knockback-level item in hotbar
                    val kbCandidates = (0..8)
                        .map { it to player.inventory.getStackInSlot(it) }
                        .filter { it.second != null }
                        .mapNotNull { (slot, stack) ->
                            val lvl = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack)
                            if (lvl > 0 && (!onlySword || stack.item is ItemSword)) (slot to lvl) else null
                        }

                    val bestKBTuple = kbCandidates.maxByOrNull { it.second }

                    if (bestKBTuple != null) {
                        val (kbSlot, kbLevel) = bestKBTuple

                        if (willFallIntoVoidAfterKB(player.rotationYaw, target, kbLevel)) {
                            preferredSlot = kbSlot
                        }
                    }
                }
            }

            // If no preferred KB slot, fall back to best damage weapon in hotbar (#Kotlin Style)
            val (slot, _) = if (preferredSlot != null) {
                preferredSlot!! to player.inventory.getStackInSlot(preferredSlot!!)
            } else {
                (0..8)
                    .map { it to player.inventory.getStackInSlot(it) }
                    .filter {
                        it.second != null && ((onlySword && it.second.item is ItemSword)
                                || (!onlySword && (it.second.item is ItemSword || it.second.item is ItemTool)))
                    }
                    .maxByOrNull { it.second.attackDamage } ?: return@handler
            }

            if (slot == mc.thePlayer.inventory.currentItem) // If in hand no need to swap
                return@handler

            // Switch to best weapon
            SilentHotbar.selectSlotSilently(this, slot, spoofTicks, true, !spoof, spoof)

            if (!spoof) {
                player.inventory.currentItem = slot
                SilentHotbar.resetSlot(this)
            }

            // Resend attack packet
            sendPacket(event.packet)
            event.cancelEvent()
        }
    }

    private fun willFallIntoVoidAfterKB(attackerYaw: Float, target: EntityLivingBase, kbLevel: Int): Boolean {
        val (hx, hz) = computeKBVector(attackerYaw, kbLevel)
        val startX = target.posX
        val startY = target.posY
        val startZ = target.posZ

        // Use FallingPlayer to simulate future path with initial knockback velocity
        val sim = FallingPlayer(
            startX,
            startY,
            startZ,
            hx.toDouble(),
            kbVertical.toDouble(),
            hz.toDouble(),
            attackerYaw,
            0f,
            0f
        )

        val collision = sim.findCollision(kbVoidTicks)
        return collision == null
    }

    private fun computeKBVector(attackerYaw: Float, kbLevel: Int): Pair<Float, Float> {
        val yawRad = Math.toRadians(attackerYaw.toDouble())
        val base = kbHorizontal * (1f + kbPerLevel * kbLevel)
        val x = (-sin(yawRad)).toFloat() * base
        val z = (cos(yawRad)).toFloat() * base
        return x to z
    }
}
