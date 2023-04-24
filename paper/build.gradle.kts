plugins {
    id("platform-conventions")
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") // Paper
    maven("https://maven.enginehub.org/repo/") // WorldGuard
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PAPI
    maven("https://repo.codemc.io/repository/maven-public/") // LWC
    maven("https://repo.glaremasters.me/repository/towny/") // Towny
    maven("https://jitpack.io") // GriefPrevention
}

dependencies {
    bendingImplementation(projects.bendingCommon)
    bendingImplementation(projects.adapterV119R3) { targetConfiguration = "reobf" }
    bendingImplementation(libs.tasker.bukkit)
    bendingImplementation(libs.bstats.bukkit)
    bendingImplementation(libs.cloud.minecraft) { isTransitive = false }
    bendingImplementation(libs.cloud.paper)
    bendingImplementation(libs.inventory.framework)
    bendingImplementation(libs.bundles.configurate) { exclude(module = "gson") }
    bendingImplementation(libs.bundles.drivers.nonstandard) { isTransitive = false }
    compileOnly(libs.paper)
    compileOnly(libs.grief.prevention)
    compileOnly(libs.towny)
    compileOnly(libs.lwc)
    compileOnly(libs.worldguard.bukkit)
    compileOnly(libs.papi)
}

tasks {
    shadowJar {
        exclude("fonts/") // We aren't using any fonts from IF
        dependencies {
            reloc("cloud.commandframework", "cloudframework")
            reloc("com.typesafe", "typesafe")
            reloc("org.spongepowered.configurate", "configurate")
            reloc("com.github.stefvanschie.inventoryframework", "inventoryframework")
            reloc("io.leangen", "leangen")
        }
    }
    named<Copy>("processResources") {
        filesMatching("*plugin.yml") {
            expand("pluginVersion" to project.version)
        }
    }
}

bendingPlatform {
    productionJar.set(tasks.shadowJar.flatMap { it.archiveFile })
}
