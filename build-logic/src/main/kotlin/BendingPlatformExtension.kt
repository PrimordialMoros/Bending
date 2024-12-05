import org.gradle.api.file.RegularFileProperty
import javax.inject.Inject

abstract class BendingPlatformExtension @Inject constructor() {
  abstract val productionJar: RegularFileProperty
}
