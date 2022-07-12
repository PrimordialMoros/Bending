plugins {
    signing
    `maven-publish`
}

dependencies {
    compileOnly("me.moros", "storage", "2.1.0")
    compileOnly("com.github.ben-manes.caffeine", "caffeine", "3.1.1")
    compileOnly("org.spongepowered", "configurate-core", "4.1.2")
    compileOnly("io.papermc.paper", "paper-api", "1.18.2-R0.1-SNAPSHOT")
}

java {
    if (!isSnapshot()) {
        withJavadocJar()
    }
    withSourcesJar()
}

tasks {
    withType<Sign>().configureEach {
        onlyIf { !isSnapshot() }
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
        val snapshotUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        val releaseUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        repositories {
            maven {
                credentials { username = user; password = pass }
                url = if (isSnapshot()) snapshotUrl else releaseUrl
            }
        }
    }
}

signing {
    sign(publishing.publications["maven"])
}

fun isSnapshot() = project.version.toString().endsWith("-SNAPSHOT")
