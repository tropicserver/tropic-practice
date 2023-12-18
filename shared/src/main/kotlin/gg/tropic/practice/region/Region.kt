package gg.tropic.practice.region

/**
 * @author GrowlyX
 * @since 12/17/2023
 */
enum class Region
{
    NA, EU, Both;

    fun extractFrom(id: String) = when (true)
    {
        id.startsWith("na") -> NA
        id.startsWith("eu") -> EU
        else -> Both
    }
}
