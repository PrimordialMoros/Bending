import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SonatypeHost

plugins {
    id("bending.base-conventions")
    id("com.vanniktech.maven.publish")
    signing
}

mavenPublishing {
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
                url = "https://github.com/PrimordialMoros"
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
    configure(JavaLibrary(JavadocJar.Javadoc(), true))
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
}
