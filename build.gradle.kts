plugins {
    id("org.checkerframework").version("0.6.14")
}

allprojects {
    group = "me.moros"
    version = "2.2.0-SNAPSHOT"

    apply(plugin = "java")
    apply(plugin = "org.checkerframework")

    repositories {
        mavenCentral()
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        maven("https://repo.papermc.io/repository/maven-public/")
    }
    configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }
    tasks {
        withType<JavaCompile> {
            options.compilerArgs.add("-Xlint:unchecked")
            options.compilerArgs.add("-Xlint:deprecation")
            options.encoding = "UTF-8"
        }
    }
}
