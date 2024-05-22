plugins {
    id("bending.base-conventions")
    alias(libs.plugins.jmh)
}

dependencies {
    implementation(projects.bendingCommon)
}
