plugins {
    id("java-library")
    id("com.github.johnrengelman.shadow").version("7.1.2")
    id("org.checkerframework").version("0.6.20")
}

allprojects {
    group = "me.moros"
    version = "2.3.0-SNAPSHOT"

    apply(plugin = "java-library")
    apply(plugin = "org.checkerframework")
    apply(plugin = "com.github.johnrengelman.shadow")

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
        assemble {
            dependsOn(shadowJar)
        }
    }
}
