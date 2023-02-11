plugins {
    id("org.spongepowered.gradle.vanilla")
}

minecraft {
    version(libs.versions.minecraft.get())
}

dependencies {
    compileOnly(projects.bendingApi)
}
