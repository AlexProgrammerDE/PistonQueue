plugins {
    java
    `maven-publish`
}

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://repo.dmulloy2.net/nexus/repository/public")
    maven("https://repo.codemc.org/repository/maven-releases")
    maven("https://repo.codemc.org/repository/maven-snapshots")
    maven("https://nexus.velocitypowered.com/repository/maven-public")
    mavenCentral()
}

dependencies {
    implementation("org.spongepowered:configurate-yaml:4.1.2")
    implementation("org.bstats:bstats-bungeecord:3.0.0")
    implementation("org.bstats:bstats-velocity:3.0.0")
    compileOnly("org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.18-R0.1-SNAPSHOT")
    compileOnly("net.md-5:bungeecord-api:1.18-R0.1-SNAPSHOT")
    compileOnly("com.velocitypowered:velocity-api:3.1.0")
    annotationProcessor("com.velocitypowered:velocity-api:3.1.0")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.7.0")
    compileOnly("net.pistonmaster:pistonmotd-api:5.0.0-SNAPSHOT")
    compileOnly("org.projectlombok:lombok:1.18.22")
}

group = "net.pistonmaster"
version = "2.3.1"
description = "PistonQueue"
java.sourceCompatibility = JavaVersion.VERSION_1_8

publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}
