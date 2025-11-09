import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Copy

fun ShadowJar.reloc(pkg: String, name: String) {
    relocate(pkg, "bending.libraries.$name")
}

val Project.releaseNotes: Provider<String> get() = providers.environmentVariable("RELEASE_NOTES")

fun Project.isSnapshot() = project.version.toString().endsWith("-SNAPSHOT")

fun Project.apiVersion(): String = project.version.toString().replace(Regex("""\b\d+(?=[^.]*$)"""), "0")

fun Copy.expandProperties(fileName: String, props: Map<String, Any>) {
    props.forEach { (key, value) ->
        inputs.property(key, value.toString())
    }

    filesMatching(fileName) {
        expand(inputs.properties)
    }
}
