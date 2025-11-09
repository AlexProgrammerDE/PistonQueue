plugins {
    id("pq.platform-conventions")
    id("xyz.jpenilla.run-velocity") version "3.0.2"
}

dependencies {
    implementation(projects.pistonqueueShared)
    compileOnly(projects.pistonqueueBuildData)

    implementation("net.pistonmaster:PistonUtils:1.4.0")
    implementation("org.bstats:bstats-velocity:3.1.0")

    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    compileOnly("com.github.spotbugs:spotbugs-annotations:4.8.3")
}

tasks {
  runVelocity {
    velocityVersion("3.4.0-SNAPSHOT")
  }
}
