import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

fun ShadowJar.reloc(pkg: String, name: String) {
    relocate(pkg, "bending.libraries.$name")
}
