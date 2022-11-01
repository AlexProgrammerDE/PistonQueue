import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    `java-library`
    id("pq.shadow-conventions")
}

(tasks.getByName("shadowJar") as ShadowJar).archiveFileName.set(
    "PistonQueue-${
        project.name.substringAfter("pistonqueue-").capitalize()
    }-${project.version}.jar"
)

(tasks.getByName("shadowJar") as ShadowJar).destinationDirectory.set(rootProject.layout.buildDirectory.dir("libs"))
