plugins {
    id("pq.java-conventions")
}

dependencies {
    api("de.exlll:configlib-yaml:4.8.0")
    compileOnly("net.pistonmaster:pistonmotd-api:5.2.7")
    compileOnly("org.apiguardian:apiguardian-api:1.1.2")
    api("com.github.spotbugs:spotbugs-annotations:4.9.8")
    compileOnly("com.google.guava:guava:33.5.0-jre")
    testImplementation("com.google.guava:guava:33.5.0-jre")
}
