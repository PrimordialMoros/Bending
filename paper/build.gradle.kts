plugins {
    id("bending.platform-conventions")
    alias(libs.plugins.paperweight.userdev)
    alias(libs.plugins.hangar)
    alias(libs.plugins.run.paper)
}

repositories {
    maven("https://repo.papermc.io/repository/maven-public/") // Paper
    maven("https://maven.enginehub.org/repo/") // WorldGuard
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PAPI
    maven("https://repo.codemc.io/repository/maven-public/") // LWC
    maven("https://repo.glaremasters.me/repository/towny/") // Towny
    maven("https://repo.moros.me/snapshots/")
    maven("https://jitpack.io/") // GriefPrevention
}

dependencies {
    bendingImplementation(projects.bendingCommon)
    bendingImplementation(projects.bendingNms)
    bendingImplementation(libs.tasker.paper)
    bendingImplementation(libs.bstats.bukkit)
    bendingImplementation(libs.cloud.minecraft)
    bendingImplementation(libs.cloud.paper)
    bendingImplementation(libs.inventory.framework)
    runtimeDownload(libs.caffeine)
    runtimeDownload(libs.hikari)
    runtimeDownload(libs.jdbi)
    runtimeDownload(libs.bundles.flyway)
    runtimeDownload(libs.bundles.drivers.nonstandard)
    runtimeDownload(libs.bundles.configurate.loaders)
    compileOnly(libs.grief.prevention)
    compileOnly(libs.towny)
    compileOnly(libs.lwc)
    compileOnly(libs.worldguard)
    compileOnly(libs.papi)
    paperweight.paperDevBundle(libs.versions.paper.api)
}

tasks {
    runServer {
        minecraftVersion(libs.versions.minecraft.get())
    }
    shadowJar {
        exclude("fonts/") // We aren't using any fonts from IF
        dependencies {
            reloc("org.incendo.cloud", "cloud")
            reloc("com.github.stefvanschie.inventoryframework", "inventoryframework")
        }
    }
    named<Copy>("processResources") {
        filesMatching("paper-plugin.yml") {
            expand(mapOf("version" to project.version, "mcVersion" to libs.versions.minecraft.get()))
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

paperweight {
    reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION
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
        dependencies.url("LWC Extended", "https://www.spigotmc.org/resources/lwc-extended.69551/") { required = false }
        dependencies.hangar("MiniPlaceholders") { required = false }
        dependencies.hangar("PlaceholderAPI") { required = false }
        dependencies.hangar("Towny") { required = false }
        dependencies.hangar("GriefPrevention") { required = false }
    }
}

modrinth {
    versionName = "paper-$version"
    loaders = listOf("paper")
    gameVersions.add(libs.versions.minecraft)
}
