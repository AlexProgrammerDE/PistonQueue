plugins {
    id("pq.java-conventions")
}

dependencies {
    implementation("org.spongepowered:configurate-yaml:4.2.0")
    compileOnly("net.pistonmaster:pistonmotd-api:5.2.7")
    compileOnly("com.google.guava:guava:33.5.0-jre")
}
