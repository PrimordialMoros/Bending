plugins {
    id("platform-conventions")
    alias(libs.plugins.fabric.loom)
}

repositories {
    maven("https://maven.fabricmc.net/") {
        mavenContent { includeGroup("net.fabricmc") }
    }
    maven("https://maven.nucleoid.xyz/") {
        mavenContent { includeGroup("eu.pb4") }
    }
}

dependencies {
    minecraft(libs.fabric.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.permissions)
    modImplementation(libs.fabric.placeholder)
    modImplementation(include(libs.sgui.get())!!)
    modImplementation(include(libs.adventure.fabric.get())!!)
    modImplementation(include(libs.adventure.legacy.get())!!)
    modImplementation(include(libs.cloud.fabric.get())!!)
    implementation(include(libs.cloud.minecraft.get())!!)
    bendingImplementation(projects.bendingCommon)
    bendingImplementation(projects.bendingNms)
    bendingImplementation(libs.tasker.fabric)
    bendingImplementation(libs.bundles.configurate) { exclude(module = "gson") }
    bendingImplementation(libs.caffeine)
    bendingImplementation(libs.hikari)
    bendingImplementation(libs.jdbi)
    bendingImplementation(libs.h2)
}

loom {
    interfaceInjection.enableDependencyInterfaceInjection
}

tasks {
    shadowJar {
        dependencies {
            reloc("com.typesafe", "typesafe")
            reloc("org.spongepowered.configurate", "configurate")
            reloc("com.github.benmanes.caffeine", "caffeine")
            reloc("com.zaxxer.hikari", "hikari")
            reloc("org.jdbi", "jdbi")
            reloc("org.h2", "h2")
        }
    }
    named<Copy>("processResources") {
        filesMatching("fabric.mod.json") {
            expand("pluginVersion" to project.version)
        }
    }
    remapJar {
        val shadowJar = getByName<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar")
        dependsOn(shadowJar)
        inputFile.set(shadowJar.archiveFile)
        addNestedDependencies.set(true)
        archiveFileName.set("${project.name}-mc${libs.versions.minecraft.get()}-${project.version}.jar")
    }
}

bendingPlatform {
    productionJar.set(tasks.remapJar.flatMap { it.archiveFile })
}

modrinth {
    versionName.set("fabric-$version")
    gameVersions.add(libs.versions.minecraft)
    dependencies {
        required.project("fabric-api")
        optional.project("placeholder-api")
    }
}
