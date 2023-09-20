import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

val platforms = setOf(
    rootProject.projects.pistonqueueBukkit,
    rootProject.projects.pistonqueueBungee,
    rootProject.projects.pistonqueueVelocity
).map { it.dependencyProject }

tasks {
    jar {
        archiveClassifier.set("")
        archiveFileName.set("PistonQueue-${rootProject.version}.jar")
        destinationDirectory.set(rootProject.projectDir.resolve("build/libs"))
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        platforms.forEach { platform ->
            val shadowJarTask = platform.tasks.named<ShadowJar>("shadowJar").get()
            dependsOn(shadowJarTask)
            dependsOn(platform.tasks.withType<Jar>())
            from(zipTree(shadowJarTask.archiveFile))
        }
    }
}
