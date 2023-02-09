plugins {
    id("io.papermc.paperweight.userdev").version("1.5.0")
}

dependencies {
    compileOnly(project(":bending-api"))
    paperDevBundle("1.18.2-R0.1-SNAPSHOT")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}
