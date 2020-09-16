plugins {
    java
    id("com.github.johnrengelman.shadow").version("6.0.0")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation("org.bstats", "bstats-bukkit-lite", "1.7")
    implementation("com.zaxxer", "HikariCP", "3.4.5")
    implementation("net.kyori", "adventure-platform-bukkit", "4.0.0-SNAPSHOT")
    implementation("org.spongepowered", "configurate-hocon", "3.7.1")
    implementation("co.aikar", "taskchain-bukkit", "3.7.2")
    implementation("co.aikar","acf-paper", "0.5.0-SNAPSHOT")
    implementation("org.jdbi", "jdbi3-core", "3.14.4")
    implementation("org.postgresql", "postgresql", "42.2.16.jre7")
    implementation("com.github.ben-manes.caffeine", "caffeine", "2.8.5")
    implementation("org.apache.commons", "commons-math3", "3.6.1")
    compileOnly("com.destroystokyo.paper", "paper-api", "1.16.2-R0.1-SNAPSHOT")
    compileOnly("com.github.TechFortress", "GriefPrevention", "16.7.1")
    compileOnly("com.github.TownyAdvanced", "Towny", "0.96.2.0")
    compileOnly("com.sk89q.worldedit", "worldedit-core", "7.0.0-SNAPSHOT")
    compileOnly("com.sk89q.worldedit", "worldedit-bukkit", "7.0.0-SNAPSHOT")
    compileOnly("com.sk89q.worldguard", "worldguard-core", "7.0.0-SNAPSHOT")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        archiveBaseName.set(rootProject.name)
        dependencies {
            relocate("org.bstats", "me.moros.bending.bstats")
            relocate("com.zaxxer", "me.moros.bending.internal.hikari")
            relocate("net.kyori", "me.moros.bending.internal.kyori")
            relocate("ninja.leaping", "me.moros.bending.internal.configurate")
            relocate("co.aikar.taskchain", "me.moros.bending.internal.taskchain")
            relocate("co.aikar.commands", "me.moros.bending.internal.acf")
            relocate("co.aikar.locales", "me.moros.bending.internal.locales")
            relocate("org.jdbi", "me.moros.bending.internal.jdbi")
            relocate("com.github.benmanes", "me.moros.bending.internal.caffeine")
            relocate("org.apache", "me.moros.bending.internal.apache")
        }
        /*minimize {
            exclude(dependency("org.postgresql:.*:.*"))
            exclude(dependency("com.github.benmanes:.*:.*"))
        }*/
    }
    build {
        dependsOn(shadowJar)
    }
    withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
    withType<JavaCompile> {
        options.compilerArgs.add("-parameters")
        options.isFork = true
        options.forkOptions.executable = "javac"
    }

    named<Copy>("processResources") {
        filesMatching("plugin.yml") {
            expand("pluginVersion" to project.version)
        }
    }
}
