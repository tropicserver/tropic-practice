package gg.tropic.practice

/**
 * @author GrowlyX
 * @since 2/10/2024
 */
var devProvider: () -> Boolean = { false }

fun isDev() = devProvider()
fun isProd() = !devProvider()

fun namespace() = "tropicpractice"

fun practiceGroup() = "mip"
fun gameGroup() = "mipgame"
fun lobbyGroup() = "miplobby"

fun String.suffixWhenDev() = (if (isDev()) "${if (this == "tropicpractice")
    "tropicprac" else this}DEV" else this)
