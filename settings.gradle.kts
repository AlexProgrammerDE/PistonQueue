enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("com.github.johnrengelman.shadow") version "8.1.1"
        id("net.kyori.indra") version "3.2.0"
        id("net.kyori.indra.git") version "3.2.0"
        id("net.kyori.indra.publishing") version "3.2.0"
        id("net.kyori.blossom") version "2.2.0"
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/") {
          name = "Sonatype Snapshot Repository"
          mavenContent { snapshotsOnly() }
        }
        maven("https://papermc.io/repo/repository/maven-public/") {
            name = "PaperMC"
        }
        maven("https://nexus.velocitypowered.com/repository/maven-public/") {
            name = "VelocityPowered"
        }
        maven("https://repo.codemc.org/repository/maven-public") {
            name = "CodeMC"
        }
        maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
            name = "PlaceholderAPI"
        }
    }
}

rootProject.name = "PistonQueue"

setOf(
    "build-data",
    "placeholder",
    "shared",
    "bukkit",
    "bungee",
    "velocity",
    "universal"
).forEach { setupPQSubproject(it) }

fun setupPQSubproject(name: String) {
    setupSubproject("pistonqueue-$name") {
        projectDir = file(name)
    }
}

inline fun setupSubproject(name: String, block: ProjectDescriptor.() -> Unit) {
    include(name)
    project(":$name").apply(block)
}
