plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    implementation(libs.shadow)
    implementation(libs.vanilla)
}
