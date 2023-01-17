pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.spongepowered.org/repository/maven-public/")
    }
}
rootProject.name = "bending"

include("bending-api")
include("bending-common")
file("bending-paper/adapters").listFiles { _, name -> name.startsWith("adapter-") }?.forEach {
    include("bending-paper:adapters:${it.name}")
}
include("bending-fabric")
include("bending-paper")
include("bending-sponge")
//include("code-generator")
