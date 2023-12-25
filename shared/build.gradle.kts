repositories {
    maven("https://repo.glaremasters.me/repository/concuncan/")
}

dependencies {
    implementation(project(":services:replications:replication-models"))
    api(project(":services:statistics"))

    api("joda-time:joda-time:2.12.5")
    api("xyz.xenondevs:particle:1.7.1")
}
