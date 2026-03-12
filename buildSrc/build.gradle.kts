plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.3.0")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.3.2")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.4.8")
    implementation("net.ltgt.errorprone:net.ltgt.errorprone.gradle.plugin:5.1.0")
    implementation("org.openrewrite:plugin:7.28.1")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}
