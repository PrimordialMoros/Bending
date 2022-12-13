plugins {
    id("io.papermc.paperweight.userdev").version("1.4.0").apply(false)
}

subprojects {
    apply(plugin = "io.papermc.paperweight.userdev")

    dependencies {
        compileOnly(project(":bending-api"))
    }
}

