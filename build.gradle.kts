plugins {
    base
}

tasks.named<UpdateDaemonJvm>("updateDaemonJvm") {
    languageVersion = JavaLanguageVersion.of(25)
}

allprojects {
    group = "net.pistonmaster"
    version = property("maven_version")!!
    description = "Best queue plugin out there!"
}
