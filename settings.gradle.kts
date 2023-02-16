enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/")
    }
}
rootProject.name = "bending"

setupSubproject("bending-api", "api")
setupSubproject("bending-common", "common")
setupSubproject("bending-nms", "nms")
setupSubproject("bending-fabric", "fabric")
setupSubproject("bending-paper", "paper")
setupSubproject("bending-sponge", "sponge")
file("paper/adapters").listFiles { _, name -> name.startsWith("adapter-") }?.forEach {
    setupSubproject(it.name, it.path)
}
//include("code-generator")

fun setupSubproject(name: String, dir: String) {
    include(name)
    project(":$name").projectDir = file(dir)
}
