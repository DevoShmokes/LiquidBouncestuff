package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack
import net.ccbluex.liquidbounce.utils.attack.EntityUtils.isSelected
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.minecraft.item.EnumAction
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.Vec3
import kotlin.math.abs

object BetterVelocity : Module("BetterVelocity", Category.COMBAT) {

    private val reach by float("Reach", 2.9f, 0f..7f)
    private val wtapVertical by float("wtapVertical", 1f, 0f..1f)
    private val wtapHorizontal by float("wtapHorizontal", 0.8f, 0f..1f)
    private val comboVertical by float("comboVertical", 0.8f, 0f..1f)
    private val comboHorizontal by float("comboHorizontal", 0.6f, 0f..1f)
    private val requireSprint by boolean("requiresprint", true)
    private val debug by boolean("ShowEstimatedKB", true)
    private val veloReceived by boolean("ShowVeloRecieved", false)
    private val veloTaken by boolean("ShowVeloTaken", false)
    private val antiCombo by boolean("AntiCombo", false)
    private val requireEnemyInFov by boolean("RequireEnemyInFOV", false)
    private val fov by float("TargetInFOVforReduction", 120f, 1f..180f)
    private val range by float("TargetInRangeForReduction", 6f, 1f..8f)

    private var lastAttackedEntity: net.minecraft.entity.Entity? = null
    private var lastAttackTime = 0L

    private fun closestPointOnAABBToPoint(box: AxisAlignedBB, point: Vec3): Vec3 {
        val x = point.xCoord.coerceIn(box.minX, box.maxX)
        val y = point.yCoord.coerceIn(box.minY, box.maxY)
        val z = point.zCoord.coerceIn(box.minZ, box.maxZ)
        return Vec3(x, y, z)
    }

    private fun isInFOV(): Boolean {
        if (!requireEnemyInFov) return true

        val player = mc.thePlayer ?: return false

        if (player.heldItem?.itemUseAction == EnumAction.BLOCK) {
            val scaryEntity = mc.theWorld?.loadedEntityList?.filter { entity ->
                var result = false
                Backtrack.runWithNearestTrackedDistance(entity) {
                    result = isSelected(entity, true) &&
                        player.canEntityBeSeen(entity) &&
                        player.getDistanceToEntityBox(entity) <= range &&
                        RotationUtils.rotationDifference(entity) <= fov
                }
                result
            }?.minByOrNull { player.getDistanceToEntityBox(it) }

            return scaryEntity != null
        }

        return false
    }

    val onPacket = handler<PacketEvent> { event ->
        val player = mc.thePlayer ?: return@handler
        val packet = event.packet

        when (packet) {
            is S12PacketEntityVelocity -> {
                if (packet.entityID != player.entityId) return@handler
                if (requireSprint && !mc.gameSettings.keyBindSprint.isKeyDown) return@handler

                val x = abs(packet.motionX)
                val y = abs(packet.motionY)
                val z = abs(packet.motionZ)
                val motion = x * y

                if (veloReceived) {
                    ClientUtils.displayChatMessage("velo recieved X: $x Y: $y Z: $z")
                }

                if (isInFOV()) {
                    if (motion > 6000000 || x > 4000 || z > 4000) {
                        var combo = false
                        if (antiCombo) {
                            val entity = lastAttackedEntity
                            if (entity != null && player.posY - entity.posY < 1.4) {
                                val eyes = player.eyes
                                val closest = closestPointOnAABBToPoint(entity.hitBox, eyes)
                                if (closest.distanceTo(eyes) <= reach &&
                                    System.currentTimeMillis() - lastAttackTime < 1000L) {
                                    combo = true
                                }
                            }
                        }

                        if (combo) {
                            if (debug) ClientUtils.displayChatMessage("HIT attempted to stop combo")
                            packet.motionY = (packet.motionY * comboVertical).toInt()
                            packet.motionZ = (packet.motionZ * comboHorizontal).toInt()
                            packet.motionX = (packet.motionX * comboHorizontal).toInt()
                        } else {
                            if (debug) ClientUtils.displayChatMessage("HIT reduced wtap")
                            packet.motionY = (packet.motionY * wtapVertical).toInt()
                            packet.motionZ = (packet.motionZ * wtapHorizontal).toInt()
                            packet.motionX = (packet.motionX * wtapHorizontal).toInt()
                        }
                    } else {
                        if (debug) ClientUtils.displayChatMessage("HIY normal hit no reduction")
                    }
                } else if (debug) {
                    ClientUtils.displayChatMessage("HIT no enemy in fov")
                }

                val px = packet.motionX
                val pz = packet.motionZ
                if (veloTaken) {
                    ClientUtils.displayChatMessage("velo taken X: $px Z: $pz")
                }
            }

            is C02PacketUseEntity -> {
                if (packet.action == C02PacketUseEntity.Action.ATTACK) {
                    lastAttackTime = System.currentTimeMillis()
                    lastAttackedEntity = packet.getEntityFromWorld(mc.theWorld)
                }
            }
        }
    }
}

