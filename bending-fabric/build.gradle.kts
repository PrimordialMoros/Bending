plugins {
    id("platform-conventions")
    alias(libs.plugins.fabric.loom)
}

repositories {
    maven("https://maven.fabricmc.net/") {
        mavenContent { includeGroup("net.fabricmc") }
    }
    maven("https://maven.nucleoid.xyz/") // Placeholder API
}

dependencies {
    minecraft(libs.fabric.minecraft)
    mappings(loom.officialMojangMappings())
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.placeholder)
    include(libs.fabric.placeholder)
    modImplementation(libs.adventure.fabric)
    include(libs.adventure.fabric)
    modImplementation(libs.adventure.legacy)
    include(libs.adventure.legacy)
    bendingImplementation(project(":bending-common"))
    modImplementation(libs.math.fabric.get())
    include(libs.math.fabric.get())
    bendingImplementation(libs.tasker.fabric.get())
    bendingImplementation(libs.configurate.hocon.get())
    bendingImplementation(libs.h2.get())
    modImplementation(libs.cloud.fabric)
    include(libs.cloud.fabric)
    implementation(libs.cloud.minecraft) {
        isTransitive = false
    }
    include(libs.cloud.minecraft)
}

loom {
    interfaceInjection.enableDependencyInterfaceInjection
    serverOnlyMinecraftJar()
}

tasks {
    shadowJar {
        reloc("io.leangen", "leangen")
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
