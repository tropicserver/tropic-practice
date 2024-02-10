package gg.tropic.practice.configuration

import gg.scala.commons.persist.datasync.DataSyncKeys
import gg.scala.commons.persist.datasync.DataSyncService
import gg.scala.commons.persist.datasync.DataSyncSource
import gg.scala.flavor.service.Service
import gg.tropic.practice.namespace
import gg.tropic.practice.suffixWhenDev
import net.kyori.adventure.key.Key

/**
 * @author GrowlyX
 * @since 10/13/2023
 */
@Service
object PracticeConfigurationService : DataSyncService<PracticeConfiguration>()
{
    object LobbyConfigurationKeys : DataSyncKeys
    {
        override fun newStore() = "mi-practice-lobbyconfig"

        override fun store() = Key.key(namespace(), "lobbyconf")
        override fun sync() = Key.key(namespace().suffixWhenDev(), "lcsync")
    }

    override fun locatedIn() = DataSyncSource.Mongo

    override fun keys() = LobbyConfigurationKeys
    override fun type() = PracticeConfiguration::class.java
}
