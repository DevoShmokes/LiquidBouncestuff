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

package net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.modes.clicking

import net.ccbluex.liquidbounce.deeplearn.data.sample.ClickingSample
import net.ccbluex.liquidbounce.event.events.MouseButtonEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.debugrecorder.ModuleSampleRecorder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import org.lwjgl.glfw.GLFW

object ClickingRecorder : ModuleSampleRecorder.RecorderMode<ClickingSample>("Clicking") {

    private var startTime = 0L

    @Suppress("unused")
    private val mouseHandler = handler<MouseButtonEvent> { event ->
        if (event.button != GLFW.GLFW_MOUSE_BUTTON_LEFT || event.action != GLFW.GLFW_PRESS) {
            return@handler
        }

        var elapsed = System.currentTimeMillis() - startTime
        val lastClick = getLastSample()

        // If the last click was more than 1 second ago,
        // reset the start time
        if (lastClick == null || elapsed - lastClick.elapsed > 1000) {
            startTime = System.currentTimeMillis()
            elapsed = 0L
            chat(regular("Starting new clicking sample cycle."))
        }

        val sample = ClickingSample(elapsed)
        recordSample(sample)
        chat(regular("Recorded clicking sample: ${sample.elapsed}ms"))
    }

}
