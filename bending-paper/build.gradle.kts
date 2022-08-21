import io.papermc.paperweight.userdev.attribute.Obfuscation

plugins {
    id("com.github.johnrengelman.shadow").version("7.1.2")
    id("io.papermc.paperweight.userdev").version("1.3.8").apply(false)
}

repositories {
    maven("https://maven.enginehub.org/repo/") // WorldGuard
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PAPI
    maven("https://ci.ender.zone/plugin/repository/everything/") // LWC
    maven("https://repo.glaremasters.me/repository/towny/") // Towny
    maven("https://jitpack.io") // GriefPrevention
}

val adapters = configurations.create("adapters") {
    description = "Adapters to include in the JAR"
    isCanBeConsumed = false
    isCanBeResolved = true
    shouldResolveConsistentlyWith(configurations["runtimeClasspath"])
    attributes {
        attribute(Obfuscation.OBFUSCATION_ATTRIBUTE, objects.named(Obfuscation.OBFUSCATED))
    }
}

dependencies {
    implementation(project(":bending-api"))
    project.project(":bending-paper:adapters").subprojects.forEach {
        adapters(project(it.path, "reobf"))
    }
    implementation("me.moros", "storage", "2.1.0")
    implementation("com.github.ben-manes.caffeine", "caffeine", "3.1.1") {
        exclude(module = "checker-qual")
    }
    implementation("org.spongepowered", "configurate-hocon", "4.1.2")
    implementation("org.jdbi", "jdbi3-core", "3.32.0") {
        exclude(module = "caffeine")
        exclude(module = "slf4j-api")
    }
    implementation("com.zaxxer", "HikariCP", "5.0.1") {
        exclude(module = "slf4j-api")
    }
    implementation("org.postgresql", "postgresql", "42.4.0") {
        exclude(module = "checker-qual")
    }
    implementation("com.h2database", "h2", "2.1.214")
    implementation("org.bstats", "bstats-bukkit", "3.0.0")
    implementation("cloud.commandframework", "cloud-paper", "1.7.0")
    implementation("cloud.commandframework", "cloud-minecraft-extras", "1.7.0") {
        exclude(group = "net.kyori")
    }
    implementation("com.github.stefvanschie.inventoryframework", "IF", "0.10.6")
    compileOnly("io.papermc.paper", "paper-api", "1.18.2-R0.1-SNAPSHOT")
    compileOnly("com.github.TechFortress", "GriefPrevention", "16.18")
    compileOnly("com.palmergames.bukkit.towny", "towny", "0.98.2.6")
    compileOnly("com.griefcraft.lwc", "LWCX", "2.2.6")
    compileOnly("com.sk89q.worldguard", "worldguard-bukkit", "7.0.7")
    compileOnly("me.clip", "placeholderapi", "2.11.2")
    compileOnly("net.luckperms", "api", "5.4")
}

configurations.implementation {
    exclude(module = "error_prone_annotations")
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
    shadowJar {
        dependsOn(project(":bending-paper:adapters").subprojects.map { it.tasks.named("assemble") })
        from(Callable {
            adapters.resolve()
                    .map { f ->
                        zipTree(f).matching {
                            exclude("META-INF/")
                        }
                    }
        })
        archiveClassifier.set("")
        archiveBaseName.set(rootProject.name)
        destinationDirectory.set(rootProject.buildDir)
        dependencies {
            relocate("cloud.commandframework", "me.moros.bending.internal.cf")
            relocate("com.github.benmanes.caffeine", "me.moros.bending.internal.caffeine")
            relocate("com.github.stefvanschie.inventoryframework", "me.moros.bending.internal.inventoryframework")
            relocate("com.typesafe", "me.moros.bending.internal.typesafe")
            relocate("com.zaxxer.hikari", "me.moros.bending.internal.hikari")
            relocate("io.leangen", "me.moros.bending.internal.leangen")
            relocate("me.moros.storage", "me.moros.bending.internal.storage")
            relocate("org.bstats", "me.moros.bending.bstats")
            relocate("org.h2", "me.moros.bending.internal.h2")
            relocate("org.jdbi", "me.moros.bending.internal.jdbi")
            relocate("org.postgresql", "me.moros.bending.internal.postgresql")
            relocate("org.spongepowered.configurate", "me.moros.bending.internal.configurate")
        }
    }
    named<Copy>("processResources") {
        filesMatching("plugin.yml") {
            expand("pluginVersion" to project.version)
        }
        from("$rootDir/LICENSE") {
            rename { "${rootProject.name.toUpperCase()}_${it}" }
        }
    }
}
