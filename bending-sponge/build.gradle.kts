import org.spongepowered.gradle.plugin.config.PluginLoaders
import org.spongepowered.plugin.metadata.model.PluginDependency

plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.sponge)
    alias(libs.plugins.vanilla)
}

repositories {
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://repo.jpenilla.xyz/snapshots/") //cloud-sponge not available in main repo yet
}

minecraft {
    version(libs.versions.minecraft.get())
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
            description("Lead dev")
        }
        dependency("spongeapi") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(false)
        }
        dependency("luckperms") {
            loadOrder(PluginDependency.LoadOrder.AFTER)
            optional(true)
        }
    }
}


dependencies {
    implementation(project(":bending-common"))
    implementation(libs.math.sponge)
    implementation(libs.tasker.sponge)
    implementation(libs.mariadb) {
        isTransitive = false
    }
    implementation(libs.postgresql)
    implementation(libs.h2)
    implementation(libs.hsql)
    implementation(libs.bstats.sponge)
    implementation(libs.cloud.sponge)
    implementation(libs.cloud.minecraft) {
        isTransitive = false
    }
    compileOnly(libs.sponge.api)
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
