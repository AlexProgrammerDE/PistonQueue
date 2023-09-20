plugins {
    java
    id("net.kyori.blossom")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

sourceSets {
    main {
        blossom {
            javaSources {
                property("version", project.version.toString())
                property("description", rootProject.description)
                property("url", "https://pistonmaster.net/PistonQueue")
            }
        }
    }
}
