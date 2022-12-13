pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}
rootProject.name = "bending"

include("bending-api")
file("bending-paper/adapters").listFiles { _, name -> name.startsWith("adapter-") }?.forEach {
    include("bending-paper:adapters:${it.name}")
}
include("bending-paper")
