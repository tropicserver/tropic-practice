dependencies {
    compileOnly(project(":shared"))

    compileOnly(project(":services:application:api"))
    implementation(project(":services:games:models"))
}
