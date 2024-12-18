plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.0.0-beta4")
    implementation("net.kyori:indra-common:3.1.3")
}
