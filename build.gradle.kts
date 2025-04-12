plugins {
    base
}

allprojects {
    group = "net.pistonmaster"
    version = property("maven_version")!!
    description = "Best queue plugin out there!"
}

tasks.register("outputVersion") {
    doLast {
        println(project.version)
    }
}
