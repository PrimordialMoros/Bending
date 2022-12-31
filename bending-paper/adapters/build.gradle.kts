plugins {
    alias(libs.plugins.userdev).apply(false)
}

subprojects {
    apply(plugin = "io.papermc.paperweight.userdev")

    dependencies {
        compileOnly(project(":bending-api"))
    }
}

