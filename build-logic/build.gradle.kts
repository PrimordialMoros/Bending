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
    implementation(libs.minotaur)
    implementation(libs.checker)
}

kotlin {
    jvmToolchain(17)
}
