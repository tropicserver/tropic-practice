package gg.tropic.practice.reports.menu.utility

import java.util.*

/**
 * https://stackoverflow.com/questions/12967896/converting-integers-to-roman-numerals-java
 */
object RomanNumerals
{
    private val mappings = TreeMap<Int, String>()

    init
    {
        mappings[1000] = "M"
        mappings[900] = "CM"
        mappings[500] = "D"
        mappings[400] = "CD"
        mappings[100] = "C"
        mappings[90] = "XC"
        mappings[50] = "L"
        mappings[40] = "XL"
        mappings[10] = "X"
        mappings[9] = "IX"
        mappings[5] = "V"
        mappings[4] = "IV"
        mappings[1] = "I"
    }

    fun toRoman(number: Int): String
    {
        val key = mappings.floorKey(number)

        return if (number == key)
        {
            mappings[number]!!
        } else
        {
            mappings[key] + toRoman(number - key)
        }
    }
}
