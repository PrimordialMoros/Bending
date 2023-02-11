plugins {
    `kotlin-dsl`
}

configurations.all {
    resolutionStrategy {
        force("org.ow2.asm:asm:9.4")
        force("org.ow2.asm:asm-commons:9.4")
    }
}

repositories {
    gradlePluginPortal()
    maven("https://repo.spongepowered.org/repository/maven-public/")
}

dependencies {
    implementation(libs.shadow)
    implementation(libs.vanilla)
}
