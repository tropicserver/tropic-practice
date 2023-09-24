dependencies {
    compileOnly(project(":shared"))

    project.subprojects.forEach {
        println(it.name)
    }
    compileOnly(project(":services:application:api"))
    compileOnly(project(":services:replications:api"))
}
