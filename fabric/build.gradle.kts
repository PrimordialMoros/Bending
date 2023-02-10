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
    modImplementation(libs.fabric.permissions)
    modImplementation(include(libs.fabric.placeholder.get())!!)
    modImplementation(include(libs.sgui.get())!!)
    modImplementation(include(libs.adventure.fabric.get())!!)
    modImplementation(include(libs.adventure.legacy.get())!!)
    modImplementation(include(libs.math.fabric.get())!!)
    modImplementation(include(libs.cloud.fabric.get())!!)
    implementation(include(libs.cloud.minecraft.get())!!)
    bendingImplementation(project(":common"))
    bendingImplementation(project(":nms"))
    bendingImplementation(libs.tasker.fabric)
    bendingImplementation(libs.bundles.configurate) {
        exclude(module = "gson")
    }
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
            exclude(dependency("io.leangen.geantyref:geantyref"))
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
        archiveFileName.set("bending-${project.name}-mc${libs.versions.minecraft.get()}-${project.version}.jar")
    }
}

bendingPlatform {
    productionJar.set(tasks.remapJar.flatMap { it.archiveFile })
}
