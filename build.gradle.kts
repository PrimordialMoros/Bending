plugins {
    java
    signing
    `maven-publish`
    id("com.github.johnrengelman.shadow").version("6.1.0")
}

group = "me.moros"
version = "1.1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
	mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://repo.aikar.co/content/groups/aikar/")
	maven("https://maven.enginehub.org/repo/")
	maven("https://repo.codemc.org/repository/maven-public")
	maven("https://jitpack.io")
}

dependencies {
    implementation("me.moros", "atlas-core", "1.1.0-SNAPSHOT")
    implementation("org.bstats", "bstats-bukkit-lite", "1.7")
    implementation("org.apache.commons", "commons-math3", "3.6.1")
    compileOnly("com.destroystokyo.paper", "paper-api", "1.16.5-R0.1-SNAPSHOT")
    compileOnly("com.github.TechFortress", "GriefPrevention", "16.7.1")
    compileOnly("com.github.TownyAdvanced", "Towny", "0.96.2.0")
    compileOnly("com.sk89q.worldedit", "worldedit-core", "7.0.0-SNAPSHOT")
    compileOnly("com.sk89q.worldedit", "worldedit-bukkit", "7.0.0-SNAPSHOT")
    compileOnly("com.sk89q.worldguard", "worldguard-core", "7.0.0-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set(rootProject.name)
        dependencies {
            relocate("co.aikar.commands", "me.moros.atlas.acf")
            relocate("co.aikar.locales", "me.moros.atlas.locales")
            relocate("co.aikar.taskchain", "me.moros.atlas.taskchain")
            relocate("com.github.benmanes.caffeine", "me.moros.atlas.caffeine")
            relocate("com.typesafe", "me.moros.atlas.typesafe")
            relocate("com.zaxxer.hikari", "me.moros.atlas.hikari")
            relocate("io.leangen", "me.moros.atlas.jdbi-leangen")
            relocate("net.jodah.expiringmap", "me.moros.atlas.expiringmap")
            relocate("org.antlr", "me.moros.atlas.jdbi-antlr")
            relocate("org.apache.commons.math3", "me.moros.bending.internal.apachemath")
            relocate("org.bstats", "me.moros.bending.bstats")
            relocate("org.checkerframework", "me.moros.atlas.cf")
            relocate("org.h2", "me.moros.atlas.h2")
            relocate("org.jdbi", "me.moros.atlas.jdbi")
            relocate("org.postgresql", "me.moros.atlas.postgresql")
            relocate("org.spongepowered.configurate", "me.moros.atlas.configurate")
        }
    }
    build {
        dependsOn(shadowJar)
    }
    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    withType<Sign>().configureEach {
        onlyIf { !version.toString().endsWith("SNAPSHOT") }
    }
    withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
        options.isFork = true
        options.forkOptions.executable = "javac"
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
