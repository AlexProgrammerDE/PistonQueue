plugins {
    id("pq.platform-conventions")
}

dependencies {
    implementation(projects.pistonqueueShared)

    implementation("org.bstats:bstats-bungeecord:3.1.0")

    compileOnly("net.md-5:bungeecord-api:1.20-R0.2")
}
