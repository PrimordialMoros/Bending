plugins {
    alias(libs.plugins.shadow)
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
    implementation(project(":bending-common"))
    project.project(":bending-paper:adapters").subprojects.forEach {
        implementation(project(it.path, "reobf"))
    }
    implementation(libs.math.bukkit)
    implementation(libs.tasker.bukkit)
    implementation(libs.mariadb) {
        isTransitive = false
    }
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.hsql)
    implementation(libs.configurate.hocon)
    implementation(libs.bstats.bukkit)
    implementation(libs.cloud.paper)
    implementation(libs.cloud.minecraft) {
        isTransitive = false
    }
    implementation(libs.inventory.framework)
    compileOnly(libs.paper)
    compileOnly(libs.grief.prevention)
    compileOnly(libs.towny)
    compileOnly(libs.lwc)
    compileOnly(libs.worldguard.bukkit)
    compileOnly(libs.papi)
    compileOnly(libs.luckperms.api)
}

configurations {
    implementation {
        exclude(module = "error_prone_annotations")
    }
    runtimeClasspath  {
        exclude(module = "checker-qual")
        exclude(module = "slf4j-api")
    }
}

tasks {
    assemble {
        dependsOn(shadowJar)
    }
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set(project.name)
        destinationDirectory.set(rootProject.buildDir)
        fun reloc(pkg: String, name: String) = relocate(pkg, "me.moros.bending.$name")
        fun relocInternal(pkg: String, name: String) = reloc(pkg, "internal.$name")
        dependencies {
            reloc("me.moros.storage", "storage")
            reloc("net.kyori.event", "event.bus")
            reloc("org.bstats", "bstats")
            relocInternal("cloud.commandframework", "cloudframework")
            relocInternal("com.github.benmanes.caffeine", "caffeine")
            relocInternal("com.github.stefvanschie.inventoryframework", "inventoryframework")
            relocInternal("com.typesafe", "typesafe")
            relocInternal("com.zaxxer.hikari", "hikari")
            relocInternal("io.leangen", "leangen")
            relocInternal("org.h2", "h2")
            relocInternal("org.hsqldb", "hsqldb")
            relocInternal("org.jdbi", "jdbi")
            relocInternal("org.mariadb", "mariadb")
            relocInternal("org.postgresql", "postgresql")
            relocInternal("org.spongepowered.configurate", "configurate")
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
