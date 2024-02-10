package gg.tropic.practice.map

import gg.scala.commons.persist.datasync.DataSyncKeys
import gg.scala.commons.persist.datasync.DataSyncService
import gg.scala.commons.persist.datasync.DataSyncSource
import gg.scala.flavor.service.Service
import gg.tropic.practice.PracticeShared
import gg.tropic.practice.namespace
import gg.tropic.practice.suffixWhenDev
import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 9/21/2023
 */
@Service
object MapService : DataSyncService<MapContainer>()
{
    object MapKeys : DataSyncKeys
    {
        override fun newStore() = "mi-practice-maps"

        override fun store() = Key.key(namespace(), "maps")
        override fun sync() = Key.key(namespace().suffixWhenDev(), "msync")
    }

    override fun locatedIn() = DataSyncSource.Mongo

    override fun keys() = MapKeys
    override fun type() = MapContainer::class.java

    fun maps() = cached().maps.values

    fun mapWithID(id: String) = cached().maps.values
        .firstOrNull {
            it.name.equals(id, true)
        }

    var onPostReload = {}
    override fun postReload()
    {
        onPostReload()
    }
}
