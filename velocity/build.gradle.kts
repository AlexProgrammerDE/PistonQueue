plugins {
    id("pq.platform-conventions")
}

dependencies {
    implementation(projects.pistonqueueShared)
    compileOnly(projects.pistonqueueBuildData)

    implementation("net.pistonmaster:PistonUtils:1.4.0")
    implementation("org.bstats:bstats-velocity:3.1.0")

    // Use latest Velocity API snapshot for transfer and newer features
    compileOnly("com.velocitypowered:velocity-api:3.4.0-SNAPSHOT")
}
