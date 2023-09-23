package gg.tropic.practice.kit.group

/**
 * @author GrowlyX
 * @since 9/17/2023
 */
class KitGroupContainer
{
    companion object
    {
        const val DEFAULT = "__default__"
    }

    private val backingGroups: MutableList<KitGroup> =
        mutableListOf(
            KitGroup(id = DEFAULT)
        )

    val groups: List<KitGroup>
        get() = backingGroups

    fun add(group: KitGroup)
    {
        if (group.id == DEFAULT)
        {
            throw IllegalStateException("stop")
        }

        backingGroups += group
    }

    fun remove(group: KitGroup)
    {
        if (group.id == DEFAULT)
        {
            throw IllegalStateException("stop")
        }

        backingGroups -= group
    }

    fun default() = groups.first { it.id == DEFAULT }
}
