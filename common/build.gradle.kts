plugins {
    id("base-conventions")
}

dependencies {
    api(projects.bendingApi)
    api(libs.storage)
    api(libs.eventbus)
    compileOnlyApi(libs.caffeine)
    compileOnlyApi(libs.luckperms.api)
    compileOnly(libs.adventure.minimessage)
    compileOnly(libs.mini.placeholders)
    compileOnly(libs.hikari)
    compileOnly(libs.jdbi)
    compileOnly(libs.flyway.core)
    compileOnly(libs.guava)
    compileOnly(libs.gson)
    compileOnly(libs.bundles.configurate)
    compileOnly(libs.bundles.cloud)
}
