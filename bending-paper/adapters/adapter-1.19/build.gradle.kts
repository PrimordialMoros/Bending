plugins {
    id("io.papermc.paperweight.userdev").version("1.3.11")
}

dependencies {
    compileOnly(project(":bending-api"))
    paperDevBundle("1.19.2-R0.1-SNAPSHOT")
}

tasks {
    assemble {
        dependsOn(reobfJar)
    }
}
