plugins {
    id("base-conventions")
    id("com.modrinth.minotaur")
}

val platformExt = extensions.create("bendingPlatform", BendingPlatformExtension::class)

configurations.create("bendingImplementation")
configurations.implementation {
    extendsFrom(configurations.getByName("bendingImplementation"))
}

val runtimeDownload: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

tasks {
    shadowJar {
        configurations = listOf(project.configurations.getByName("bendingImplementation"))
        archiveClassifier.set("")
        archiveBaseName.set(project.name)
        from("$rootDir/LICENSE") {
            rename { "${rootProject.name.uppercase()}_${it}" }
        }
        val excluded = setOf("checker-qual", "error_prone_annotations", "geantyref", "slf4j-api")
        dependencies {
            reloc("org.bstats", "bstats")
            reloc("net.kyori.event", "eventbus")
            reloc("me.moros.storage", "storage")
            exclude {
                excluded.contains(it.moduleName)
            }
        }
        mergeServiceFiles()
    }
    val copyJar = register("copyJar", CopyFile::class) {
        fileToCopy.set(platformExt.productionJar)
        destination.set(platformExt.productionJar.flatMap { rootProject.layout.buildDirectory.file(it.asFile.name) })
        dependsOn(jar)
    }
    assemble {
        dependsOn(copyJar)
    }
}

modrinth {
    projectId.set("DzD7S3mv")
    versionType.set("release")
    file.set(platformExt.productionJar)
    changelog.set(releaseNotes)
    token.set(providers.environmentVariable("MODRINTH_TOKEN"))
    dependencies {
        optional.project("luckperms")
        optional.project("miniplaceholders")
    }
}
