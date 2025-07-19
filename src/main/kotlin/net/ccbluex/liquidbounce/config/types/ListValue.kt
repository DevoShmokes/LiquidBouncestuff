package net.ccbluex.liquidbounce.config.types

import com.google.gson.Gson
import com.google.gson.JsonElement
import net.ccbluex.liquidbounce.config.gson.stategies.Exclude
import net.ccbluex.liquidbounce.config.gson.stategies.ProtocolExclude
import java.util.HashSet
import java.util.TreeSet

open class ListValue<T : MutableCollection<E>, E>(
    name: String,
    /**
     * Enabled values. A mutable and unordered [Set].
     */
    value: T,

    /**
     * Used to determine the type of the inner value.
     */
    @Exclude val innerValueType: ValueType = ValueType.INVALID,

    /**
     * Used to deserialize the [value] from JSON.
     * TODO: Might replace [innerType] with a [Class] variable
     *   from the inner value type in the future.
     */
    @Exclude @ProtocolExclude val innerType: Class<E>,

) : Value<T>(
    name,
    defaultValue = value,
    valueType = ValueType.LIST,
) {

    init {
        require(value is List<*> || value is HashSet<*> || value is Set<*>) {
            "Inner value must be a List, HashSet or Set, but was ${value::class.java.name}"
        }
    }

    override fun deserializeFrom(gson: Gson, element: JsonElement) {
        val currValue = this.inner

        set(when (currValue) {
            is List<*> -> {
                element.asJsonArray.mapTo(
                    mutableListOf()
                ) { gson.fromJson(it, this.innerType) } as T
            }

            is HashSet<*> -> {
                element.asJsonArray.mapTo(
                    HashSet()
                ) { gson.fromJson(it, this.innerType) } as T
            }

            is Set<*> -> {
                element.asJsonArray.mapTo(
                    TreeSet()
                ) { gson.fromJson(it, this.innerType) } as T
            }

            else -> error("Unsupported collection type: ${currValue::class.java.name}")
        })
    }

}
