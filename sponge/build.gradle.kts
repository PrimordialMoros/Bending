plugins {
    id("platform-conventions")
    alias(libs.plugins.sponge)
    id("org.spongepowered.gradle.vanilla")
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
    platform(org.spongepowered.gradle.vanilla.repository.MinecraftPlatform.SERVER)
}

dependencies {
    bendingImplementation(projects.bendingCommon)
    bendingImplementation(projects.bendingNms)
    bendingImplementation(libs.tasker.sponge)
    bendingImplementation(libs.bstats.sponge)
    bendingImplementation(libs.cloud.minecraft) { isTransitive = false }
    bendingImplementation(libs.cloud.sponge)
    bendingImplementation(libs.bundles.drivers.nonstandard) { isTransitive = false }
    compileOnly(variantOf(libs.sponge.common) { classifier("dev") })
    compileOnly(libs.sponge.mixin)
}

tasks {
    shadowJar {
        archiveBaseName = "${project.name}-mc${libs.versions.minecraft.get()}"
        dependencies {
            reloc("org.incendo.cloud", "cloud")
        }
        manifest.attributes["MixinConfigs"] = "bending-sponge.mixins.json"
    }
}

sponge {
    apiVersion(libs.versions.sponge.api.get())
    plugin("bending") {
        loader {
            name(org.spongepowered.gradle.plugin.config.PluginLoaders.JAVA_PLAIN)
            version("1.0")
        }
        displayName("Bending")
        license("AGPL-3.0")
        version(project.version.toString())
        entrypoint("me.moros.bending.sponge.SpongeBending")
        description("Bending plugin for Sponge.")
        links {
            source("https://github.com/PrimordialMoros/Bending")
            issues("https://github.com/PrimordialMoros/Bending/issues")
        }
        contributor("Moros") {
            description("Lead Developer")
        }
    }
}

bendingPlatform {
    productionJar = tasks.shadowJar.flatMap { it.archiveFile }
}
