package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.Backtrack.runWithNearestTrackedDistance
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.movement.FallingPlayer
import net.ccbluex.liquidbounce.utils.attack.EntityUtils
import net.ccbluex.liquidbounce.utils.render.drawWithTessellatorWorldRenderer
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
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemSword
import net.minecraft.item.EnumAction
import net.minecraft.block.BlockAir
import net.minecraft.util.BlockPos
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import org.lwjgl.input.Mouse
import org.lwjgl.opengl.GL11.*
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
    private val autoStick by boolean("AutoStick", false)
    private val autoStickSimulate by boolean("AutoStickSimulate", true) { autoStick }
    private val autoStickVoidTicks by int("AutoStickVoidTicks", 300, 50..400) { autoStickSimulate && autoStick }
    private val autoStickKBHorizontal by float("AutoStickKBHorizontal", 0.4f, 0.2f..1.0f) { autoStickSimulate && autoStick }
    private val autoStickKBPerLevel by float("AutoStickKBPerLevel", 0.5f, 0.0f..1.0f) { autoStickSimulate && autoStick }
    private val autoStickKBVertical by float("AutoStickKBVertical", 0.1f, 0.0f..0.5f) { autoStickSimulate && autoStick }
    private val autoStickRender by boolean("AutoStickRender", false) { autoStick }
    private val autoStickRenderRange by float("AutoStickRenderRange", 12f, 3f..24f) { autoStick }

    private val fov by float("TargetInFOVforBlock", 180f, 1f..180f)
    private val range by float("TargetInRangeForBlock", 4.4f, 1f..8f)
    private val enemyFOV by float("EnemyFOV", 90f, 1f..180f)

    private val blockDelay by int("BlockDelay", 50, 0..100) { block }

    private var leftDelay = generateClickDelay()
    private var leftLastSwing = 0L
    private var lastBlocking = 0L
    private var shouldJitter = false
    private var lastChosenSlot = -1

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
        lastChosenSlot = -1
    }

    val onRender3D = handler<Render3DEvent> {
        mc.thePlayer?.let { thePlayer ->
            val time = System.currentTimeMillis()
            val doubleClick = if (simulateDoubleClicking) nextInt(-1, 1) else 0

            if (block && thePlayer.swingProgress > 0 && !mc.gameSettings.keyBindUseItem.isKeyDown) {
                mc.gameSettings.keyBindUseItem.pressTime = 0
            }

            if (autoStick && autoStickRender) {
                renderAutoStickPrediction()
            }

            if (Mouse.isButtonDown(mc.gameSettings.keyBindAttack.keyCode + 100) && !mc.gameSettings.keyBindUseItem.isKeyDown && shouldAutoClick) {
                // Auto Stick decision before executing click/block logic
                maybeSwitchForAutoStick()
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

    // --- Auto Stick support ---
    private fun maybeSwitchForAutoStick() {
        if (!autoStick) return
        val player = mc.thePlayer ?: return

        // Prefer the entity under crosshair; otherwise pick nearest valid target
        val target = (mc.objectMouseOver?.entityHit as? EntityLivingBase)
            ?: mc.theWorld.loadedEntityList.asSequence()
                .filterIsInstance<EntityLivingBase>()
                .filter { EntityUtils.isSelected(it, true) }
                .filter { player.getDistanceToEntityBox(it) <= range }
                .minByOrNull { player.getDistanceToEntityBox(it) }
            ?: return
        if (!EntityUtils.isSelected(target, true)) return

        // Find KB stick first (also provides level)
        val kbStickSlot = findKnockbackStickSlot()
        val currentSlot = player.inventory.currentItem

        // Decide: simulation (preferred) or fallback heuristic
        val shouldUseStick = if (autoStickSimulate && kbStickSlot != -1) {
            val stack = player.inventory.getStackInSlot(kbStickSlot)
            val kbLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack)
            predictVoidWithKB(player, target, kbLevel)
        } else {
            isVoidBehindTarget(player, target)
        }

        if (shouldUseStick) {
            if (kbStickSlot != -1 && kbStickSlot != currentSlot && kbStickSlot != lastChosenSlot) {
                selectHotbarSlot(kbStickSlot)
                if (debug) ClientUtils.displayChatMessage("AutoStick: switching to KB stick (sim=${autoStickSimulate})")
                lastChosenSlot = kbStickSlot
            }
        } else {
            // If currently on a KB stick, prefer switching to a sword
            val held = player.heldItem
            if (isKnockbackStick(held)) {
                val swordSlot = findSwordSlot()
                if (swordSlot != -1 && swordSlot != currentSlot && swordSlot != lastChosenSlot) {
                    selectHotbarSlot(swordSlot)
                    if (debug) ClientUtils.displayChatMessage("AutoStick: switching back to sword")
                    lastChosenSlot = swordSlot
                }
            }
        }
    }

    // Predict if a KB hit would send target into void by simulating their future path
    private fun predictVoidWithKB(attacker: EntityLivingBase, target: EntityLivingBase, kbLevel: Int): Boolean {
        if (kbLevel <= 0) return false

        val (hx, hz) = computeKBVector(attacker, target, kbLevel,
            autoStickKBHorizontal, autoStickKBPerLevel
        )

        val initVX = target.motionX + hx
        val initVZ = target.motionZ + hz
        val initVY = if (target.motionY > autoStickKBVertical) target.motionY else autoStickKBVertical.toDouble()

        val sim = FallingPlayer(
            target.posX,
            target.posY,
            target.posZ,
            initVX,
            initVY,
            initVZ,
            attacker.rotationYaw,
            0f,
            0f
        )

        val collision = sim.findCollision(autoStickVoidTicks)
        return collision == null
    }

    private fun computeKBVector(
        attacker: EntityLivingBase,
        target: EntityLivingBase,
        kbLevel: Int,
        baseHorizontal: Float,
        perLevel: Float,
    ): Pair<Double, Double> {
        val dx = target.posX - attacker.posX
        val dz = target.posZ - attacker.posZ
        val len = kotlin.math.sqrt(dx * dx + dz * dz)
        if (len < 1e-6) return 0.0 to 0.0
        val dirX = dx / len
        val dirZ = dz / len
        val base = baseHorizontal * (1f + perLevel * kbLevel)
        // push away from attacker along (dirX, dirZ)
        return (dirX * base) to (dirZ * base)
    }

    private fun renderAutoStickPrediction() {
        val player = mc.thePlayer ?: return

        // Prefer entity under crosshair; fallback to nearest valid target within range
        val target = (mc.objectMouseOver?.entityHit as? EntityLivingBase)
            ?: mc.theWorld.loadedEntityList.asSequence()
                .filterIsInstance<EntityLivingBase>()
                .filter { EntityUtils.isSelected(it, true) }
                .filter { player.getDistanceToEntityBox(it) <= autoStickRenderRange }
                .minByOrNull { player.getDistanceToEntityBox(it) }
            ?: return
        if (!EntityUtils.isSelected(target, true)) return

        // Allow plain sticks for visualization; prefer actual KB stick if present
        val kbStickSlot = findKnockbackStickSlot()
        val anyStickSlot = findPlainStickSlot()
        val chosenSlot = if (kbStickSlot != -1) kbStickSlot else anyStickSlot
        if (chosenSlot == -1) return
        val stack = player.inventory.getStackInSlot(chosenSlot)
        var kbLevel = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack)
        if (kbLevel <= 0) kbLevel = 1 // visualize with minimal KB if not enchanted

        val (hx, hz) = computeKBVector(player, target, kbLevel, autoStickKBHorizontal, autoStickKBPerLevel)
        val initVX = target.motionX + hx
        val initVZ = target.motionZ + hz
        val initVY = if (target.motionY > autoStickKBVertical) target.motionY else autoStickKBVertical.toDouble()

        // Simulate tick-by-tick future positions until collision or limit
        val positions = mutableListOf<Vec3>()
        var x = target.posX
        var y = target.posY
        var z = target.posZ
        var vx = initVX
        var vy = initVY
        var vz = initVZ

        val world = mc.theWorld ?: return
        var hit = false
        repeat(autoStickVoidTicks) {
            val start = Vec3(x, y, z)

            // Physics step (mirrors FallingPlayer without input)
            vy -= 0.08
            vx *= 0.91
            vy *= 0.9800000190734863
            vy *= 0.91
            vz *= 0.91

            x += vx
            y += vy
            z += vz

            val end = Vec3(x, y, z)
            // Stop if ground collision is predicted (same condition as FallingPlayer)
            val result = world.rayTraceBlocks(start, end, true)
            if (result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && result.sideHit == net.minecraft.util.EnumFacing.UP) {
                positions += Vec3(result.blockPos.x + 0.5, result.blockPos.y.toDouble(), result.blockPos.z + 0.5)
                hit = true
                return@repeat
            }

            positions += end
        }

        // Render line strip of predicted positions in world space
        val manager = mc.renderManager ?: return

        glPushAttrib(GL_ALL_ATTRIB_BITS)
        glPushMatrix()
        glDisable(GL_TEXTURE_2D)
        glEnable(GL_BLEND)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDisable(GL_DEPTH_TEST)
        glLineWidth(2f)
        glColor4f(0f, 1f, 0f, 0.8f)

        drawWithTessellatorWorldRenderer {
            begin(GL_LINE_STRIP, DefaultVertexFormats.POSITION)
            positions.forEach { p ->
                pos(p.xCoord - manager.viewerPosX, p.yCoord - manager.viewerPosY, p.zCoord - manager.viewerPosZ).endVertex()
            }
        }

        // Mark end point (collision or last)
        val last = positions.lastOrNull()
        if (last != null) {
            glPointSize(6f)
            glBegin(GL_POINTS)
            if (hit) glColor4f(1f, 0f, 0f, 0.9f) else glColor4f(1f, 1f, 0f, 0.9f)
            glVertex3d(last.xCoord - manager.viewerPosX, last.yCoord - manager.viewerPosY, last.zCoord - manager.viewerPosZ)
            glEnd()
        }

        glEnable(GL_DEPTH_TEST)
        glDisable(GL_BLEND)
        glEnable(GL_TEXTURE_2D)
        glPopMatrix()
        glPopAttrib()
    }

    private fun isVoidBehindTarget(player: EntityLivingBase, target: EntityLivingBase): Boolean {
        val world = mc.theWorld ?: return false

        val dx = target.posX - player.posX
        val dz = target.posZ - player.posZ
        val len = kotlin.math.sqrt((dx * dx + dz * dz))
        if (len < 0.001) return false
        val dirX = dx / len
        val dirZ = dz / len

        // Sample a few steps behind the target along player->target direction
        // and ensure there is no solid ground beneath for several blocks.
        val baseY = kotlin.math.floor(target.posY).toInt()
        for (step in 1..4) {
            val sx = target.posX + dirX * step
            val sz = target.posZ + dirZ * step
            val sample = BlockPos(sx, baseY.toDouble(), sz)

            var clearBelow = true
            var checkPos = sample.down()
            var checks = 0
            while (checks < 3 && checkPos.y > 0) {
                val block = world.getBlockState(checkPos).block
                if (block !is BlockAir) {
                    clearBelow = false
                    break
                }
                checkPos = checkPos.down()
                checks++
            }

            if (clearBelow) return true
        }
        return false
    }

    private fun findKnockbackStickSlot(): Int {
        val player = mc.thePlayer ?: return -1
        for (slot in 0..8) {
            val stack = player.inventory.getStackInSlot(slot)
            if (isKnockbackStick(stack)) return slot
        }
        return -1
    }

    private fun findPlainStickSlot(): Int {
        val player = mc.thePlayer ?: return -1
        for (slot in 0..8) {
            val stack = player.inventory.getStackInSlot(slot)
            if (stack != null && stack.item == Items.stick) return slot
        }
        return -1
    }

    private fun isKnockbackStick(stack: ItemStack?): Boolean {
        if (stack == null) return false
        if (stack.item != Items.stick) return false
        val level = EnchantmentHelper.getEnchantmentLevel(Enchantment.knockback.effectId, stack)
        return level > 0
    }

    private fun findSwordSlot(): Int {
        val player = mc.thePlayer ?: return -1
        for (slot in 0..8) {
            val stack = player.inventory.getStackInSlot(slot)
            if (stack?.item is ItemSword) return slot
        }
        return -1
    }

    private fun selectHotbarSlot(slot: Int) {
        val player = mc.thePlayer ?: return
        if (slot < 0 || slot > 8) return
        if (player.inventory.currentItem == slot) return
        player.inventory.currentItem = slot
        mc.playerController.updateController()
    }
}
