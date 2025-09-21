plugins {
  id("pq.java-conventions")
}

dependencies {
  implementation(project(":pistonqueue-bukkit", "shadow"))
  implementation(project(":pistonqueue-bungee", "shadow"))
  implementation(project(":pistonqueue-velocity", "shadow"))
}

tasks {
  jar {
    archiveClassifier.set("")
    archiveFileName.set("PistonQueue-${rootProject.version}.jar")
    destinationDirectory.set(rootProject.projectDir.resolve("build/libs"))

    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(configurations.runtimeClasspath)
    from({ configurations.runtimeClasspath.get().map { zipTree(it) } })
  }
}
