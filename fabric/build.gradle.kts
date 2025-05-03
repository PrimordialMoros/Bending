plugins {
    id("bending.platform-conventions")
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
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.placeholder)

    modImplementation(libs.fabric.permissions)
    include(libs.fabric.permissions)
    modImplementation(libs.sgui)
    include(libs.sgui)
    modImplementation(libs.adventure.fabric)
    include(libs.adventure.fabric)
    modImplementation(libs.cloud.fabric)
    include(libs.cloud.fabric)

    implementation(libs.cloud.minecraft)
    include(libs.cloud.minecraft)
    implementation(libs.bundles.configurate)
    include(libs.bundles.configurate)

    bendingImplementation(projects.bendingCommon)
    bendingImplementation(projects.bendingNms)
    bendingImplementation(libs.tasker.fabric)
    bendingImplementation(libs.caffeine)
    bendingImplementation(libs.hikari)
    bendingImplementation(libs.jdbi)
    bendingImplementation(libs.h2)
    bendingImplementation(libs.flyway.core) { exclude(module = "gson") }
}

loom {
    interfaceInjection.enableDependencyInterfaceInjection
}

tasks {
    shadowJar {
        dependencies {
            reloc("com.github.benmanes.caffeine", "caffeine")
            reloc("com.zaxxer.hikari", "hikari")
            reloc("org.jdbi", "jdbi")
            reloc("org.h2", "h2")
            reloc("org.flyway", "flyway")
            reloc("org.antlr", "antlr")
            reloc("com.fasterxml.jackson", "jackson")
        }
    }
    named<Copy>("processResources") {
        filesMatching("fabric.mod.json") {
            expand(mapOf("version" to project.version, "mcVersion" to libs.versions.minecraft.get()))
        }
    }
    remapJar {
        val shadowJar = getByName<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar")
        dependsOn(shadowJar)
        inputFile = shadowJar.archiveFile
        addNestedDependencies = true
        archiveFileName = "${project.name}-mc${libs.versions.minecraft.get()}-${project.version}.jar"
    }
}

bendingPlatform {
    productionJar = tasks.remapJar.flatMap { it.archiveFile }
}

modrinth {
    versionName = "fabric-$version"
    gameVersions.add(libs.versions.minecraft)
    dependencies {
        required.project("fabric-api")
        optional.project("placeholder-api")
    }
}
