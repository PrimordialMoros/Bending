dependencies {
    api(project(":bending-api"))
    api(libs.eventbus)
    api(libs.hikari)
    api(libs.jdbi) { exclude(module = "caffeine") }
    api(libs.storage)
    api(libs.tasker.core)
    api(libs.caffeine)
    compileOnlyApi(libs.configurate.hocon)
    compileOnlyApi(libs.guava)
    compileOnlyApi(libs.bundles.cloud)
    compileOnlyApi(libs.luckperms.api)
}

configurations {
    runtimeElements {
        exclude(module = "error_prone_annotations")
        exclude(module = "slf4j-api")
    }
}

