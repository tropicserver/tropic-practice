package gg.tropic.practice.map.rating

import gg.scala.commons.annotations.Model
import gg.scala.store.controller.annotations.Indexed
import gg.scala.store.storage.storable.IDataStoreObject
import java.util.*

/**
 * Class created on 1/12/2024

 * @author 98ping
 * @project tropic-practice
 * @website https://solo.to/redis
 */
@Model
data class MapRating(
    override val identifier: UUID,
    @Indexed val rating: Int,
    @Indexed val rater: UUID,
    @Indexed val mapID: String,
) : IDataStoreObject