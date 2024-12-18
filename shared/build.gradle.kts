plugins {
    id("pq.java-conventions")
}

dependencies {
    implementation("org.spongepowered:configurate-yaml:4.1.2")
    compileOnly("net.pistonmaster:pistonmotd-api:5.1.1")
    compileOnly("com.google.guava:guava:33.3.1-jre")
}
