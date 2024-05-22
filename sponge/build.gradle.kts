plugins {
    id("bending.platform-conventions")
    id("org.spongepowered.gradle.vanilla")
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://repo.jpenilla.xyz/snapshots/") { //cloud-sponge not available in main repo yet
        mavenContent {
            includeGroup("org.incendo")
            snapshotsOnly()
        }
    }
}

minecraft {
    version(libs.versions.minecraft.get())
    platform(org.spongepowered.gradle.vanilla.repository.MinecraftPlatform.SERVER)
}

dependencies {
    bendingImplementation(projects.bendingCommon)
    bendingImplementation(projects.bendingNms)
    bendingImplementation(libs.tasker.sponge)
    bendingImplementation(libs.bstats.sponge)
    bendingImplementation(libs.cloud.minecraft) { isTransitive = false }
    bendingImplementation(libs.cloud.sponge)
    bendingImplementation(libs.h2)
    bendingImplementation(libs.flyway.core) { exclude(module = "gson") }
    compileOnly(variantOf(libs.sponge.common) { classifier("dev") })
    compileOnly(libs.sponge.mixin)
}

tasks {
    shadowJar {
        archiveBaseName = "${project.name}-mc${libs.versions.minecraft.get()}"
        dependencies {
            reloc("org.incendo.cloud", "cloud")
            reloc("org.h2", "h2")
            reloc("org.flyway", "flyway")
            reloc("com.fasterxml.jackson", "jackson")
        }
        manifest.attributes["MixinConfigs"] = "bending-sponge.mixins.json"
    }
    named<Copy>("processResources") {
        filesMatching("META-INF/sponge_plugins.json") {
            expand(mapOf("version" to project.version, "apiVersion" to libs.versions.sponge.api.get()))
        }
    }
}

bendingPlatform {
    productionJar = tasks.shadowJar.flatMap { it.archiveFile }
}
