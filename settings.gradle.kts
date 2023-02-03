pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }
}
rootProject.name = "bending"

include("api")
include("common")
include("nms")
file("paper/adapters").listFiles { _, name -> name.startsWith("adapter-") }?.forEach {
    include("paper:adapters:${it.name}")
}
include("fabric")
include("paper")
include("sponge")
//include("code-generator")
