plugins {
    `java-library`
    `maven-publish`
}

repositories {
    maven {
        url = uri("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    }
    maven {
        url = uri("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    }
    maven {
        url = uri("https://papermc.io/repo/repository/maven-public/")
    }
    mavenCentral()
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.18.1-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.4")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
}

group = "net.pistonmaster"
version = "3.0.0-SNAPSHOT"
description = "PistonQueuePlaceholder"
java.sourceCompatibility = JavaVersion.VERSION_1_8
java.targetCompatibility = JavaVersion.VERSION_1_8

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}
