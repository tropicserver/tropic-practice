dependencies {
    compileOnly(project(":services:api"))
    compileOnly(project(":services:games:game-models"))
    compileOnly(project(":services:replications:replication-models"))
    api(project(":services:statistics"))
    api("joda-time:joda-time:2.12.5")
}
