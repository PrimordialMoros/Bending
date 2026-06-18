plugins {
    id("bending.shadow-conventions")
    id("com.modrinth.minotaur")
}

val platformExt = extensions.create("bendingPlatform", BendingPlatformExtension::class)

val runtimeDownload: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
    }
}

tasks {
    val copyJar = register<CopyFile>("copyJar") {
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
