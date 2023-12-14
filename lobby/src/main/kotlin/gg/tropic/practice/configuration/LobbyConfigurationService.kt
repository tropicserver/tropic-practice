package gg.tropic.practice.configuration

import gg.scala.commons.persist.datasync.DataSyncKeys
import gg.scala.commons.persist.datasync.DataSyncService
import gg.scala.commons.persist.datasync.DataSyncSource
import gg.scala.flavor.service.Service
import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 10/13/2023
 */
@Service
object LobbyConfigurationService : DataSyncService<LobbyConfiguration>()
{
    object LobbyConfigurationKeys : DataSyncKeys
    {
        override fun newStore() = "mi-practice-lobbyconfig"

        override fun store() = Key.key("tropicpractice", "lobbyconf")
        override fun sync() = Key.key("tropicpractice", "lcsync")
    }

    override fun locatedIn() = DataSyncSource.Mongo

    override fun keys() = LobbyConfigurationKeys
    override fun type() = LobbyConfiguration::class.java
}
