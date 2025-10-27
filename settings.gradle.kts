enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
        // Sonatype snapshots (for libraries publishing snapshots via Sonatype)
        maven("https://s01.oss.sonatype.org/content/repositories/snapshots/") {
            name = "Sonatype Snapshots"
            mavenContent { snapshotsOnly() }
        }
        // PaperMC public repo (updated domain)
        maven("https://repo.papermc.io/repository/maven-public/") {
            name = "PaperMC"
        }
        // VelocityPowered repos (releases and snapshots)
        maven("https://repo.velocitypowered.com/releases") {
            name = "Velocity Releases"
            mavenContent { releasesOnly() }
        }
        maven("https://repo.velocitypowered.com/snapshots") {
            name = "Velocity Snapshots"
            mavenContent { snapshotsOnly() }
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
