plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("gradle.plugin.com.github.johnrengelman:shadow:7.1.2")
    implementation("net.kyori:indra-common:3.1.1")
}

java.targetCompatibility = JavaVersion.VERSION_1_8
java.sourceCompatibility = JavaVersion.VERSION_1_8
