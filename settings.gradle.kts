enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    plugins {
        id("com.github.johnrengelman.shadow") version "8.1.1"
        id("org.cadixdev.licenser") version "0.6.1"
        id("net.kyori.indra") version "3.1.2"
        id("net.kyori.indra.git") version "3.1.2"
        id("net.kyori.indra.publishing") version "3.1.2"
        id("net.kyori.blossom") version "1.3.1"
    }
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots") {
            name = "Sonatype"
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
        maven("https://repo.dmulloy2.net/nexus/repository/public") {
            name = "dmulloy2"
        }
    }
}

rootProject.name = "PistonQueue"

setOf(
    "build-data",
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
