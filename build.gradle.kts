plugins {
    `java-library`
    alias(libs.plugins.checker)
}

allprojects {
    group = "me.moros"
    version = "3.1.0-SNAPSHOT"

    apply(plugin = "java-library")
    apply(plugin = "org.checkerframework")

    repositories {
        mavenCentral()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }

    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }

    tasks {
        withType<JavaCompile> {
            options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
            options.encoding = "UTF-8"
        }
    }
}
