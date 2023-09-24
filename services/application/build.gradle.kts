plugins {
    application
}

dependencies {
    implementation(kotlin("stdlib"))

    implementation(project(":shared"))
    implementation(project(":services:queue"))
    implementation(project(":services:statistics"))
    implementation(project(":services:application:api"))
    implementation(project(":services:statistics:leaderboards"))
    implementation(project(":services:replications:manager"))

    implementation("gg.scala.commons:store:3.2.2")
    implementation("gg.scala.commons:serversync:3.2.2")
    implementation("gg.scala.commons:serializers:3.2.2")

    implementation("com.xenomachina:kotlin-argparser:2.0.7")
    implementation("gg.scala.store:shared:0.1.8")

    implementation("com.google.code.gson:gson:2.10.1")
    implementation("gg.scala.aware:aware:1.1.9")
    implementation("io.lettuce:lettuce-core:6.2.6.RELEASE")
}

application {
    mainClass.set("gg.tropic.practice.application.ApplicationServerKt")
}
