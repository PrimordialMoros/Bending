plugins {
    id("base-conventions")
    id("org.spongepowered.gradle.vanilla")
}

minecraft {
    version().set(libs.versions.minecraft)
    platform(org.spongepowered.gradle.vanilla.repository.MinecraftPlatform.SERVER)
}

dependencies {
    compileOnlyApi(projects.bendingApi)
}
