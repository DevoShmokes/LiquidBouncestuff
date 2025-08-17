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

package net.ccbluex.liquidbounce.deeplearn.data.sample

import net.ccbluex.liquidbounce.config.gson.util.decode
import java.io.File

data class ClickingSample(
    val elapsed: Long
) : Sample<ClickingSample> {

    override fun toInput(previous: ClickingSample) = floatArrayOf(
        elapsed.normalize()
    )

    override fun toOutput(previous: ClickingSample) = floatArrayOf(
        (elapsed - previous.elapsed).normalize()
    )

    private fun Long.normalize() = this / 1000f

    companion object {

        private fun parse(file: File): List<ClickingSample> = when {
            file.isDirectory -> file.listFiles().flatMap(::parse)
            file.extension == "json" -> decode<List<ClickingSample>>(file.inputStream())
            else -> emptyList()
        }

        fun parse(vararg files: File): List<ClickingSample> = files.flatMap(::parse)

    }
}
