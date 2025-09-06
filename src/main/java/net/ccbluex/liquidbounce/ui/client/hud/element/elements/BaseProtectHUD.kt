package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.misc.BaseProtect
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.client.hud.element.Side
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import java.awt.Color

@ElementInfo(name = "BaseProtect")
class BaseProtectHUD(
    x: Double = 6.0, y: Double = 80.0, scale: Float = 1F,
    side: Side = Side.default()
) : Element("BaseProtect", x, y, scale, side) {

    private val showCoords by boolean("ShowBedCoords", true)
    private val showNearest by boolean("ShowNearest", true)
    private val showStatus by boolean("ShowStatus", true)

    private val bgColor by color("Background", Color(0, 0, 0, 90))
    private val borderColor by color("Border", Color(0, 0, 0, 150))
    private val textColor by color("Text", Color.WHITE)

    override fun drawElement(): Border {
        val fr = mc.fontRendererObj

        val inBW = BaseProtect.isInBedwars()
        val bed = BaseProtect.getBedPos()
        val range = BaseProtect.getAlertRangeBlocks()
        val nearest = BaseProtect.nearestEnemyDistanceToBed()

        val title = "Base Protect"
        val status = when {
            !inBW -> "Status: Lobby"
            bed == null -> "Status: Searching bed..."
            nearest == null -> "Status: Safe"
            else -> if (nearest <= range) "Status: THREAT ${nearest}m" else "Nearest: ${nearest}m"
        }

        val bedLine = if (showCoords) {
            if (bed != null) "Bed: ${bed.x} ${bed.y} ${bed.z}" else "Bed: -"
        } else ""

        val nextLine = if (showNearest && nearest != null) "Nearest: ${nearest}m" else ""

        val lines = mutableListOf(title)
        if (showStatus) lines += status
        if (bedLine.isNotEmpty()) lines += bedLine
        if (nextLine.isNotEmpty() && status.startsWith("Status")) lines += nextLine

        val width = lines.maxOf { fr.getStringWidth(it) } + 8
        val height = 4 + lines.size * (fr.FONT_HEIGHT + 2)

        val w = width.toFloat()
        val h = height.toFloat()
        // Background
        drawRect(0f, 0f, w, h, bgColor.rgb)
        // Border (1px)
        drawRect(0f, 0f, w, 1f, borderColor.rgb)           // top
        drawRect(0f, h - 1f, w, h, borderColor.rgb)        // bottom
        drawRect(0f, 0f, 1f, h, borderColor.rgb)           // left
        drawRect(w - 1f, 0f, w, h, borderColor.rgb)        // right

        var yOff = 4
        for (line in lines) {
            fr.drawString(line, 4, yOff, textColor.rgb)
            yOff += fr.FONT_HEIGHT + 2
        }

        return Border(0f, 0f, width.toFloat(), height.toFloat())
    }
}
