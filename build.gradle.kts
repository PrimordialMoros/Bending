plugins {
    id("java")
    id("com.github.johnrengelman.shadow").version("7.1.2")
}

allprojects {
    group = "me.moros"
    version = "1.5.0-SNAPSHOT"

    apply(plugin = "java-library")
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
    }
    dependencies {
        compileOnly("org.checkerframework", "checker-qual", "3.22.1")
    }
}
