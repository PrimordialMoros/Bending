plugins {
    id("base-conventions")
    `maven-publish`
    signing
}

java {
    if (!isSnapshot()) { withJavadocJar() }
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = project.name
                description = "Modern Bending api for Minecraft"
                url = "https://github.com/PrimordialMoros/Bending"
                inceptionYear = "2020"
                licenses {
                    license {
                        name = "The GNU Affero General Public License, Version 3.0"
                        url = "https://www.gnu.org/licenses/agpl-3.0.txt"
                        distribution = "repo"
                    }
                }
                developers {
                    developer {
                        id = "moros"
                        name = "Moros"
                    }
                }
                scm {
                    connection = "scm:git:https://github.com/PrimordialMoros/Bending.git"
                    developerConnection = "scm:git:ssh://git@github.com/PrimordialMoros/Bending.git"
                    url = "https://github.com/PrimordialMoros/Bending"
                }
                issueManagement {
                    system = "Github"
                    url = "https://github.com/PrimordialMoros/Bending/issues"
                }
            }
        }
    }
    repositories {
        val snapshotUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
        val releaseUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
        maven {
            name = "sonatype"
            credentials(PasswordCredentials::class)
            url = if (isSnapshot()) snapshotUrl else releaseUrl
        }
    }
}

signing {
    setRequired { !isSnapshot() }
    sign(publishing.publications["maven"])
}
