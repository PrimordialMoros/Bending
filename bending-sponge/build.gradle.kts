import org.spongepowered.gradle.vanilla.repository.MinecraftPlatform
import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    id("platform-conventions")
    alias(libs.plugins.sponge)
    alias(libs.plugins.vanilla)
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://repo.jpenilla.xyz/snapshots/") { //cloud-sponge not available in main repo yet
        mavenContent {
            includeGroup("cloud.commandframework")
            snapshotsOnly()
        }
    }
}

minecraft {
    version(libs.versions.minecraft.get())
    platform(MinecraftPlatform.SERVER)
}

dependencies {
    bendingImplementation(project(":bending-common"))
    bendingImplementation(libs.math.sponge)
    bendingImplementation(libs.tasker.sponge)
    bendingImplementation(libs.bstats.sponge)
    bendingImplementation(libs.configurate.hocon)
    bendingImplementation(libs.cloud.minecraft) { isTransitive = false }
    bendingImplementation(libs.cloud.sponge)
    bendingImplementation(libs.bundles.drivers.nonstandard) { isTransitive = false }
    bendingImplementation(libs.slf4j.api)
    bendingImplementation(libs.slf4j.simple)
}

tasks {
    runServer {
        displayMinecraftVersions
    }
    shadowJar {
        dependencies {
            reloc("org.slf4j", "slf4j")
            exclude(dependency("io.leangen.geantyref:geantyref"))
        }
    }
}

sponge {
    apiVersion(libs.versions.sponge.get())
    plugin("bending") {
        loader {
            name(PluginLoaders.JAVA_PLAIN)
            version("1.0")
        }
        displayName("Bending")
        license("AGPL-3.0")
        version(project.version.toString())
        entrypoint("me.moros.bending.SpongeBending")
        description("Bending plugin for Sponge.")
        links {
            source("https://github.com/PrimordialMoros/Bending")
            issues("https://github.com/PrimordialMoros/Bending/issues")
        }
        contributor("Moros") {
            description("Lead Developer")
        }
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
    }
}

bendingPlatform {
    productionJar.set(tasks.shadowJar.flatMap { it.archiveFile })
}
