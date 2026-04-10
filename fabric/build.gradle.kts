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
    maven("https://repo.moros.me/snapshots/") {
        mavenContent { includeGroup("net.kyori") }
    }
}

dependencies {
    minecraft(libs.fabric.minecraft)
    implementation(libs.fabric.loader)
    implementation(libs.fabric.api)
    implementation(libs.fabric.placeholder)

    implementation(libs.fabric.permissions)
    include(libs.fabric.permissions)
    implementation(libs.sgui)
    include(libs.sgui)
    implementation(libs.adventure.fabric)
    include(libs.adventure.fabric)
    implementation(libs.cloud.fabric)
    include(libs.cloud.fabric)

    implementation(libs.cloud.minecraft)
    include(libs.cloud.minecraft)
    implementation(libs.bundles.configurate)
    include(libs.bundles.configurate)

    shadow(projects.bendingCommon)
    shadow(projects.bendingNms)
    shadow(libs.tasker.fabric)
    shadow(libs.caffeine)
    shadow(libs.hikari)
    shadow(libs.jdbi)
    shadow(libs.h2)
    shadow(libs.flyway.core) { exclude(module = "gson") }
}

loom {
    interfaceInjection.enableDependencyInterfaceInjection
}

tasks {
    shadowJar {
        val processedFabricModJson = layout.buildDirectory.file("resources/main/fabric.mod.json").get().asFile.absoluteFile
        eachFile {
            // Exclude the fabric.mod.json in the resources folder to allow the one from Fabric's jar task to be added
            if (path == "fabric.mod.json" && file.absoluteFile == processedFabricModJson) {
                exclude()
            }
        }
        from(zipTree(jar.flatMap { it.archiveFile }))
        configurations = listOf(project.configurations.shadow.get())
        archiveFileName = "${project.name}-mc${libs.versions.minecraft.get()}-${project.version}.jar"
        dependencies {
            reloc("com.github.benmanes.caffeine", "caffeine")
            reloc("com.zaxxer.hikari", "hikari")
            reloc("org.jdbi", "jdbi")
            reloc("org.h2", "h2")
            reloc("org.flyway", "flyway")
            reloc("com.fasterxml.jackson", "jackson")
            reloc("tools.jackson", "jackson")
        }
    }
    named<Copy>("processResources") {
        expandProperties(
            "fabric.mod.json",
            mapOf("version" to project.version, "mcVersion" to libs.versions.minecraft.get())
        )
    }
}

bendingPlatform {
    productionJar = tasks.shadowJar.flatMap { it.archiveFile }
}

modrinth {
    versionName = "fabric-$version"
    gameVersions.add(libs.versions.minecraft)
    dependencies {
        required.project("fabric-api")
        optional.project("placeholder-api")
    }
}
