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
package net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.once
import net.ccbluex.liquidbounce.event.until
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.entity.getBottomPos
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket

/**
 * Cancels velocity and phases into the bottom pos of the block below the player.
 */
object VelocityPhase : VelocityMode("Phase") {

    // Same as [PhaseHypixel.GRAVITY]
    private const val GRAVITY = 0.07840000152

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
            val velocityX = packet.velocityX / 8000.0
            val velocityY = packet.velocityY / 8000.0
            val velocityZ = packet.velocityZ / 8000.0

            if (velocityY <= 0) {
                return@handler
            }

            attemptCancel() || return@handler
            event.cancelEvent()
        } else if (packet is ExplosionS2CPacket) {
            attemptCancel() || return@handler

            packet.playerKnockback.ifPresent { knockback ->
                knockback.x = 0.0
                knockback.y = 0.0
                knockback.z = 0.0
            }
        }
    }

    private fun attemptCancel(): Boolean {
        val bottomPos = player.getBottomPos() ?: return false
        debugGeometry("Block") {
            ModuleDebug.DebuggedPoint(
                bottomPos,
                Color4b(0, 255, 0, 100),
                0.1
            )
        }
        debugParameter("Block") { bottomPos.toString() }

        until<PlayerNetworkMovementTickEvent> { event ->
            // blockBelow.y + 1 is the top of the block
            event.y = (bottomPos.y + 1) - GRAVITY

            // The code above does nothing on POST
            event.state == EventState.PRE
        }

        until<QueuePacketEvent> { event ->
            if (event.origin == TransferOrigin.OUTGOING) {
                event.action = PacketQueueManager.Action.QUEUE
            }

            event.packet is CommonPongC2SPacket
        }
        return true
    }

}
