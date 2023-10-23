dependencies {
    compileOnly(project(":shared"))
    compileOnly(project(":services:application:api"))
    compileOnly(project(":services:replications:replication-manager"))
    compileOnly(project(":services:games:game-manager"))
    compileOnly(project(":services:games:game-models"))

    implementation("net.md-5:bungeecord-chat:1.20-R0.1")
}
