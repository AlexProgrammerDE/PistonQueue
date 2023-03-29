plugins {
    id("pq.platform-conventions")
}

dependencies {
    implementation(projects.pistonqueueShared)
    compileOnly(projects.pistonqueueBuildData)

    implementation("org.bstats:bstats-velocity:3.0.2")

    compileOnly("com.velocitypowered:velocity-api:3.1.1")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.1")
}
