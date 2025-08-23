package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack.runWithNearestTrackedDistance
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.attack.EntityUtils
import net.ccbluex.liquidbounce.utils.client.ClientUtils
import net.ccbluex.liquidbounce.utils.extensions.fixedSensitivityPitch
import net.ccbluex.liquidbounce.utils.extensions.fixedSensitivityYaw
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.extensions.isBlock
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextFloat
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils.nextInt
import net.ccbluex.liquidbounce.utils.timing.TimeUtils.randomClickDelay
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Items
import net.minecraft.item.EnumAction
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.random.Random.Default.nextBoolean

object BetterAutoClicker : Module("BetterAutoClicker", Category.COMBAT) {

    private val simulateDoubleClicking by boolean("SimulateDoubleClicking", false)

    private val maxCPSValue = int("MaxCPS", 8, 1..20)
    private val maxCPS by maxCPSValue

    private val minCPSValue = int("MinCPS", 5, 1..20)
    private val minCPS by minCPSValue

    private val debug by boolean("showattacks", false)
    private val debug2 by boolean("shownemyangletoyou", false)
    private val debug3 by boolean("showshouldblock", false)
    private val debug4 by boolean("showblocks", false)

    private val left by boolean("Left", true)
    private val jitter by boolean("Jitter", false)
    private val block by boolean("AutoBlock", false)

    private val fov by float("TargetInFOVforBlock", 180f, 1f..180f)
    private val range by float("TargetInRangeForBlock", 4.4f, 1f..8f)
    private val enemyFOV by float("EnemyFOV", 90f, 1f..180f)

    private val blockDelay by int("BlockDelay", 50, 0..100) { block }

    private var leftDelay = generateClickDelay()
    private var leftLastSwing = 0L
    private var lastBlocking = 0L
    private var shouldJitter = false

    init {
        maxCPSValue.onChange { _, new -> new.coerceAtLeast(minCPS) }
        minCPSValue.onChange { _, new -> new.coerceAtMost(maxCPS) }
        minCPSValue.setSupport { !maxCPSValue.isMinimal() }
    }

    private val shouldAutoClick
        get() = mc.thePlayer.capabilities.isCreativeMode || !mc.objectMouseOver.typeOfHit.isBlock

    override fun onDisable() {
        leftLastSwing = 0L
        lastBlocking = 0L
    }

    val onRender3D = handler<Render3DEvent> {
        mc.thePlayer?.let { thePlayer ->
            val time = System.currentTimeMillis()
            val doubleClick = if (simulateDoubleClicking) nextInt(-1, 1) else 0

            if (block && thePlayer.swingProgress > 0 && !mc.gameSettings.keyBindUseItem.isKeyDown) {
                mc.gameSettings.keyBindUseItem.pressTime = 0
            }

            if (mc.gameSettings.keyBindAttack.isKeyDown && !mc.gameSettings.keyBindUseItem.isKeyDown && shouldAutoClick) {
                if (left && time - leftLastSwing >= leftDelay) {
                    handleLeftClick(time, doubleClick)
                } else if (block && isWorthBlocking() && mc.gameSettings.keyBindAttack.pressTime != 0) {
                    handleBlock(time)
                }
            }
        }
    }

    val onTick = handler<UpdateEvent> {
        if (debug3) {
            ClientUtils.displayChatMessage("should block: ${isWorthBlocking()}")
        }

        mc.thePlayer?.let { thePlayer ->
            shouldJitter = !mc.objectMouseOver.typeOfHit.isBlock &&
                (thePlayer.isSwingInProgress || mc.gameSettings.keyBindAttack.pressTime != 0)

            if (jitter && left && shouldAutoClick && shouldJitter) {
                if (nextBoolean()) thePlayer.fixedSensitivityYaw += nextFloat(-1f, 1f)
                if (nextBoolean()) thePlayer.fixedSensitivityPitch += nextFloat(-1f, 1f)
            }
        }
    }

