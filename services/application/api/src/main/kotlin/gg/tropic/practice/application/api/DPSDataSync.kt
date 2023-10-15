package gg.tropic.practice.application.api

import gg.scala.aware.message.AwareMessage
import gg.scala.aware.thread.AwareThreadContext
import net.evilblock.cubed.serializers.Serializers
import java.util.logging.Logger

/**
 * @author GrowlyX
 * @since 2/10/2023
 */
abstract class DPSDataSync<T>
{
    private var cached: T? = null
    private val cache = DPSRedisShared.keyValueCache

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

    internal fun reload()
    {
        val model = cache().sync()
            .get(
                keys().store().asString()
            )

        if (model == null)
        {
            cached = type()
                .getDeclaredConstructor()
                .newInstance()

            cache().sync()
                .set(
                    keys().store().asString(),
                    Serializers.gson.toJson(cached)
                )

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

    fun sync(
        newModel: T
    )
    {
        cached = newModel

        cache().sync()
            .set(
                keys().store().asString(),
                Serializers.gson.toJson(cached)
            )

        pushKvUpdate()
    }

    private fun pushKvUpdate()
    {
        AwareMessage
            .of(
                keys().sync().asString(),
                sync()
            )
            .publish(
                AwareThreadContext.ASYNC
            )
    }
}
