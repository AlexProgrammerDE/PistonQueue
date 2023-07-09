plugins {
    java
    id("net.kyori.blossom")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

blossom {
    replaceToken("{version}", version)
    replaceToken("{description}", rootProject.description)
    replaceToken("{url}", "https://pistonmaster.net/PistonQueue")
}
