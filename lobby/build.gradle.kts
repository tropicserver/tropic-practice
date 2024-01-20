dependencies {
    api(project(":shared"))
    api(project(":services:statistics"))

    compileOnly("gg.scala.cgs:parties:1.4.3")
    api(project(":services:replications:replication-models"))
}
