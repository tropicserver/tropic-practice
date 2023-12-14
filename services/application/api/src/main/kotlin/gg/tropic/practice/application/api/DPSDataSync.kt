package gg.tropic.practice.application.api

import com.mongodb.client.MongoCollection
import com.mongodb.client.model.Filters
import com.mongodb.client.model.UpdateOptions
import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import gg.scala.store.ScalaDataStoreShared
import gg.scala.store.spigot.ScalaDataStoreSpigotImpl
import net.evilblock.cubed.serializers.Serializers
import org.bson.Document
import java.util.concurrent.CompletableFuture
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 2/10/2023
 */
abstract class DPSDataSync<T>
{
    private var cached: T? = null
    private val cache = DPSRedisShared.keyValueCache

    private var collection: MongoCollection<Document>? = null

    open fun locatedIn(): DPSDataSyncSource
    {
        return DPSDataSyncSource.Redis
    }

    abstract fun keys(): DPSDataSyncKeys
    abstract fun type(): Class<T>

    fun cached() = cached
        ?: throw IllegalStateException(
            "DataSyncService did not load facets for ${type().name}!"
        )

    private val backingSync by lazy {
        // this is the channel which commons is
        // listening on apparently?
        DPSRedisService("UuidCache", raw = true)
            .apply {
                start()
            }
            .configure { this }
    }

    fun sync() = backingSync
    open fun cache() = cache

    open fun cleanup()
    {

    }

    private var initialReloadComplete = false

    open fun postReload()
    {
        if (initialReloadComplete)
        {
            Logger.getGlobal().info(
                "[datasync] reloaded ${
                    type().name
                } facets for ${
                    javaClass.name
                }"
            )
        }

        initialReloadComplete = true
    }

    fun load()
    {
        if (locatedIn() == DPSDataSyncSource.Mongo)
        {
            val resource = ScalaDataStoreShared.INSTANCE
                .getNewMongoConnection()
                .getAppliedResource()

            collection = resource.getCollection("DataSync")
        }

        sync()
            .listen(
                keys().sync().asString()
            ) {
                reload()
            }

        reload()
        Logger.getGlobal().info(
            "[datasync] loaded service ${
                javaClass.name
            }, listening on key ${
                keys().sync().asString()
            }"
        )
    }

    private fun pullModelFromSource(): String?
    {
        if (collection != null)
        {
            return collection!!
                .find(Filters.eq("uid", keys().newStore()))
                .first()?.toJson()
        }

        return cache().sync()
            .get(
                keys().newStore()
            )
    }

    private fun pushNewModelToSource(model: T)
    {
        if (collection != null)
        {
            val tree = Serializers.gson.toJsonTree(model)
            tree.asJsonObject.addProperty(
                "uid",
                keys().newStore()
            )

            collection!!.updateOne(
                Filters.eq("uid", keys().newStore()),
                Document(
                    "\$set",
                    Document.parse(
                        Serializers.gson.toJson(tree)
                    )
                ),
                UpdateOptions().upsert(true)
            )
        }

        cache().sync()
            .set(
                keys().newStore(),
                Serializers.gson.toJson(cached)
            )
    }

    internal fun reload()
    {
        val model = pullModelFromSource()

        if (model == null)
        {
            cached = type()
                .getDeclaredConstructor()
                .newInstance()

            pushNewModelToSource(cached!!)
            pushKvUpdate()
            return
        }

        kotlin.runCatching {
            cleanup()
        }.onFailure {
            it.printStackTrace()
        }

        this.cached = Serializers.gson
            .fromJson(
                model, type()
            )
            ?: type()
                .getDeclaredConstructor()
                .newInstance()

        kotlin.runCatching {
            postReload()
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun sync(newModel: T): CompletableFuture<Void>
    {
        cached = newModel

        return CompletableFuture.runAsync {
            pushNewModelToSource(newModel)
            pushKvUpdate()
        }
    }

    private fun pushKvUpdate()
    {
        AwareMessage
            .of(
                keys().sync().asString(),
                sync()
            )
            .publish(
                AwareThreadContext.SYNC
            )
    }
}
