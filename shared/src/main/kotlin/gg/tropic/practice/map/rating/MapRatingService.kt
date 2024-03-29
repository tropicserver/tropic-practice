package gg.tropic.practice.map.rating

import com.mongodb.client.model.Filters
import gg.scala.flavor.service.Configure
import gg.scala.flavor.service.Service
import gg.scala.store.controller.DataStoreObjectControllerCache
import gg.tropic.practice.map.Map
import gg.tropic.practice.map.MapService
import net.evilblock.cubed.util.bukkit.Tasks
import org.bson.Document

/**
 * Class created on 1/12/2024

 * @author 98ping
 * @project tropic-practice
 * @website https://solo.to/redis
 */
@Service
object MapRatingService
{
    val ratingMap: MutableMap<String, Double> = mutableMapOf()

    @Configure
    fun configure()
    {
        Tasks.asyncTimer(0L, 60 * 20L) {
            MapService.cached().maps.values.forEach {
                ratingMap[it.name] = loadAverageRating(it)
            }
        }
    }

    fun loadAverageRating(map: Map) = DataStoreObjectControllerCache
        .findNotNull<MapRating>()
        .mongo()
        .aggregate(
            listOf(
                Document(
                    "\$match",
                    Document(
                        "mapID", map.name
                    )
                ),
                Document(
                    "\$group",
                    Document(
                        mapOf(
                            "_id" to "\$_id",
                            "average" to Document(
                                "\$avg", "\$rating"
                            )
                        )
                    )
                )
            )
        )
        .first()
        ?.getDouble("average")
        ?: 0.0

    fun getRatingCount(map: Map) = DataStoreObjectControllerCache
        .findNotNull<MapRating>()
        .mongo()
        .loadWithFilter(
            Filters.eq("mapID", map.name)
        )

    fun create(rating: MapRating) = DataStoreObjectControllerCache
        .findNotNull<MapRating>()
        .save(rating)
}
