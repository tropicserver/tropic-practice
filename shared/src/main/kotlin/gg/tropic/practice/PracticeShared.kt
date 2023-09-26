package gg.tropic.practice

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import gg.tropic.practice.map.metadata.AbstractMapMetadata
import net.evilblock.cubed.serializers.Serializers
import net.evilblock.cubed.serializers.impl.AbstractTypeSerializer
import org.bukkit.potion.PotionEffectType

/**
 * @author GrowlyX
 * @since 8/5/2022
 */
object PracticeShared
{
    const val KEY = "tropicpractice"

    // i don't like this, but we need to do it
    init
    {
        Serializers.create {
            registerTypeAdapter(
                AbstractMapMetadata::class.java,
                AbstractTypeSerializer<AbstractMapMetadata>()
            )

            registerTypeAdapter(
                PotionEffectType::class.java,
                object : TypeAdapter<PotionEffectType>()
                {
                    override fun write(out: JsonWriter?, value: PotionEffectType?)
                    {
                        out?.value(value?.name)
                    }

                    override fun read(`in`: JsonReader?): PotionEffectType?
                    {
                        return PotionEffectType.getByName(`in`?.nextString())
                    }
                }
            )
        }
    }
}
