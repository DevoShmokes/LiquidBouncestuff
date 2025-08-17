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
import org.lwjgl.glfw.GLFW

object ClickingRecorder : ModuleSampleRecorder.DebugRecorderMode<ClickingSample>("Clicking") {

    @Suppress("unused")
    private val mouseHandler = handler<MouseButtonEvent> { event ->
        if (event.button != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return@handler
        }


    }

}
