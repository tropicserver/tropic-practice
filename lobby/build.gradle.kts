dependencies {
    api(project(":shared"))
    api(project(":services:queue"))
    api(project(":services:statistics"))

    compileOnly(
        "gg.tropic.game.extensions:tropic-core-game-extensions:1.1.0"
    )
}
