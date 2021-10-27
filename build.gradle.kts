plugins {
    `java-library`
    signing
    `maven-publish`
    id("com.github.johnrengelman.shadow").version("7.1.0")
    id("io.papermc.paperweight.userdev").version("1.1.12")
}

group = "me.moros"
version = "1.1.2-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
    if (!isSnapshot()) {
        withJavadocJar()
    }
    withSourcesJar()
}

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://ci.ender.zone/plugin/repository/everything/")
    maven("https://jitpack.io")
}

dependencies {
    api("me.moros", "atlas-core", "1.4.0-SNAPSHOT")
    implementation("org.bstats", "bstats-bukkit", "2.2.1")
    implementation("co.aikar","acf-paper", "0.5.0-SNAPSHOT")
    implementation("com.github.stefvanschie.inventoryframework", "IF", "0.10.3")
    compileOnly("org.checkerframework", "checker-qual", "3.18.1")
    paperDevBundle("1.17.1-R0.1-SNAPSHOT")
    compileOnly("me.clip", "placeholderapi", "2.10.10")
    compileOnly("com.github.TechFortress", "GriefPrevention", "16.7.1") {
        exclude(module = "worldguard")
    }
    compileOnly("com.github.TownyAdvanced", "Towny", "0.96.7.0")
    compileOnly("com.sk89q.worldguard", "worldguard-bukkit", "7.0.0")
    compileOnly("com.griefcraft.lwc", "LWCX", "2.2.6")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set(rootProject.name)
        dependencies {
            relocate("co.aikar.commands", "me.moros.atlas.acf")
            relocate("co.aikar.locales", "me.moros.atlas.locales")
            relocate("com.github.benmanes.caffeine", "me.moros.atlas.caffeine")
            relocate("com.github.stefvanschie.inventoryframework", "me.moros.atlas.inventoryframework")
            relocate("com.typesafe", "me.moros.atlas.typesafe")
            relocate("com.zaxxer.hikari", "me.moros.atlas.hikari")
            relocate("io.leangen", "me.moros.atlas.jdbi-leangen")
            relocate("org.antlr", "me.moros.atlas.jdbi-antlr")
            relocate("org.bstats", "me.moros.bending.bstats")
            relocate("org.h2", "me.moros.atlas.h2")
            relocate("org.jdbi", "me.moros.atlas.jdbi")
            relocate("org.postgresql", "me.moros.atlas.postgresql")
            relocate("org.spongepowered.configurate", "me.moros.atlas.configurate")
        }
        minimize()
    }
    build {
        dependsOn(shadowJar)
    }
    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    withType<Sign>().configureEach {
        onlyIf { !isSnapshot() }
    }
    withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
        options.encoding = "UTF-8"
    }
    named<Copy>("processResources") {
        filesMatching("plugin.yml") {
            expand("pluginVersion" to project.version)
        }
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name.set(project.name.toLowerCase())
            description.set("Modern Bending plugin for Minecraft servers running PaperMC")
            url.set("https://github.com/PrimordialMoros/Bending")
            licenses {
                license {
                    name.set("The GNU Affero General Public License, Version 3.0")
                    url.set("https://www.gnu.org/licenses/agpl-3.0.txt")
                }
            }
            developers {
                developer {
                    id.set("moros")
                    name.set("Moros")
                }
            }
            scm {
                connection.set("scm:git:https://github.com/PrimordialMoros/Bending.git")
                developerConnection.set("scm:git:ssh://git@github.com/PrimordialMoros/Bending.git")
                url.set("https://github.com/PrimordialMoros/Bending")
            }
            issueManagement {
                system.set("Github")
                url.set("https://github.com/PrimordialMoros/Bending/issues")
            }
        }
    }
    if (project.hasProperty("ossrhUsername") && project.hasProperty("ossrhPassword")) {
        val user = project.property("ossrhUsername") as String?
        val pass = project.property("ossrhPassword") as String?
        val repoUrl = if (isSnapshot()) uri("https://oss.sonatype.org/content/repositories/snapshots/") else uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        repositories {
            maven {
                credentials { username = user; password = pass }
                url = repoUrl
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}

fun isSnapshot() = project.version.toString().endsWith("-SNAPSHOT")
