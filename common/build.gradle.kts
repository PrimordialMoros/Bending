dependencies {
    api(projects.bendingApi)
    api(libs.storage)
    api(libs.eventbus)
    api(libs.caffeine)
    api(libs.hikari)
    api(libs.jdbi) { exclude(module = "caffeine") }
    compileOnlyApi(libs.guava)
    compileOnlyApi(libs.luckperms.api)
    compileOnlyApi(libs.bundles.configurate)
    compileOnlyApi(libs.bundles.cloud)
}

configurations {
    runtimeElements {
        exclude(module = "error_prone_annotations")
        exclude(module = "slf4j-api")
    }
}

