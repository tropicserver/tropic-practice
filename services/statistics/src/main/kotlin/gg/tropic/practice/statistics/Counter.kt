package gg.tropic.practice.statistics

import org.bukkit.Bukkit
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * @author GrowlyX
 * @since 12/21/2023
 */
data class Counter(
    private val owner: UUID,
    private val mappings: ConcurrentHashMap<String, Double> = ConcurrentHashMap()
)
{
    fun update(id: String, value: Int) = mappings.put(id, value.toDouble())
    fun update(id: String, value: Double) = mappings.put(id, value)

    fun reset(id: String)
    {
        mappings[id] = 0.0
    }

    fun increment(id: String, amount: Double = 1.0)
    {
        mappings.compute(id) { _, value ->
            val old = (value ?: 0.0)
            Bukkit.getPlayer(owner)?.apply {
                sendMessage("$id: $old -> ${old + amount}")
            }
            old + amount
        }
    }

    fun valueOf(id: String) = mappings.getOrPut(id) { 0.0 }
}
