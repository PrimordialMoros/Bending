plugins {
    id("bending.base-conventions")
    id("net.neoforged.moddev")
}

neoForge {
    neoFormVersion = libs.versions.neoform.get()
}

dependencies {
    compileOnlyApi(projects.bendingApi)
}
