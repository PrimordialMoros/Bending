plugins {
    id("com.github.johnrengelman.shadow")
    alias(libs.plugins.userdev)
}

dependencies {
    compileOnly(project(":bending-api"))
    implementation(project(":bending-nms"))
    paperweight.paperDevBundle("1.20-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    dependencies {
        relocate("me.moros.bending.common.adapter", "me.moros.bending.paper.adapter.v1_20_R1")
    }
}
