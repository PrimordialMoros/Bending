import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.result.DependencyResult
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class WriteDependencies : DefaultTask() {
    @get:Input
    abstract val tree: Property<ResolvedComponentResult>

    @get:InputFiles
    abstract val files: ConfigurableFileCollection

    @get:Input
    abstract val outputFileName: Property<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun run() {
        val outputLines = StringBuilder()
        val outputFile = outputDir.get().file(outputFileName.get()).asFile
        for (dependency in deps()) {
            val id = dependency.resolvedVariant.owner as ModuleComponentIdentifier
            outputLines.append(id.displayName).append("\n")
        }
        outputFile.parentFile.mkdirs()
        outputFile.delete()
        outputFile.writeText(outputLines.toString())
    }

    private fun deps(): List<ResolvedDependencyResult> {
        val set = mutableSetOf<ResolvedDependencyResult>()
        set.addFrom(tree.get().dependencies)
        return set.associateBy { it.resolvedVariant.owner.displayName }
                .map { it.value }
                .sortedBy { it.resolvedVariant.owner.displayName }
    }

    private fun MutableSet<ResolvedDependencyResult>.addFrom(dependencies: Set<DependencyResult>) {
        for (dependency in dependencies) {
            dependency as ResolvedDependencyResult
            add(dependency)
        }
    }
}
