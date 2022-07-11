pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}
rootProject.name = "bending"

listOf("1.18.2", "1.19").forEach {
    include("bending-paper:adapters:adapter-$it")
}
include("bending-api")
include("bending-paper")
