plugins {
    id("platform-conventions")
    alias(libs.plugins.hangar)
    alias(libs.plugins.run.paper)
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
    bendingImplementation(projects.adapterV120R3) { targetConfiguration = "reobf" }
    bendingImplementation(libs.tasker.bukkit)
    bendingImplementation(libs.bstats.bukkit)
    bendingImplementation(libs.cloud.minecraft) { isTransitive = false }
    bendingImplementation(libs.cloud.paper)
    bendingImplementation(libs.inventory.framework)
    bendingImplementation(libs.bundles.configurate) { exclude(module = "gson") }
    runtimeDownload(libs.caffeine)
    runtimeDownload(libs.hikari)
    runtimeDownload(libs.jdbi)
    runtimeDownload(libs.bundles.flyway)
    runtimeDownload(libs.bundles.drivers.nonstandard)
    compileOnly(libs.paper)
    compileOnly(libs.grief.prevention)
    compileOnly(libs.towny)
    compileOnly(libs.lwc)
    compileOnly(libs.worldguard)
    compileOnly(libs.papi)
}

tasks {
    runServer {
        minecraftVersion(libs.versions.minecraft.get())
    }
    shadowJar {
        exclude("fonts/") // We aren't using any fonts from IF
        dependencies {
            reloc("cloud.commandframework", "cloudframework")
            reloc("com.typesafe", "typesafe")
            reloc("org.spongepowered.configurate", "configurate")
            reloc("com.github.stefvanschie.inventoryframework", "inventoryframework")
        }
    }
    named<Copy>("processResources") {
        filesMatching("*plugin.yml") {
            expand("pluginVersion" to project.version)
        }
    }
}

val generateRuntimeDependencies = tasks.register("writeDependencies", WriteDependencies::class) {
    val runtimeDownloadConfig = configurations.getByName("runtimeDownload")
    tree = runtimeDownloadConfig.incoming.resolutionResult.rootComponent
    files.from(runtimeDownloadConfig)
    outputFileName = "bending-dependencies"
    outputDir = layout.buildDirectory.dir("generated/dependencies")
}

sourceSets.main {
    resources {
        srcDir(generateRuntimeDependencies)
    }
}

bendingPlatform {
    productionJar = tasks.shadowJar.flatMap { it.archiveFile }
}

hangarPublish.publications.register("plugin") {
    version = project.version as String
    channel = "Release"
    id = "Bending"
    changelog = releaseNotes
    apiKey = providers.environmentVariable("HANGAR_TOKEN")
    platforms.paper {
        jar = bendingPlatform.productionJar
        platformVersions.add(libs.versions.minecraft)
        dependencies.url("LuckPerms", "https://luckperms.net/") { required = false }
        dependencies.url("WorldGuard", "https://enginehub.org/worldguard/") { required = false }
        dependencies.url("PlaceholderAPI", "https://www.spigotmc.org/resources/placeholderapi.6245/"){ required = false }
        dependencies.url("LWC Extended", "https://www.spigotmc.org/resources/lwc-extended.69551/") { required = false }
        dependencies.hangar("MiniPlaceholders") { required = false }
        dependencies.hangar("Towny") { required = false }
        dependencies.hangar("GriefPrevention") { required = false }
    }
}

modrinth {
    versionName = "paper-$version"
    loaders = listOf("paper", "purpur")
    gameVersions.add(libs.versions.minecraft)
}
