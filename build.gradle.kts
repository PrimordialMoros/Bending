plugins {
    java
    signing
    `maven-publish`
    id("com.github.johnrengelman.shadow").version("7.1.2")
    id("io.papermc.paperweight.userdev").version("1.3.3")
}

group = "me.moros"
version = "1.3.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
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
    maven("https://maven.enginehub.org/repo/") // WorldGuard
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") // PAPI
    maven("https://ci.ender.zone/plugin/repository/everything/") // LWC
    maven("https://jitpack.io") // GriefPrevention, Towny
}

dependencies {
    implementation("me.moros", "storage", "2.1.0")
    implementation("com.github.ben-manes.caffeine", "caffeine", "3.0.5") {
        exclude(module = "checker-qual")
    }
    implementation("org.spongepowered", "configurate-hocon", "4.1.2")
    implementation("org.jdbi", "jdbi3-core", "3.26.1") {
        exclude(module = "caffeine")
    }
    implementation("com.zaxxer", "HikariCP", "5.0.0")
    implementation("org.postgresql", "postgresql", "42.3.1") {
        exclude(module = "checker-qual")
    }
    implementation("com.h2database", "h2", "2.0.206")
    implementation("org.bstats", "bstats-bukkit", "2.2.1")
    implementation("cloud.commandframework","cloud-paper", "1.6.1")
    implementation("cloud.commandframework","cloud-minecraft-extras", "1.6.1") {
        exclude(group = "net.kyori")
    }
    implementation("com.github.stefvanschie.inventoryframework", "IF", "0.10.4")
    paperDevBundle("1.18.1-R0.1-SNAPSHOT")
    compileOnly("com.github.TechFortress", "GriefPrevention", "16.17.1")
    compileOnly("com.github.TownyAdvanced", "Towny", "0.97.5.0")
    compileOnly("com.griefcraft.lwc", "LWCX", "2.2.6")
    compileOnly("com.sk89q.worldguard", "worldguard-bukkit", "7.0.0") {
        exclude(module = "bukkit")
    }
    compileOnly("me.clip", "placeholderapi", "2.11.1")
    compileOnly("net.luckperms", "api", "5.3")
    compileOnly("org.checkerframework", "checker-qual", "3.21.0")
}

configurations.implementation {
    exclude(module = "slf4j-api")
    exclude(module = "error_prone_annotations")
}

tasks {
    shadowJar {
        dependencies {
            relocate("cloud.commandframework", "me.moros.bending.internal.cf")
            relocate("com.github.benmanes.caffeine", "me.moros.bending.internal.caffeine")
            relocate("com.github.stefvanschie.inventoryframework", "me.moros.bending.internal.inventoryframework")
            relocate("com.typesafe", "me.moros.bending.internal.typesafe")
            relocate("com.zaxxer.hikari", "me.moros.bending.internal.hikari")
            relocate("io.leangen", "me.moros.bending.internal.leangen")
            relocate("me.moros.storage", "me.moros.bending.internal.storage")
            relocate("org.antlr", "me.moros.bending.internal.antlr")
            relocate("org.bstats", "me.moros.bending.bstats")
            relocate("org.h2", "me.moros.bending.internal.h2")
            relocate("org.jdbi", "me.moros.bending.internal.jdbi")
            relocate("org.postgresql", "me.moros.bending.internal.postgresql")
            relocate("org.spongepowered.configurate", "me.moros.bending.internal.configurate")
        }
        //minimize()
    }
    assemble {
        dependsOn(reobfJar)
    }
    withType<Sign>().configureEach {
        onlyIf { !isSnapshot() }
    }
    withType<JavaCompile> {
        options.compilerArgs.add("-Xlint:unchecked")
        options.compilerArgs.add("-Xlint:deprecation")
        options.encoding = "UTF-8"
    }
    named<Copy>("processResources") {
        filesMatching("plugin.yml") {
            expand("pluginVersion" to project.version)
        }
        from("LICENSE") {
            rename { "${project.name.toUpperCase()}_${it}"}
        }
    }
}

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
        pom {
            name.set(project.name)
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
