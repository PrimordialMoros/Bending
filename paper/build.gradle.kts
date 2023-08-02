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
    bendingImplementation(projects.adapterV120R1) { targetConfiguration = "reobf" }
    bendingImplementation(libs.tasker.bukkit)
    bendingImplementation(libs.bstats.bukkit)
    bendingImplementation(libs.cloud.minecraft) { isTransitive = false }
    bendingImplementation(libs.cloud.paper)
    bendingImplementation(libs.inventory.framework)
    bendingImplementation(libs.bundles.configurate) { exclude(module = "gson") }
    runtimeDownload(libs.caffeine)
    runtimeDownload(libs.hikari)
    runtimeDownload(libs.jdbi)
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
    tree.set(runtimeDownloadConfig.incoming.resolutionResult.rootComponent)
    files.from(runtimeDownloadConfig)
    outputFileName.set("bending-dependencies")
    outputDir.set(layout.buildDirectory.dir("generated/dependencies"))
}

sourceSets.main {
    resources {
        srcDir(generateRuntimeDependencies)
    }
}

bendingPlatform {
    productionJar.set(tasks.shadowJar.flatMap { it.archiveFile })
}

hangarPublish.publications.register("plugin") {
    version.set(project.version as String)
    owner.set("Moros")
    slug.set("Bending")
    channel.set("Release")
    changelog.set(releaseNotes)
    apiKey.set(providers.environmentVariable("HANGAR_TOKEN"))
    platforms.register(io.papermc.hangarpublishplugin.model.Platforms.PAPER) {
        jar.set(bendingPlatform.productionJar)
        platformVersions.add(libs.versions.minecraft)
        dependencies.url("LuckPerms", "https://luckperms.net/") { required.set(false) }
        dependencies.url("WorldGuard", "https://enginehub.org/worldguard/") { required.set(false) }
        dependencies.url("PlaceholderAPI", "https://www.spigotmc.org/resources/placeholderapi.6245/") { required.set(false) }
        dependencies.url("LWC Extended", "https://www.spigotmc.org/resources/lwc-extended.69551/") { required.set(false) }
        dependencies.hangar("MiniPlaceholders", "MiniPlaceholders") { required.set(false) }
        dependencies.hangar("TownyAdvanced", "Towny") { required.set(false) }
        dependencies.hangar("GriefPrevention", "GriefPrevention") { required.set(false) }
    }
}

modrinth {
    versionName.set("paper-$version")
    loaders.set(listOf("paper", "purpur"))
    gameVersions.add(libs.versions.minecraft)
}
