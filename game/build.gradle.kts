repositories {
    maven("https://repo.glaremasters.me/repository/concuncan/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    api(project(":shared"))
    api(project(":services:statistics"))
    api(project(":services:api"))
    api(project(":services:games:game-models"))
    api(project(":services:replications:replication-models"))

    compileOnly("com.grinderwolf:slimeworldmanager-plugin:2.2.1")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")
    compileOnly(
        "gg.tropic.game.extensions:tropic-core-game-extensions:1.1.1"
    )
}
