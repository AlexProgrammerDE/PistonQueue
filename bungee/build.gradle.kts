plugins {
    id("pq.platform-conventions")
}

dependencies {
    implementation(projects.pistonmotdApi)
    implementation(projects.pistonmotdShared)
    implementation(projects.pistonmotdKyoriRelocated)

    implementation("org.bstats:bstats-bungeecord:3.0.0")
    implementation("net.kyori:adventure-platform-bungeecord:4.1.2")

    compileOnly("net.md-5:bungeecord-api:1.18-R0.1-SNAPSHOT")
}
