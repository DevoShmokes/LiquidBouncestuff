/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import net.minecraft.entity.Entity
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C02PacketUseEntity.Action.ATTACK
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.START_SPRINTING
import net.minecraft.network.play.client.C0BPacketEntityAction.Action.STOP_SPRINTING
import net.minecraft.network.play.server.S29PacketSoundEffect

object BetterKB : Module("BetterKB", Category.COMBAT) {

    private val debug by boolean("Debug", false)
    private val sprint by boolean("ShowSprint", false)

    private var isSprinting = false
    private var blockInput = false
    private var lastAttackedEntity: Entity? = null
    private var lastAttackTime = 0L

    override fun onToggle(state: Boolean) {
        blockInput = false
    }

    val onPacket = handler<PacketEvent> { event ->
        val packet = event.packet
        when (packet) {
            is C0BPacketEntityAction -> when (packet.action) {
                START_SPRINTING -> {
                    if (sprint) {
                        ClientUtils.displayChatMessage("started sprinting")
                    }
                    isSprinting = true
                }
                STOP_SPRINTING -> {
                    if (sprint) {
                        ClientUtils.displayChatMessage("stopped sprinting")
                    }
                    isSprinting = false
                }
                else -> {}
            }
            is S29PacketSoundEffect -> {
                if (packet.soundName == "game.player.hurt") {
                    val entity = lastAttackedEntity
                    val distance = entity?.getDistance(packet.x, packet.y, packet.z)?.toDouble()
                    if (distance != null && packet.volume == 1.0f && isSprinting &&
                        System.currentTimeMillis() - lastAttackTime < 1000L && distance < 3.0
                    ) {
                        if (debug) {
                            val name = entity.name
                            ClientUtils.displayChatMessage("attacked $name distance: %.1f".format(distance))
                        }
                        lastAttackTime = 0L
                        blockInput = true
                    }
                }
            }
            is C02PacketUseEntity -> {
                if (packet.action == ATTACK) {
                    lastAttackTime = System.currentTimeMillis()
                    lastAttackedEntity = packet.getEntityFromWorld(mc.theWorld)
                }
            }
        }
    }

    fun shouldBlockInput(): Boolean {
        if (handleEvents()) {
            if (blockInput) {
                blockInput = false
                return true
            }
        }
        return false
    }
}

