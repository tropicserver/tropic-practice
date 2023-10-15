dependencies {
    implementation(project(":services:games:game-models"))
    implementation(project(":services:replications:replication-models"))
    api(project(":services:statistics"))
    api("joda-time:joda-time:2.12.5")
}
