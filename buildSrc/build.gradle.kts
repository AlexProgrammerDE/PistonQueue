plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.diffplug.spotless:spotless-plugin-gradle:8.2.1")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.3.1")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.4.8")
    implementation("net.ltgt.errorprone:net.ltgt.errorprone.gradle.plugin:5.0.0")
    implementation("org.openrewrite:plugin:7.26.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}
