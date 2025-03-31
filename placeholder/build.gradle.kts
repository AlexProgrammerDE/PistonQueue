plugins {
    id("pq.platform-conventions")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")

    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")

    implementation("net.pistonmaster:PistonUtils:1.4.0")
}
