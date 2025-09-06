package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.BlockUtils
import net.ccbluex.liquidbounce.utils.kotlin.StringUtils.contains
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.server.S45PacketTitle
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import kotlin.math.roundToInt

/**
 * BaseProtect: Detect BedWars start, locate own bed, and warn when enemies approach it.
 */
object BaseProtect : Module("BaseProtect", Category.MISC) {

    // Config
    private val alertRange by int("AlertRange", 20, 10..100)
    private val searchRadius by int("BedSearchRadius", 32, 8..64)
    private val warnDelay by int("WarnDelay", 5000, 1000..30000)
    private val alertSound by text("AlertSound", "ambient.weather.thunder")
    private val alertVolume by float("AlertVolume", 10.0f, 0f..10f)

    // State
    @Volatile private var inBedwars = false
    @Volatile private var countdownSeen = false
    @Volatile private var gameStarted = false
    @Volatile private var bedPos: BlockPos? = null
    private val recentlyWarned = mutableMapOf<String, Long>()

    private val BW_SUBSTRINGS = arrayOf("bedwars", "bed wars")

    private fun tryDetectBedwarsAndBed() {
        val world = mc.theWorld ?: return

        // Scoreboard-based BedWars detection
        val scoreboard = world.scoreboard
        val title = scoreboard.getObjectiveInDisplaySlot(1)?.displayName
        val titleMatches = BW_SUBSTRINGS in title
        val anyObjectiveMatches = world.scoreboard.objectiveNames.any { BW_SUBSTRINGS in it }
        val anyTeamMatches = world.scoreboard.teams.any { BW_SUBSTRINGS in it.colorPrefix }

        val scoreboardLooksLikeBW = titleMatches || anyObjectiveMatches || anyTeamMatches
        val wasInBedwars = inBedwars
        // Mark in-game purely off countdown-based start to avoid lobby beds
        inBedwars = gameStarted

        if (!wasInBedwars && inBedwars) {
            // On transition to detected BedWars, lock bed position near player
            if (bedPos == null) bedPos = findNearestBed()
            bedPos?.let {
                chat("§7[BaseProtect] §aBed detected at §f${it.x} ${it.y} ${it.z}§a. Range: §f${alertRange}")
            } ?: run {
                chat("§7[BaseProtect] §eBedWars detected. Searching for your bed...")
            }
        }
    }

    private fun findNearestBed(): BlockPos? {
        val world = mc.theWorld ?: return null
        val player = mc.thePlayer ?: return null

        val found = BlockUtils.searchBlocks(searchRadius, setOf(Blocks.bed), 256)
        if (found.isEmpty()) return null

        return found.keys.minByOrNull { pos ->
            val dx = player.posX - (pos.x + 0.5)
            val dy = player.posY - (pos.y + 0.5)
            val dz = player.posZ - (pos.z + 0.5)
            dx * dx + dy * dy + dz * dz
        }
    }

    private fun distanceToBedSq(x: Double, y: Double, z: Double): Double {
        val bed = bedPos ?: return Double.MAX_VALUE
        val dx = x - (bed.x + 0.5)
        val dy = y - (bed.y + 0.5)
        val dz = z - (bed.z + 0.5)
        return dx * dx + dy * dy + dz * dz
    }

