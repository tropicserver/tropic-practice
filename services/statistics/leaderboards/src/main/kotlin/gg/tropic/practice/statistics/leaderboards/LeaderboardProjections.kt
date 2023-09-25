package gg.tropic.practice.statistics.leaderboards

import org.bson.Document

/**
 * @author GrowlyX
 * @since 9/25/2023
 */
fun sortFieldDescending(field: String) = listOf(
    Document(
        "\$sort",
        Document(field, -1L)
    ),
    Document(
        "\$project",
        Document("_id", "\$_id")
            .append(
                "value",
                "\$$field"
            )
    )
)