    private fun isWorthBlocking(): Boolean {
        val player = mc.thePlayer ?: return false
        if (player.heldItem?.itemUseAction !in arrayOf(EnumAction.BLOCK)) return false

        val swordOrAxeItems = listOf(
            Items.iron_sword, Items.golden_sword, Items.diamond_sword, Items.wooden_sword, Items.stone_sword,
            Items.iron_axe, Items.golden_axe, Items.diamond_axe, Items.wooden_axe, Items.stone_axe
        )

        val scaryEntity = mc.theWorld.loadedEntityList
            .filterIsInstance<Entity>()
            .filter { entity ->
                var result = false
                runWithNearestTrackedDistance(entity) {
                    if (!EntityUtils.isSelected(entity, true) || !player.canEntityBeSeen(entity)) return@runWithNearestTrackedDistance
                    if (player.getDistanceToEntityBox(entity) > range) return@runWithNearestTrackedDistance
                    if (RotationUtils.rotationDifference(entity) > fov) return@runWithNearestTrackedDistance
                    val held = (entity as? EntityLivingBase)?.heldItem?.item
                    if (held !in swordOrAxeItems) return@runWithNearestTrackedDistance
                    result = isWithinEnemyFOV(entity, false)
                }
                result
            }
            .minByOrNull { player.getDistanceToEntityBox(it) }
            ?: return false

        if (debug2) {
            isWithinEnemyFOV(scaryEntity, true)
        }

        return true
    }

    private fun isWithinEnemyFOV(entity: Entity, test: Boolean): Boolean {
        val enemyYaw = entity.rotationYaw
        val enemyPitch = entity.rotationPitch
        val enemyEyePos = entity.getPositionEyes(1.0f)
        val playerAABB = mc.thePlayer.entityBoundingBox
        val closestPoint = getClosestPointOnAABB(playerAABB, enemyEyePos)
        val deltaYaw = normalizeAngle(getYawToPoint(entity, closestPoint) - enemyYaw)
        val deltaPitch = normalizeAngle(getPitchToPoint(entity, closestPoint) - enemyPitch)

        if (test) {
            ClientUtils.displayChatMessage("angle yaw: $deltaYaw pitch: $deltaPitch")
        }

        return abs(deltaYaw) <= enemyFOV / 2f && abs(deltaPitch) <= enemyFOV / 2f
    }

    private fun normalizeAngle(angle: Float): Float {
        var normalized = angle % 360f
        if (normalized > 180f) normalized -= 360f
        if (normalized < -180f) normalized += 360f
        return normalized
    }

    private fun getClosestPointOnAABB(aabb: AxisAlignedBB, point: Vec3): Vec3 {
        val x = point.xCoord.coerceIn(aabb.minX, aabb.maxX)
        val y = point.yCoord.coerceIn(aabb.minY, aabb.maxY)
        val z = point.zCoord.coerceIn(aabb.minZ, aabb.maxZ)
        return Vec3(x, y, z)
    }

    private fun getYawToPoint(entity: Entity, point: Vec3): Float {
        val deltaX = point.xCoord - entity.posX
        val deltaZ = point.zCoord - entity.posZ
        return ((Math.toDegrees(atan2(deltaZ, deltaX)) - 90.0).toFloat()) % 360f
    }

    private fun getPitchToPoint(entity: Entity, point: Vec3): Float {
        val deltaY = point.yCoord - (entity.posY + entity.eyeHeight.toDouble())
        val distance = entity.getDistance(point.xCoord, entity.posY, point.zCoord)
        return (Math.toDegrees(atan2(deltaY, distance)) * -1.0).toFloat() % 360f
    }

    private fun handleLeftClick(time: Long, doubleClick: Int) {
        repeat(1 + doubleClick) {
            if (debug) {
                ClientUtils.displayChatMessage("clicked")
            }
            KeyBinding.onTick(mc.gameSettings.keyBindAttack.keyCode)
            leftLastSwing = time
            leftDelay = generateClickDelay()
        }
    }

    private fun handleBlock(time: Long) {
        if (time - lastBlocking >= blockDelay) {
            if (debug4) {
                ClientUtils.displayChatMessage("blocking")
            }
            KeyBinding.onTick(mc.gameSettings.keyBindUseItem.keyCode)
            lastBlocking = time
        }
    }

    private fun generateClickDelay() = randomClickDelay(minCPS, maxCPS)
}
