repositories {
    maven("https://repo.glaremasters.me/repository/concuncan/")
    maven("https://repo.dmulloy2.net/repository/public/")
}

dependencies {
    api(project(":shared"))
    api(project(":services:api"))
    api(project(":services:games:game-models"))
    compileOnly("com.grinderwolf:slimeworldmanager-plugin:2.2.1")
}
