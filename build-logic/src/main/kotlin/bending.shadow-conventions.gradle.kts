plugins {
    id("bending.base-conventions")
    id("com.gradleup.shadow")
}

tasks {
    shadowJar {
        archiveClassifier = ""
        mergeServiceFiles()
        filesMatching("META-INF/services/**") {
            duplicatesStrategy = DuplicatesStrategy.INCLUDE
        }
        val licenseName = "LICENSE_${rootProject.name.uppercase()}"
        from("$rootDir/LICENSE") {
            into("META-INF")
            rename { licenseName }
        }
        val excluded = setOf("error_prone_annotations", "geantyref", "jspecify", "slf4j-api")
        dependencies {
            exclude {
                excluded.contains(it.moduleName)
            }
            reloc("org.bstats", "bstats")
            reloc("com.sasorio.event", "eventbus")
            reloc("me.moros.storage", "storage")
        }
    }
}
