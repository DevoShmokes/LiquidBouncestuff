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

package net.ccbluex.liquidbounce.deeplearn.data

import net.ccbluex.liquidbounce.deeplearn.data.sample.Sample

@Suppress("ArrayInDataClass")
data class DataSet(val features: Array<FloatArray>, val labels: Array<FloatArray>) {

    companion object {

        /**
         * Creates a DataSet from an array of samples.
         */
        fun <T : Sample<T>> fromSamples(samples: List<T>): DataSet {
            require(samples.isNotEmpty()) { "Samples collection must not be empty." }

            val sample = samples.first()
            val features = Array(samples.size) { FloatArray(sample.inputSize) }
            val labels = Array(samples.size) { FloatArray(sample.outputSize) }

            for (i in samples.indices) {
                val sample = samples[i]
                val previousSample = if (i > 0) samples[i - 1] else null
                features[i] = sample.toInput(previousSample)
                labels[i] = sample.toOutput(previousSample)
            }

            return DataSet(features, labels)
        }

    }

}
