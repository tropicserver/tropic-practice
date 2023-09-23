package gg.tropic.practice.map.metadata

import net.evilblock.cubed.serializers.impl.AbstractTypeSerializable

/**
 * @author GrowlyX
 * @since 11/10/2022
 */
abstract class AbstractMapMetadata : AbstractTypeSerializable
{
    abstract val id: String
}

