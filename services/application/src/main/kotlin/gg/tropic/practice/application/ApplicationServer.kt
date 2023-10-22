package gg.tropic.practice.application

import com.mongodb.MongoClient
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import com.xenomachina.argparser.mainBody
import gg.scala.aware.AwareHub
import gg.scala.aware.uri.WrappedAwareUri
import gg.scala.cache.uuid.ScalaStoreUuidCache
import gg.scala.cache.uuid.impl.distribution.DistributedRedisUuidCacheTranslator
import gg.scala.cache.uuid.resolver.impl.MojangDataResolver
import gg.scala.commons.agnostic.sync.ServerSync
import gg.scala.store.ScalaDataStoreShared
import gg.scala.store.connection.AbstractDataStoreConnection
import gg.scala.store.connection.mongo.AbstractDataStoreMongoConnection
import gg.scala.store.connection.mongo.impl.UriDataStoreMongoConnection
import gg.scala.store.connection.mongo.impl.details.DataStoreMongoConnectionDetails
import gg.scala.store.connection.redis.AbstractDataStoreRedisConnection
import gg.tropic.practice.application.api.DPSRedisService
import gg.tropic.practice.application.api.DPSRedisShared
import gg.tropic.practice.application.api.defaults.kit.KitDataSync
import gg.tropic.practice.application.api.defaults.kit.group.KitGroupDataSync
import gg.tropic.practice.application.api.defaults.map.MapDataSync
import gg.tropic.practice.games.manager.GameManager
import gg.tropic.practice.queue.GameQueueManager
import gg.tropic.practice.replications.manager.ReplicationManager
import net.evilblock.cubed.serializers.Serializers

class ApplicationServerArgs(parser: ArgParser)
{
    val redisHost by parser
        .storing(
            "--redishost",
            help = "The host of the Redis server"
        )
        .default("127.0.0.1")

    val redisPort by parser
        .storing(
            "--redisport",
            help = "The port of the Redis server"
        ) {
            toInt()
        }
        .default(6379)

    val mongoHost by parser
        .storing(
            "--mongohost",
            help = "The host of the Mongo server"
        )
        .default("127.0.0.1")

    val mongoDatabase by parser
        .storing(
            "--mongodatabase",
            help = "The database for the Mongo server"
        )
        .default("Scala")

    val mongoPort by parser
        .storing(
            "--mongoport",
            help = "The port of the Mongo server"
        ) {
            toInt()
        }
        .default(27017)
}

/**
 * @author GrowlyX
 * @since 9/23/2023
 */
fun main(args: Array<String>) = mainBody {
    val parsedArgs = ArgParser(args)
        .parseInto {
            ApplicationServerArgs(it)
        }

    AwareHub.configure(
        WrappedAwareUri(
            parsedArgs.redisHost,
            parsedArgs.redisPort
        )
    ) {
        Serializers.gson
    }

    DPSRedisShared.keyValueCache
    ServerSync.configureIndependent()

    val aware = DPSRedisService("dps")
        .configure { this }

    ScalaDataStoreShared.INSTANCE =
        object : ScalaDataStoreShared()
        {
            override fun getNewRedisConnection(): AbstractDataStoreRedisConnection
            {
                return object : AbstractDataStoreRedisConnection()
                {
                    override fun createNewConnection() = aware
                }
            }

            override fun getNewMongoConnection(): AbstractDataStoreMongoConnection
            {
                return UriDataStoreMongoConnection(
                    DataStoreMongoConnectionDetails(
                        database = parsedArgs.mongoDatabase
                    ),
                    MongoClient(parsedArgs.mongoHost, parsedArgs.mongoPort)
                )
            }

            override fun debug(from: String, message: String)
            {
                AbstractDataStoreConnection.LOGGER.info("$from: $message")
            }

            override fun forceDisableRedisThreshold() = true
        }


    ScalaStoreUuidCache.configure(
        DistributedRedisUuidCacheTranslator(aware),
        MojangDataResolver
    )

    MapDataSync.load()
    KitDataSync.load()
    KitGroupDataSync.load()

    GameManager.load()
    GameQueueManager.load()
    ReplicationManager.load()

    while (true)
    {
        Thread.sleep(Long.MAX_VALUE)
    }
}
