package net.ccbluex.liquidbounce.features.module.modules.`fun`

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.ServerUtils
import net.ccbluex.liquidbounce.utils.rotation.Rotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.currentRotation
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils.serverRotation
import net.minecraft.client.settings.GameSettings
import net.minecraft.network.play.server.S45PacketTitle

/**
 * AutoDance
 * Spins, crouches, and swings after detecting a win on Hypixel.
 */
object AutoDance : Module("AutoDance", Category.FUN, subjective = true, gameDetecting = false) {

    // Settings
    private val hypixelOnly by boolean("HypixelOnly", true)
    private val durationTicks by int("DurationTicks", 120, 20..400) // default ~6s
    private val spinSpeed by float("SpinSpeed", 30f, 5f..90f) // degrees per tick
    private val crouchInterval by int("CrouchInterval", 5, 1..20)
    private val swingInterval by int("SwingInterval", 2, 1..10)
    private val switchToFist by boolean("SwitchToFist", true)

    // State
    private var remainingTicks = 0
    private var yaw = 0f
    private var tickCounter = 0

    override fun onEnable() {
        // Do not auto-start; wait for actual win detection
        remainingTicks = 0
        tickCounter = 0
    }

    override fun onDisable() {
        stopDance()
    }

    private fun shouldTriggerForServer(): Boolean {
        if (!hypixelOnly) return true
        return ServerUtils.remoteIp.contains("hypixel", ignoreCase = true)
    }

    private fun startDance() {
        val player = mc.thePlayer ?: return
        remainingTicks = durationTicks
        tickCounter = 0
        yaw = serverRotation.yaw

        if (switchToFist) {
            // Try to switch to an empty hotbar slot to swing with fist
            for (slot in 0..8) {
                val stack = player.inventory.mainInventory[slot]
                if (stack == null) {
                    player.inventory.currentItem = slot
                    break
                }
            }
        }
    }

    private fun stopDance() {
        // Release sneak key if we toggled it
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false
        }
        // Reset rotation handling back to normal
        RotationUtils.syncSpecialModuleRotations()
        currentRotation = null
        remainingTicks = 0
    }

    private fun checkWinMessage(text: String): Boolean {
        val msg = text.uppercase()
        // Common Hypixel end-game indicators
        return ("VICTORY" in msg || "WINNER" in msg || "YOU WIN" in msg)
    }

    // Detect Hypixel win via title packets only to avoid lobby false positives
    val onPacket = handler<PacketEvent> { event ->
        if (!shouldTriggerForServer()) return@handler

        when (val p = event.packet) {
            is S45PacketTitle -> {
                val t = p.message?.unformattedText ?: return@handler
                // Only react to main title/subtitle displays
                val typeName = p.type?.name ?: ""
                if (typeName.equals("TITLE", true) || typeName.equals("SUBTITLE", true)) {
                    if (checkWinMessage(t)) startDance()
                }
            }
        }
    }

    // Apply rotations just before they are sent to the server
    val onRotationUpdate = handler<RotationUpdateEvent> {
        if (remainingTicks <= 0) return@handler

        // Increment yaw and apply fixed sensitivity
        yaw += spinSpeed
        if (yaw > 180f) yaw -= 360f
        if (yaw < -180f) yaw += 360f

        currentRotation = Rotation(yaw, serverRotation.pitch).fixedSensitivity()
    }

    // Handle crouch toggling and swinging each tick
    val onUpdate = handler<UpdateEvent> {
        if (remainingTicks <= 0) return@handler

        val player = mc.thePlayer ?: return@handler
        tickCounter++
        remainingTicks--

        // Toggle sneaking unless user physically holds the key
        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            val pressed = (tickCounter % (crouchInterval * 2)) < crouchInterval
            mc.gameSettings.keyBindSneak.pressed = pressed
        }

        // Swing item every swingInterval ticks
        if (tickCounter % swingInterval == 0) {
            player.swingItem()
        }

        // Auto-stop if player died or left world
        if (mc.theWorld == null || player.isDead) {
            stopDance()
        }

        // End reached
        if (remainingTicks <= 0) stopDance()
    }
}
