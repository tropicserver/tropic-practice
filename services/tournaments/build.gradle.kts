dependencies {
    compileOnly(project(":services:games:game-manager"))
    compileOnly(project(":services:games:game-models"))
    compileOnly(project(":services:application:api"))
    compileOnly(project(":shared"))

    api("com.tinder.statemachine:statemachine:0.2.0")
}
