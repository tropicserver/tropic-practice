repositories {
    maven("https://repo.glaremasters.me/repository/concuncan/")
}

dependencies {
    api(project(":shared"))
    compileOnly("com.grinderwolf:slimeworldmanager-plugin:2.2.1")
    compileOnly("com.grinderwolf:slimeworldmanager-api:2.2.1")
}
