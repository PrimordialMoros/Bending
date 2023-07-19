import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.provider.Provider

fun ShadowJar.reloc(pkg: String, name: String) {
    relocate(pkg, "bending.libraries.$name")
}

val Project.releaseNotes: Provider<String> get() = providers.environmentVariable("RELEASE_NOTES")

fun Project.isSnapshot() = project.version.toString().endsWith("-SNAPSHOT")

fun Project.apiVersion(): String = project.version.toString().replace(Regex("""\b\d+(?=[^.]*$)"""), "0")
