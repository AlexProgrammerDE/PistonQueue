plugins {
    java
    id("net.kyori.blossom")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
                property("description", rootProject.description)
                property("url", "https://modrinth.com/plugin/pistonqueue")
            }
        }
    }
}
