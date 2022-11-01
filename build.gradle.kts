plugins {
    base
}

allprojects {
    group = "net.pistonmaster"
    version = "3.0.0-SNAPSHOT"
    description = "Best queue plugin out there!"
}

tasks.create("outputVersion") {
    doLast {
        println(project.version)
    }
}

val platforms = setOf(
    projects.pistonqueueBukkit,
    projects.pistonqueueBungee,
    projects.pistonqueueSponge,
    projects.pistonqueueVelocity
).map { it.dependencyProject }

val special = setOf(
    projects.pistonqueueUniversal,
    projects.pistonqueueShared
).map { it.dependencyProject }

subprojects {
    when (this) {
        in platforms -> plugins.apply("pq.platform-conventions")
        in special -> plugins.apply("pq.java-conventions")
    }
}
