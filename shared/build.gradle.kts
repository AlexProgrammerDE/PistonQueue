plugins {
    id("pq.java-conventions")
}

dependencies {
    implementation("org.spongepowered:configurate-yaml:4.2.0")
    compileOnly("net.pistonmaster:pistonmotd-api:5.2.3")
    compileOnly("com.google.guava:guava:33.4.7-android")
}
