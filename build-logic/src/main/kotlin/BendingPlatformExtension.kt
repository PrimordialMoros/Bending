import org.gradle.api.file.RegularFileProperty
import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class BendingPlatformExtension @Inject constructor(objects: ObjectFactory) {
  val productionJar: RegularFileProperty = objects.fileProperty()
}
