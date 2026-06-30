package exposed.examples.spring.modulith.boundaries.shipping

import org.springframework.modulith.ApplicationModule
import org.springframework.modulith.PackageInfo

@ApplicationModule(allowedDependencies = ["orders :: events"])
@PackageInfo
class ModuleMetadata