    private fun isTeammate(entity: EntityPlayer): Boolean {
        // Always defer to Teams module logic so settings there control behavior
        return Teams.isInYourTeam(entity)
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler

        // Keep detection lightweight every tick
        tryDetectBedwarsAndBed()

        if (!inBedwars) return@handler

        // If bed not yet found (e.g., game just started), keep searching until found
        if (bedPos == null) {
            bedPos = findNearestBed()
            bedPos?.let { chat("§7[BaseProtect] §aBed located at §f${it.x} ${it.y} ${it.z}") }
        }

        val bed = bedPos ?: return@handler
        val range = alertRange
        val rangeSq = range.toDouble() * range
        val now = System.currentTimeMillis()

        for (entity in world.playerEntities) {
            if (entity == player) continue
            if (AntiBot.isBot(entity)) continue
            if (entity is EntityPlayer && isTeammate(entity)) continue

            val distSq = distanceToBedSq(entity.posX, entity.posY, entity.posZ)
            if (distSq <= rangeSq) {
                val key = entity.uniqueID.toString()
                val last = recentlyWarned[key] ?: 0L
                if (now - last >= warnDelay) {
                    val dist = kotlin.math.sqrt(distSq).roundToInt()
                    chat("§7[BaseProtect] §c${entity.name} near bed §7(${dist}m) at §f${bed.x} ${bed.y} ${bed.z}")
                    // Play configurable alert sound
                    val sound = alertSound.trim()
                    if (sound.isNotEmpty()) mc.thePlayer?.playSound(sound, alertVolume, 1.0f)
                    recentlyWarned[key] = now
                }
            }
        }
    }

    val onWorld = handler<WorldEvent> {
        inBedwars = false
        countdownSeen = false
        gameStarted = false
        bedPos = null
        recentlyWarned.clear()
    }

    // Detect BedWars start countdown via title and chat packets
    val onPacket = handler<PacketEvent> { event ->
        when (val p = event.packet) {
            is S45PacketTitle -> {
                val typeName = p.type?.name ?: ""
                // Accept TITLE and SUBTITLE to be safe across servers
                if (!(typeName.equals("TITLE", true) || typeName.equals("SUBTITLE", true))) return@handler
                val raw = p.message?.unformattedText?.trim() ?: return@handler
                val text = raw.uppercase()

                // Countdowns commonly show as "5", "4", ..., "1"
                if (text.matches(Regex(".*\b[5-1]\b.*"))) {
                    countdownSeen = true
                    return@handler
                }

                // Start indicators: "0", "GO!", "FIGHT!", sometimes "START!"
                if (text == "0" || text.contains("GO") || text.contains("FIGHT") || text.contains("START")) {
                    // If we observed countdown or direct start title, mark game started
                    gameStarted = true
                }
            }
            is S02PacketChat -> {
                val raw = p.chatComponent?.unformattedText?.trim() ?: return@handler
                val lower = raw.lowercase()

                // Pattern: "The game starts in X seconds!"
                val m = Regex("\\b(the\\s+game|game)\\s+starts?\\s+in\\s+(\\d+)\\s+second").find(lower)
                if (m != null) {
                    countdownSeen = true
                    val secs = m.groupValues.getOrNull(2)?.toIntOrNull()
                    if (secs != null && secs <= 1) {
                        gameStarted = true
                    }
                    return@handler
                }

                // Some servers print bare countdown numbers in chat (10..1)
                if (countdownSeen && lower.matches(Regex("^\\d{1,2}$"))) {
                    val n = lower.toIntOrNull() ?: return@handler
                    if (n <= 1) gameStarted = true
                }
            }
        }
    }

    // Exposed for HUD element
    fun isInBedwars(): Boolean = inBedwars
    fun getBedPos(): BlockPos? = bedPos
    fun getAlertRangeBlocks(): Int = alertRange

    fun nearestEnemyDistanceToBed(): Int? {
        val world = mc.theWorld ?: return null
        val player = mc.thePlayer ?: return null
        val bed = bedPos ?: return null
        if (!inBedwars) return null

        var best: Double? = null
        for (entity in world.playerEntities) {
            if (entity == player) continue
            if (AntiBot.isBot(entity)) continue
            if (entity is EntityPlayer && isTeammate(entity)) continue
            val d = distanceToBedSq(entity.posX, entity.posY, entity.posZ)
            if (best == null || d < best!!) best = d
        }
        return best?.let { kotlin.math.sqrt(it).roundToInt() }
    }
}
