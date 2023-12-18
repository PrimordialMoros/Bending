plugins {
    id("base-conventions")
    alias(libs.plugins.userdev)
}

dependencies {
    implementation(project(":bending-nms"))
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
}

tasks.shadowJar {
    dependencies {
        relocate("me.moros.bending.common.adapter", "me.moros.bending.paper.adapter.v1_20_R3")
    }
}
