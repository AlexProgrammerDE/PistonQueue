plugins {
    id("pq.shadow-conventions")
}

tasks.shadowJar {
    archiveFileName.set(
        "PistonQueue-${
            project.name.substringAfter("pistonqueue-").replaceFirstChar { it.uppercase() }
        }-${project.version}.jar"
    )
    destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
}
