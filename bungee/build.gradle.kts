plugins {
    id("pq.platform-conventions")
}

dependencies {
    implementation(projects.pistonqueueShared)

    implementation("net.pistonmaster:PistonUtils:1.4.0")
    implementation("org.bstats:bstats-bungeecord:3.2.1")

    compileOnly("net.md-5:bungeecord-api:1.21-R0.4")
}
