plugins {
    application
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation(project(":services:games:game-manager"))
    implementation(project(":services:games:game-models"))
    implementation(project(":services:replications:replication-manager"))
    implementation(project(":services:replications:replication-models"))
    implementation(project(":shared"))
    implementation(project(":services:queue"))
    implementation(project(":services:statistics"))
    implementation(project(":services:application:api"))
    implementation(project(":services:statistics:leaderboards"))

    implementation("org.apache.logging.log4j:log4j-api:2.20.0")
    implementation("org.apache.logging.log4j:log4j-core:2.20.0")

    implementation("gg.scala.commons:store:3.2.2")
    implementation("gg.scala.commons:serversync:3.2.2")
    implementation("gg.scala.commons:serializers:3.2.2")

    implementation("net.kyori:adventure-key:4.11.0")
    implementation("net.kyori:adventure-text-serializer-gson:4.11.0")

    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")
    implementation("org.mongodb:mongo-java-driver:3.12.14")

    implementation("com.xenomachina:kotlin-argparser:2.0.7")
    implementation("gg.scala.store:shared:0.1.8")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("gg.scala.aware:aware:1.1.9")
    implementation("io.lettuce:lettuce-core:6.2.6.RELEASE")

    implementation("org.litote.kmongo:kmongo:4.11.0")
}

application {
    mainClass.set("gg.tropic.practice.application.ApplicationServerKt")
}
