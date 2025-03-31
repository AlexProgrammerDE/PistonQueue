plugins {
    base
}

allprojects {
    group = "net.pistonmaster"
    version = "3.0.1-SNAPSHOT"
    description = "Best queue plugin out there!"
}

tasks.register("outputVersion") {
    doLast {
        println(project.version)
    }
}
