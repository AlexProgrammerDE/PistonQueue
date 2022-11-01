plugins {
    id("pq.platform-conventions")
}

dependencies {
    implementation(projects.pistonqueueShared)

    implementation("org.bstats:bstats-bungeecord:3.0.0")

    compileOnly("net.md-5:bungeecord-api:1.18-R0.1-SNAPSHOT")
}
