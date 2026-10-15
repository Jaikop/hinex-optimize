pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.fabricmc.net/") { name = "FabricMC" }
        maven("https://maven.neoforged.net/releases/") { name = "NeoForged" }
        maven("https://maven.kikugie.dev/releases") { name = "KikuGie Releases" }
        maven("https://maven.kikugie.dev/snapshots") { name = "KikuGie Snapshots" }
    }
}

plugins {
    id("dev.kikugie.stonecutter") version "0.9.8"
    // Applies the right Loom variant per Minecraft version (https://codeberg.org/KikuGie/loom-back-compat)
    id("dev.kikugie.loom-back-compat") version "0.4.2"
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

stonecutter {
    create(rootProject) {
        // Creates `versions/{project}-{loader}` nodes, each built by `build.{loader}.gradle.kts`.
        fun match(project: String, vararg loaders: String, version: String = project) {
            for (loader in loaders) version("$project-$loader", version).buildscript("build.$loader.gradle.kts")
        }

        match("26.2", "fabric", "neoforge")
        match("26.3", "fabric", "neoforge")
        // 26.4 is still in snapshots and NeoForge has no build for it yet.
        // The node uses the logical version "26.4"; the real snapshot id is `deps.minecraft`.
        match("26.4", "fabric")
        vcsVersion = "26.3-fabric"
    }
}

rootProject.name = "HinexOptimize"
