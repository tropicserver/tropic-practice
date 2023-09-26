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
    "services:replications:manager",
    "services:replications:api",
    "services:application:api",
    "services:games:manager",
    "services:games:api",
    "services:games:models",
)
