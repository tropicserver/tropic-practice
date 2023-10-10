pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

rootProject.name = "tropic-practice"
include(
    "shared", "game", "lobby", "devtools",
    "services:application", "services:queue", "services:statistics",
    "services:statistics:leaderboards",
    "services:api",
    "services:application:api",
    "services:replications:replication-manager",
    "services:replications:replication-models",
    "services:games:game-manager",
    "services:games:game-models"
)
