enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://repo.spongepowered.org/repository/maven-public/")
        maven("https://maven.fabricmc.net/")
    }
    includeBuild("build-logic")
}
rootProject.name = "bending"

setupSubproject("bending-api", "api")
setupSubproject("bending-common", "common")
setupSubproject("bending-nms", "nms")
setupSubproject("bending-fabric", "fabric")
setupSubproject("bending-paper", "paper")
//setupSubproject("bending-sponge", "sponge")
include("jmh")
//include("code-generator")

fun setupSubproject(name: String, dir: String) {
    include(name)
    project(":$name").projectDir = file(dir)
}
