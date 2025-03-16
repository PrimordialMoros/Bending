plugins {
    id("bending.base-conventions")
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
        archiveClassifier = ""
        archiveBaseName = project.name
        from(rootDir.resolve("LICENSE")) {
            rename { "META-INF/${it}_${rootProject.name.uppercase()}" }
        }
        val excluded = setOf("checker-qual", "error_prone_annotations", "jspecify", "geantyref", "slf4j-api")
        dependencies {
            exclude {
                excluded.contains(it.moduleName)
            }
        }
        reloc("org.bstats", "bstats")
        reloc("com.seiama.event", "eventbus")
        reloc("me.moros.storage", "storage")
        mergeServiceFiles()
    }
    val copyJar = register("copyJar", CopyFile::class) {
        fileToCopy = platformExt.productionJar
        destination = platformExt.productionJar.flatMap { rootProject.layout.buildDirectory.file(it.asFile.name) }
        dependsOn(jar)
    }
    assemble {
        dependsOn(copyJar)
    }
}

modrinth {
    projectId = "DzD7S3mv"
    versionType = "release"
    file = platformExt.productionJar
    changelog = releaseNotes
    token = providers.environmentVariable("MODRINTH_TOKEN")
    dependencies {
        optional.project("luckperms")
        optional.project("miniplaceholders")
    }
}
