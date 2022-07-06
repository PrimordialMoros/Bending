plugins {
    id("io.papermc.paperweight.userdev").version("1.3.8")
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
