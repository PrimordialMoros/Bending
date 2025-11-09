plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
    maven("https://maven.neoforged.net/releases/")
}

dependencies {
    implementation(libs.shadow)
    implementation(libs.maven.publish)
    implementation(libs.moddevgradle)
    implementation(libs.minotaur)
}

kotlin {
    jvmToolchain(21)
}
