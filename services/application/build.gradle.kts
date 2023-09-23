plugins {
    application
}

dependencies {
    api(project(":shared"))
    api(project(":services:queue"))
    api(project(":services:statistics"))
    api(project(":services:statistics:leaderboards"))
}

application {
    mainClass.set("gg.tropic.practice.application.ApplicationServerKt")
}
