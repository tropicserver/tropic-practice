dependencies {
    compileOnly(project(":services:games:game-manager"))
    compileOnly(project(":services:application:api"))
    compileOnly(project(":services:queue"))
    compileOnly(project(":shared"))

    api("com.tinder.statemachine:statemachine:0.2.0")
}
