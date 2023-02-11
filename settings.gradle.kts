enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }
}
rootProject.name = "bending"

setupSubproject("api")
setupSubproject("common")
setupSubproject("nms")
setupSubproject("fabric")
setupSubproject("paper")
setupSubproject("sponge")
file("paper/adapters").listFiles { _, name -> name.startsWith("adapter-") }?.forEach {
    include("bending-paper:adapters:${it.name}")
}
//include("code-generator")

fun setupSubproject(name: String) {
    val moduleName = "${rootProject.name}-$name"
    include(moduleName)
    project(":$moduleName").projectDir = file(name)
}
