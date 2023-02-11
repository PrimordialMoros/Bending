plugins {
    alias(libs.plugins.userdev).apply(false)
}

subprojects {
    apply(plugin = "io.papermc.paperweight.userdev")
    apply(plugin = "com.github.johnrengelman.shadow")

    dependencies {
        compileOnly(project(":bending-api"))
        implementation(project(":bending-nms"))
    }
}

