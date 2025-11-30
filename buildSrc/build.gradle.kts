plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.2.2")
    implementation("com.github.spotbugs.snom:spotbugs-gradle-plugin:6.4.7")
    implementation("net.ltgt.errorprone:net.ltgt.errorprone.gradle.plugin:4.3.0")
    implementation("org.openrewrite:plugin:7.21.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

kotlin {
    jvmToolchain(17)
}
