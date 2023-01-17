plugins {
    id("java-library")
    id("com.github.johnrengelman.shadow")
}

val platformExt = extensions.create("bendingPlatform", BendingPlatformExtension::class)

val bendingImplementation: Configuration by configurations.creating
configurations.implementation {
    extendsFrom(bendingImplementation)
}

tasks {
    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    shadowJar {
        configurations = listOf(bendingImplementation)
        exclude("org/checkerframework/") // Try to catch the myriad dependency leaks
        archiveClassifier.set("")
        from("$rootDir/LICENSE") {
            rename { "${rootProject.name.toUpperCase()}_${it}" }
        }
        dependencies {
            reloc("org.bstats", "bstats")
            reloc("me.moros.storage", "storage")
            reloc("net.kyori.event", "eventbus")
            reloc("com.typesafe", "typesafe")
            reloc("org.spongepowered.configurate", "configurate")
            reloc("cloud.commandframework", "cloudframework")
            reloc("com.github.benmanes.caffeine", "caffeine")
            reloc("com.zaxxer.hikari", "hikari")
            reloc("io.leangen", "leangen")
            reloc("org.jdbi", "jdbi")
            reloc("com.mysql", "mysql")
            reloc("org.mariadb", "mariadb")
            reloc("org.postgresql", "postgresql")
            reloc("org.sqlite", "sqlite")
            reloc("org.h2", "h2")
            reloc("org.hsqldb", "hsqldb")
        }
    }
    val copyJar = register("copyJar", CopyFile::class) {
        fileToCopy.set(platformExt.productionJar)
        destination.set(platformExt.productionJar.flatMap { rootProject.layout.buildDirectory.file(it.asFile.name) })
    }
    assemble {
        dependsOn(copyJar)
    }
}

